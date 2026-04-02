import { contextBridge, ipcRenderer } from 'electron';

contextBridge.exposeInMainWorld('wscanDesktop', {
  listAdbDevices: () => ipcRenderer.invoke('adb:listDevices'),
  importSessionArtifact: () => ipcRenderer.invoke('sessions:importArtifact'),
});
