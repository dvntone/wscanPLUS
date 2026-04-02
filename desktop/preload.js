import { contextBridge, ipcRenderer } from 'electron';

contextBridge.exposeInMainWorld('wscanDesktop', {
  // NOTE: These functions currently do not call into ipcMain because no
  // corresponding ipcMain.handle handlers are registered in the main process.
  // They return rejected Promises to avoid invoking non-existent channels.
  listAdbDevices: () =>
    Promise.reject(
      new Error(
        'ADB device listing is not available: no ipcMain handler is registered for "adb:listDevices".',
      ),
    ),
  importSessionArtifact: () =>
    Promise.reject(
      new Error(
        'Session artifact import is not available: no ipcMain handler is registered for "sessions:importArtifact".',
      ),
    ),
});
