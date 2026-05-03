import { app, BrowserWindow, dialog, ipcMain } from 'electron';
import { spawn } from 'node:child_process';
import { readFile } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { AdbTransport } from './adb/AdbTransport.js';
import { RayhunterTransport } from './rayhunter/RayhunterTransport.js';
import {
  NtfyPublisher,
  generateTopic,
  loadTopic,
  saveTopic,
} from './ntfy/NtfyPublisher.js';
import {
  PRELIGHT_CLASSIFICATIONS,
  describeDeviceReadiness,
  parseCompanionPackagePath,
  parseCompanionVersionInfo,
  summarizePreflight,
  validateDeviceSelector,
} from './adbPreflight.mjs';
import { CompanionServer } from './companionServer.mjs';
import {
  detectInterfaces,
  executeScanCycle,
  startScanning,
  stopScanning,
} from './scanner.mjs';
import { store } from './store.mjs';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const COMPANION_PACKAGE = 'com.wscanplus.app';
const COMPANION_PORT = parseInt(process.env.COMPANION_PORT ?? '47392', 10);
const COMPANION_HOST = process.env.COMPANION_HOST ?? '127.0.0.1';

let adbTransport;
let rayhunterTransport;
let ntfyPublisher;
let companionServer;
let mainWindow = null;

function unknownCompanion() {
  return {
    status: 'unknown',
    packageName: COMPANION_PACKAGE,
    versionName: '',
    versionCode: '',
  };
}

function runAdb(args) {
  return new Promise((resolve, reject) => {
    const child = spawn('adb', args, {
      windowsHide: true,
    });
    let stdout = '';
    let stderr = '';

    child.stdout.on('data', (chunk) => {
      stdout += chunk.toString();
    });

    child.stderr.on('data', (chunk) => {
      stderr += chunk.toString();
    });

    child.on('error', (error) => {
      reject(error);
    });

    child.on('close', (code) => {
      if (code === 0) {
        resolve(stdout);
        return;
      }

      reject(new Error(stderr.trim() || `adb exited with code ${code}`));
    });
  });
}

async function attachCompanionStatus(devices) {
  const results = [];

  for (const device of devices) {
    if (device.state !== 'device') {
      results.push({
        ...device,
        readiness: describeDeviceReadiness(device),
      });
      continue;
    }

    if (!validateDeviceSelector(device.serial)) {
      const companion = unknownCompanion();

      results.push({
        ...device,
        companion,
        readiness: describeDeviceReadiness({
          ...device,
          companion,
        }),
      });
      continue;
    }

    try {
      const packageOutput = await runAdb([
        '-s',
        device.serial,
        'shell',
        'pm',
        'list',
        'packages',
        COMPANION_PACKAGE,
      ]);
      const companion = parseCompanionPackagePath(
        packageOutput,
        COMPANION_PACKAGE,
      );

      if (companion.status === 'missing') {
        results.push({
          ...device,
          companion,
          readiness: describeDeviceReadiness({
            ...device,
            companion,
          }),
        });
        continue;
      }

      const dumpOutput = await runAdb([
        '-s',
        device.serial,
        'shell',
        'dumpsys',
        'package',
        COMPANION_PACKAGE,
      ]);

      const companionWithVersion = {
        ...companion,
        ...parseCompanionVersionInfo(dumpOutput),
      };

      results.push({
        ...device,
        companion: companionWithVersion,
        readiness: describeDeviceReadiness({
          ...device,
          companion: companionWithVersion,
        }),
      });
    } catch {
      const companion = unknownCompanion();

      results.push({
        ...device,
        companion,
        readiness: describeDeviceReadiness({
          ...device,
          companion,
        }),
      });
    }
  }

  return results;
}

async function initAdbTransport() {
  adbTransport = new AdbTransport();

  adbTransport.on('connect', ({ serial }) => {
    console.info(`[adb] Connected to ${serial} WatchdogService (tcp:9000)`);
  });

  adbTransport.on('data', (buffer) => {
    console.debug(`[adb] WatchdogService data ${buffer.length} bytes`);
  });

  adbTransport.on('disconnect', ({ serial }) => {
    console.info(`[adb] Disconnected from ${serial} WatchdogService`);
  });

  adbTransport.on('error', (error) => {
    console.error('[adb] Transport error', error);
  });

  try {
    const devices = await adbTransport.listDevices();
    if (devices.length === 0) {
      console.warn('[adb] No devices connected; skipping WatchdogService connect');
      return;
    }

    const serial = devices[0];
    await adbTransport.connect(serial);
  } catch (error) {
    console.error('[adb] Failed to initialize ADB transport', error);
  }
}

