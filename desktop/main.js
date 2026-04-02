import { app, BrowserWindow, ipcMain, dialog } from 'electron';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';
import { readFile } from 'node:fs/promises';
import { AdbTransport } from './adb/AdbTransport.js';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);
let adbTransport;

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

function createWindow() {
  const win = new BrowserWindow({
    width: 800,
    height: 600,
    webPreferences: {
      contextIsolation: true,
      nodeIntegration: false,
      preload: join(__dirname, 'preload.js'),
    }
  });

  win.loadFile(join(__dirname, 'index.html')).catch((err) => {
    console.error('Failed to load index.html', err);
  });
}

app.whenReady().then(() => {
  ipcMain.handle('adb:listDevices', async () => {
    if (!adbTransport) return [];
    return adbTransport.listDevices();
  });

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

  createWindow();
  void initAdbTransport();

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow();
  });
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    void adbTransport?.disconnect();
    app.quit();
  }
});
