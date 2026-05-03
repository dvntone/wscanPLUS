import { spawn } from 'node:child_process';
import { store } from './store.mjs';
import { scoreAPs } from './detector.mjs';

const COMMAND_TIMEOUT_MS = 5_000;
const SIGKILL_DELAY_MS = 250;

export class ScanError extends Error {
  constructor(message, { code = null, signal = null } = {}) {
    super(message);
    this.name = 'ScanError';
    this.code = code;
    this.signal = signal;
  }
}

export function runCommand(
  cmd,
  args,
  timeoutMs = COMMAND_TIMEOUT_MS,
) {
  return new Promise((resolve, reject) => {
    const child = spawn(cmd, args, { windowsHide: true });
    let stdout = '';
    let stderr = '';
    let forceKillTimer = null;

    const timeoutId = setTimeout(() => {
      child.kill('SIGTERM');
      forceKillTimer = setTimeout(() => {
        child.kill('SIGKILL');
      }, SIGKILL_DELAY_MS);
    }, timeoutMs);

    function cleanup() {
      clearTimeout(timeoutId);
      if (forceKillTimer) clearTimeout(forceKillTimer);
    }

    child.stdout.on('data', (chunk) => {
      stdout += chunk.toString();
    });
    child.stderr.on('data', (chunk) => {
      stderr += chunk.toString();
    });

    child.on('error', (err) => {
      cleanup();
      reject(new ScanError(err.message, { code: err.code }));
    });

    child.on('close', (code, signal) => {
      cleanup();
      if (signal === 'SIGTERM' || signal === 'SIGKILL') {
        reject(new ScanError(`Command timed out: ${cmd}`, { signal }));
        return;
      }
      if (code !== 0) {
        reject(
          new ScanError(
            stderr.trim() || `${cmd} exited with code ${code}`,
            { code },
          ),
        );
        return;
      }
      resolve(stdout);
    });
  });
}

let commandRunner = runCommand;

export function setCommandRunnerForTests(runner) {
  commandRunner = typeof runner === 'function' ? runner : runCommand;
}

export function parseIwDevOutput(raw) {
  const ifaces = [];
  for (const line of raw.split('\n')) {
    const m = line.trim().match(/^Interface\s+(\S+)/);
    if (m) ifaces.push(m[1]);
  }
  return ifaces;
}

export function parseScanOutput(raw) {
  const aps = [];
  let current = null;

  for (const line of raw.split('\n')) {
    const trimmed = line.trim();

    const bssMatch = trimmed.match(/^BSS ([0-9a-f:]{17})\(/i);
    if (bssMatch) {
      if (current) aps.push(finalize(current));
      current = {
        bssid: bssMatch[1].toLowerCase(),
        ssid: null,
        frequency: 0,
        channel: 0,
        signal: -100,
        security: 'open',
      };
      continue;
    }

    if (!current) continue;

    const freqM = trimmed.match(/^freq:\s*(\d+)/);
    if (freqM) {
      current.frequency = parseInt(freqM[1], 10);
      current.channel = freqToChannel(current.frequency);
      continue;
    }

    const sigM = trimmed.match(/^signal:\s*(-?\d+(?:\.\d+)?)\s*dBm/);
    if (sigM) {
      current.signal = parseFloat(sigM[1]);
      continue;
    }

    if (trimmed.startsWith('SSID:')) {
      current.ssid = trimmed.slice(5).trim();
      continue;
    }

    if (trimmed.startsWith('RSN:')) {
      current.security = 'wpa2';
      continue;
    }

    if (trimmed.startsWith('WPA:') && current.security !== 'wpa2') {
      current.security = 'wpa';
      continue;
    }

    if (
      trimmed.startsWith('capability:') &&
      current.security === 'open' &&
      trimmed.includes('Privacy')
    ) {
      current.security = 'wep';
    }
  }

  if (current) aps.push(finalize(current));
  return aps;
}

function finalize(ap) {
  return { ...ap, ssid: ap.ssid ?? '' };
}

export function freqToChannel(freqMHz) {
  if (freqMHz === 2484) return 14;
  if (freqMHz >= 2412 && freqMHz <= 2472) {
    return Math.round((freqMHz - 2407) / 5);
  }
  if (freqMHz >= 5170 && freqMHz <= 5825) {
    return Math.round((freqMHz - 5000) / 5);
  }
  if (freqMHz >= 5955 && freqMHz <= 7115) {
    return Math.round((freqMHz - 5950) / 5);
  }
  return 0;
}

let activeScanPromise = null;
let activeScanGeneration = null;
let activeScanRequiresActive = false;
let scanLoopTimer = null;
let scanGeneration = 0;

export function resetScannerForTests() {
  commandRunner = runCommand;
  activeScanPromise = null;
  activeScanGeneration = null;
  activeScanRequiresActive = false;
  clearTimeout(scanLoopTimer);
  scanLoopTimer = null;
  scanGeneration = 0;
}

async function runScanAndProcess({ generation, requireActive }) {
  const iface = store.state.interface;
  if (!iface) {
    throw new ScanError('No wireless interface configured');
  }

  if (requireActive && (!store.state.scanning || generation !== scanGeneration)) {
    return [];
  }

  const raw = await commandRunner('iw', ['dev', iface, 'scan']);
  const aps = parseScanOutput(raw);

  if (requireActive && (!store.state.scanning || generation !== scanGeneration)) {
    return [];
  }

  if (aps.length > 0) {
    store.addAps(aps);
    const flagged = scoreAPs(aps, store.state.aps).filter(
      (e) => e.severity !== 'info',
    );
    if (flagged.length > 0) {
      store.addRiskEntries(flagged);
    }
  }

  return aps;
}

export async function executeScanCycle({ requireActive = false } = {}) {
  const generation = scanGeneration;
  const canReuseActiveScan =
    activeScanPromise &&
    activeScanGeneration === generation &&
    activeScanRequiresActive === requireActive;

  if (!canReuseActiveScan) {
    activeScanGeneration = generation;
    activeScanRequiresActive = requireActive;
    activeScanPromise = runScanAndProcess({ generation, requireActive }).finally(() => {
      if (activeScanGeneration === generation && activeScanRequiresActive === requireActive) {
        activeScanPromise = null;
        activeScanGeneration = null;
        activeScanRequiresActive = false;
      }
    });
  }
  return activeScanPromise;
}

function scheduleNextScan() {
  if (!store.state.scanning) return;
  clearTimeout(scanLoopTimer);
  scanLoopTimer = setTimeout(() => {
    void scanLoop();
  }, store.state.scanIntervalMs);
}

async function scanLoop() {
  if (!store.state.scanning) return;
  try {
    await executeScanCycle({ requireActive: true });
  } catch (err) {
    store.addError(err);
  } finally {
    if (store.state.scanning) {
      scheduleNextScan();
    }
  }
}

export async function detectInterfaces() {
  const raw = await commandRunner('iw', ['dev']);
  return parseIwDevOutput(raw);
}

export async function startScanning(iface) {
  if (store.state.scanning) return;

  let resolvedIface = iface;
  if (!resolvedIface) {
    const ifaces = await detectInterfaces();
    if (ifaces.length === 0) {
      throw new ScanError('No wireless interfaces detected');
    }
    resolvedIface = ifaces[0];
  }

  scanGeneration += 1;
  store.update({ scanning: true, interface: resolvedIface });
  void scanLoop();
}

export function stopScanning() {
  scanGeneration += 1;
  store.update({ scanning: false });
  clearTimeout(scanLoopTimer);
  scanLoopTimer = null;
}