async function initNtfyPublisher() {
  const configDir = app.getPath('userData');
  const existingTopic = await loadTopic(configDir);
  const topic = existingTopic ?? generateTopic();
  if (!existingTopic) {
    await saveTopic(configDir, topic);
  }
  ntfyPublisher = new NtfyPublisher({ topic });
  store.setNtfyPublisher(ntfyPublisher);
  console.info(`[ntfy] Topic: ${topic}`);
}

async function initRayhunterTransport() {
  rayhunterTransport = new RayhunterTransport();

  rayhunterTransport.on('connect', ({ url }) => {
    console.info(`[rayhunter] Connected at ${url}`);
  });

  rayhunterTransport.on('disconnect', ({ url }) => {
    console.info(`[rayhunter] Disconnected from ${url}`);
  });

  rayhunterTransport.on('threats', ({ entries }) => {
    store.addCellularThreats(entries);
  });

  rayhunterTransport.on('stats', (stats) => {
    store.updateRayhunterStats(stats);
  });

  rayhunterTransport.on('error', (e) => {
    console.warn('[rayhunter] poll error', e.message);
  });

  await rayhunterTransport.init();
}

function pushToRenderer(channel, data) {
  if (
    mainWindow !== null &&
    !mainWindow.isDestroyed() &&
    !mainWindow.webContents.isDestroyed()
  ) {
    mainWindow.webContents.send(channel, data);
  }
}

function ensureCompanionServer() {
  if (companionServer) {
    return companionServer;
  }

  companionServer = new CompanionServer({
    onData: (payload) => {
      if (Array.isArray(payload.networks) && payload.networks.length > 0) {
        const normalized = payload.networks.map((n) => ({
          bssid: String(n.bssid ?? '').toLowerCase(),
          ssid: String(n.ssid ?? ''),
          signal: Number(n.rssi ?? n.signal ?? -100),
          frequency: Number(n.frequency ?? 0),
          channel: Number(n.channel ?? 0),
          security: String(n.security ?? 'open'),
          source: 'android',
          deviceId: payload.deviceId,
        }));
        store.addAps(normalized);
      }
      pushToRenderer('companion:update', {
        deviceId: payload.deviceId,
        timestamp: payload.timestamp,
        networkCount: payload.networks?.length ?? 0,
      });
    },
  });

  return companionServer;
}

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1200,
    height: 800,
    webPreferences: {
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: false,
      preload: path.join(__dirname, 'preload.js'),
    },
  });

  mainWindow.webContents.setWindowOpenHandler(() => ({ action: 'deny' }));
  mainWindow.webContents.on('will-navigate', (event, url) => {
    if (url !== mainWindow.webContents.getURL()) {
      event.preventDefault();
      console.warn('[security] Blocked navigation to', url);
    }
  });

  mainWindow.loadFile(path.join(__dirname, 'index.html')).catch((error) => {
    console.error('Failed to load index.html', error);
  });

  mainWindow.on('closed', () => {
    mainWindow = null;
  });
}

store.on('aps', (aps) => pushToRenderer('scan:aps', aps));
store.on('riskLog', (log) => pushToRenderer('scan:risklog', log));
store.on('change', (state) =>
  pushToRenderer('scan:statechange', {
    scanning: state.scanning,
    interface: state.interface,
    scanIntervalMs: state.scanIntervalMs,
  }),
);
store.on('appError', (entry) => pushToRenderer('app:error', entry));
store.on('cellularThreats', (t) => pushToRenderer('cellular:threats', t));
store.on('rayhunterStats', (s) => pushToRenderer('cellular:stats', s));

ipcMain.handle('cellular:getThreats', () => store.state.cellularThreats);
ipcMain.handle('cellular:getStats', () => store.state.rayhunterStats);

ipcMain.handle('ntfy:getTopic', () => ntfyPublisher?.topic ?? null);
ipcMain.handle('ntfy:setTopic', async (_event, topic) => {
  ntfyPublisher?.setTopic(topic);
  await saveTopic(app.getPath('userData'), topic);
});

ipcMain.handle('adb:listDevices', async () => {
  if (!adbTransport) return [];
  return adbTransport.listDevices();
});

