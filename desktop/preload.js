import { contextBridge, ipcRenderer } from 'electron';

const desktopApi = {
  listAdbDevices: () => ipcRenderer.invoke('adb:listDevices'),
  importSessionArtifact: () => ipcRenderer.invoke('sessions:importArtifact'),
  scanStart: () => ipcRenderer.invoke('scan:start'),
  scanStop: () => ipcRenderer.invoke('scan:stop'),
  scanDetectInterfaces: () => ipcRenderer.invoke('scan:detectInterfaces'),
  generateCompanionToken: () => ipcRenderer.invoke('companion:generateToken'),
};

const wscanApi = {
  version: '0.1.0',
  runAdbPreflight: () => ipcRenderer.invoke('adb:preflight'),
  startScan: (iface) => ipcRenderer.invoke('scan:start', iface),
  stopScan: () => ipcRenderer.invoke('scan:stop'),
  manualScan: () => ipcRenderer.invoke('scan:manual'),
  getScanState: () => ipcRenderer.invoke('scan:state'),
  getInterfaces: () => ipcRenderer.invoke('scan:interfaces'),
  pairCompanion: () => ipcRenderer.invoke('companion:pair'),
  getCompanionStatus: () => ipcRenderer.invoke('companion:status'),
  onAps: (callback) => registerListener('scan:aps', callback, 'onAps'),
  onRiskLog: (callback) => registerListener('scan:risklog', callback, 'onRiskLog'),
  onScanState: (callback) => registerListener('scan:statechange', callback, 'onScanState'),
  onCompanionUpdate: (callback) => registerListener('companion:update', callback, 'onCompanionUpdate'),
  onAppError: (callback) => registerListener('app:error', callback, 'onAppError'),
};

function registerListener(channel, callback, label) {
  if (typeof callback !== 'function') {
    throw new TypeError(`${label}: callback must be a function`);
  }
  const handler = (_event, data) => callback(data);
  ipcRenderer.on(channel, handler);
  return () => ipcRenderer.removeListener(channel, handler);
}

contextBridge.exposeInMainWorld('wscanDesktop', desktopApi);
contextBridge.exposeInMainWorld('wscan', wscanApi);