ipcMain.handle('adb:preflight', async () => {
  try {
    await runAdb(['start-server']);
    const versionOutput = await runAdb(['version']);
    const devicesOutput = await runAdb(['devices', '-l']);
    const summary = summarizePreflight(versionOutput, devicesOutput);
    return {
      ...summary,
      devices: await attachCompanionStatus(summary.devices),
    };
  } catch (error) {
    const message =
      error instanceof Error ? error.message : 'ADB preflight failed.';
    const failure = {
      ok: false,
      error: message,
      rawError:
        error instanceof Error
          ? {
              name: error.name,
              message: error.message,
              stack: error.stack,
              code: error.code,
            }
          : String(error),
    };
    const isAdbMissing =
      error &&
      typeof error === 'object' &&
      'code' in error &&
      error.code === 'ENOENT';

    return {
      ...failure,
      classification: isAdbMissing
        ? PRELIGHT_CLASSIFICATIONS.adbMissing
        : PRELIGHT_CLASSIFICATIONS.preflightFailed,
    };
  }
});

const IFACE_RE = /^[a-zA-Z0-9_-]{1,32}$/;

ipcMain.handle('scan:start', async (_event, iface) => {
  if (iface !== undefined && iface !== null && !IFACE_RE.test(iface)) {
    return { ok: false, error: 'Invalid interface name' };
  }
  try {
    await startScanning(iface || undefined);
    return { ok: true, started: true, interface: store.state.interface };
  } catch (error) {
    return {
      ok: false,
      error: error instanceof Error ? error.message : String(error),
    };
  }
});

ipcMain.handle('scan:stop', () => {
  stopScanning();
  return { ok: true, stopped: true };
});

ipcMain.handle('scan:manual', async () => {
  try {
    const aps = await executeScanCycle();
    return { ok: true, count: aps.length };
  } catch (error) {
    return {
      ok: false,
      error: error instanceof Error ? error.message : String(error),
    };
  }
});

ipcMain.handle('scan:state', () => ({
  scanning: store.state.scanning,
  interface: store.state.interface,
  scanIntervalMs: store.state.scanIntervalMs,
  apCount: store.state.aps.size,
  riskCount: store.state.riskLog.length,
}));

ipcMain.handle('scan:interfaces', async () => {
  try {
    const interfaces = await detectInterfaces();
    return { ok: true, interfaces };
  } catch (error) {
    return {
      ok: false,
      error: error instanceof Error ? error.message : String(error),
      interfaces: [],
    };
  }
});

ipcMain.handle('scan:detectInterfaces', async () => {
  try {
    return await detectInterfaces();
  } catch (error) {
    console.error('[scan] Failed to detect interfaces', error);
    return [];
  }
});

ipcMain.handle('companion:pair', async () => {
  const server = ensureCompanionServer();
  const token = server.generateToken();

  try {
    const address = await server.start(COMPANION_PORT, COMPANION_HOST);
    return { ok: true, token, address };
  } catch (error) {
    return {
      ok: false,
      error: error instanceof Error ? error.message : String(error),
    };
  }
});

ipcMain.handle('companion:generateToken', async () => {
  const server = ensureCompanionServer();
  const token = server.generateToken();
  return { token, address: server.address };
});

ipcMain.handle('companion:status', () => ({
  running: companionServer !== null && companionServer.address !== null,
  address: companionServer?.address ?? null,
  token: companionServer?.token ?? null,
}));

ipcMain.handle('sessions:importArtifact', async () => {
  const { canceled, filePaths } = await dialog.showOpenDialog({
    properties: ['openFile'],
    filters: [{ name: 'JSON', extensions: ['json'] }],
  });
  if (canceled || filePaths.length === 0) return null;
  const filePath = filePaths[0];
  const raw = await readFile(filePath, 'utf8');
  return { raw, filePath };
});

app.whenReady().then(async () => {
  createWindow();
  await initAdbTransport();
  await initRayhunterTransport();
  await initNtfyPublisher();

  const server = ensureCompanionServer();
  try {
    await server.start(COMPANION_PORT, COMPANION_HOST);
  } catch (error) {
    console.error('[companion] Failed to start server', error);
  }

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow();
  });
});

app.on('before-quit', () => {
  stopScanning();
  rayhunterTransport?.stop();
  companionServer?.stop().catch((error) => {
    console.error('[companion] Failed to stop server during quit', error);
  });
  adbTransport?.disconnect().catch((error) => {
    console.error('[adb] Failed to disconnect transport during quit', error);
  });
});

app.on('window-all-closed', () => {
  stopScanning();
  rayhunterTransport?.stop();
  companionServer?.stop().catch((error) => {
    console.error('[companion] Failed to stop server', error);
  });
  adbTransport?.disconnect().catch((error) => {
    console.error('[adb] Failed to disconnect transport', error);
  });
  if (process.platform !== 'darwin') {
    app.quit();
  }
});
