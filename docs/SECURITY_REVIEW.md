# Security Best Practices Report

## Executive Summary

This pass applied JavaScript frontend security guidance to the Electron desktop surfaces in `desktop/`. The desktop app already has several good controls in place: `contextIsolation` is enabled, `nodeIntegration` is disabled, navigation and popup creation are blocked, a restrictive meta CSP is present in `index.html`, and the renderer code uses safe DOM APIs instead of `innerHTML`-style sinks.

The main remaining best-practice gaps are Electron privilege reduction and IPC argument validation. The most important issue is that the renderer runs with Electron sandboxing disabled, which leaves a larger blast radius if the renderer is compromised. The second issue is that privileged IPC handlers accept renderer-supplied interface names and pass them to child-process execution without server-side validation, which is safer than shell interpolation but still broader than the repo's own child-process guardrail intends.

This report is intentionally scoped to JavaScript/Electron code. Separate Android/Kotlin findings from the broader repo audit, including the SQLCipher singleton mismatch in `ScanMapActivity`, are not repeated here because they are outside this skill's language-specific reference set.

## High Severity

### JS-ELECTRON-001

- Severity: High
- Location: [desktop/main.js](C:/Users/Devia/Documents/GitHub/wscanplus/desktop/main.js):243-247
- Evidence:

```js
webPreferences: {
  contextIsolation: true,
  nodeIntegration: false,
  sandbox: false,
  preload: path.join(__dirname, 'preload.js'),
},
```

- Impact: If the renderer is compromised through any future DOM/XSS or preload bug, disabling Electron sandboxing increases the privileges available to compromised renderer code and reduces the value of Chromium's renderer isolation.
- Fix: Prefer `sandbox: true` unless a concrete preload or dependency requirement blocks it. If the current desktop stack needs `sandbox: false`, document the exact reason and treat it as a deliberate exception that should be revisited.
- Mitigation: Keep `contextIsolation: true`, `nodeIntegration: false`, restrictive CSP, popup blocking, and strict preload exposure in place. Avoid adding any new renderer-facing privileged APIs until sandboxing is reassessed.
- False positive notes: Some Electron apps intentionally disable sandboxing for compatibility, but that should be justified explicitly because Electron security guidance treats sandboxing as a meaningful hardening control.

## Medium Severity

### JS-ELECTRON-002

- Severity: Medium
- Location: [desktop/preload.js](C:/Users/Devia/Documents/GitHub/wscanplus/desktop/preload.js):15
- Location: [desktop/main.js](C:/Users/Devia/Documents/GitHub/wscanplus/desktop/main.js):325-328
- Location: [desktop/scanner.mjs](C:/Users/Devia/Documents/GitHub/wscanplus/desktop/scanner.mjs):223-236
- Location: [desktop/scanner.mjs](C:/Users/Devia/Documents/GitHub/wscanplus/desktop/scanner.mjs):172
- Evidence:

```js
startScan: (iface) => ipcRenderer.invoke('scan:start', iface),
```

```js
ipcMain.handle('scan:start', async (_event, iface) => {
  try {
    await startScanning(iface || undefined);
```

```js
export async function startScanning(iface) {
  let resolvedIface = iface;
  ...
  store.update({ scanning: true, interface: resolvedIface });
  void scanLoop();
}
```

```js
const raw = await runCommand('iw', ['dev', iface, 'scan']);
```

- Impact: The privileged main-process scan path accepts a renderer-supplied interface string and passes it to a child process without validating that it is a discovered interface or even a syntactically valid identifier. Because `spawn(cmd, args)` is used, this is not classic shell injection, but it still broadens the command surface exposed to any compromised renderer.
- Fix: Validate `iface` in the main process before calling `startScanning`. A minimal secure pattern is to reject values that are not in the result of `detectInterfaces()`, and to enforce a conservative interface-name regex server-side rather than trusting renderer input.
- Mitigation: Keep using `spawn(cmd, args)` rather than `exec`. Do not add any string-built shell commands on this path.
- False positive notes: If all renderer code remains trusted, this may never be user-triggerable in normal flows, but Electron IPC hardening assumes renderer compromise is in scope.

### JS-ELECTRON-003

- Severity: Medium
- Location: [desktop/companionServer.mjs](C:/Users/Devia/Documents/GitHub/wscanplus/desktop/companionServer.mjs):51-58
- Location: [desktop/main.js](C:/Users/Devia/Documents/GitHub/wscanplus/desktop/main.js):26-27
- Location: [desktop/index.html](C:/Users/Devia/Documents/GitHub/wscanplus/desktop/index.html):93-97
- Evidence:

```js
start(port = 0, host = '127.0.0.1') {
  if (this.#httpServer) return Promise.resolve(this.address);
  return new Promise((resolve, reject) => {
    this.#httpServer = createServer();
    this.#wss = new WebSocketServer({ server: this.#httpServer });
```

```js
const COMPANION_PORT = parseInt(process.env.COMPANION_PORT ?? '47392', 10);
const COMPANION_HOST = process.env.COMPANION_HOST ?? '127.0.0.1';
```

```html
Generate a token and enter it in the companion app to authorise
the connection. The server binds to localhost only unless
<code>COMPANION_HOST</code> is set.
```

- Impact: The default binding is reasonably safe, but the app explicitly supports rebinding the companion service away from localhost. If operators set `COMPANION_HOST` to a LAN-facing address or `0.0.0.0`, the service becomes reachable by any host on that network segment and still relies on a bearer token over plaintext WebSocket transport.
- Fix: Treat non-loopback binding as a higher-risk mode. At minimum, validate and log when the configured host is non-loopback, and consider requiring an explicit "unsafe external bind" opt-in instead of a single environment variable.
- Mitigation: Keep localhost as the default. Keep pairing tokens high-entropy and short-lived, and avoid publishing the service address beyond the trusted operator UI.
- False positive notes: This is not a vulnerability when used with the default `127.0.0.1` bind. The risk appears when users operate it across an untrusted LAN, which the project cannot fully prevent.

## Low Severity

### JS-ELECTRON-004

- Severity: Low
- Location: [desktop/index.html](C:/Users/Devia/Documents/GitHub/wscanplus/desktop/index.html):5-8
- Evidence:

```html
<meta
  http-equiv="Content-Security-Policy"
  content="default-src 'self'; script-src 'self'; style-src 'self'; object-src 'none'; base-uri 'none'"
>
```

- Impact: This is a positive control, but because the app relies on a meta-delivered CSP rather than a response header, it cannot enforce header-only directives such as `frame-ancestors` and cannot use report-only mode. That reduces observability and some defense-in-depth options.
- Fix: If the Electron packaging path allows it, prefer equivalent CSP enforcement at the response/session layer. If not, keep the current meta CSP early in the document and avoid weakening it with `unsafe-inline` or `unsafe-eval`.
- Mitigation: The current policy is still useful and materially better than having no CSP.
- False positive notes: In a local Electron app there may be no practical HTTP layer to set response headers, so meta CSP can be the correct constrained choice.

## Positive Practices Observed

- [desktop/main.js](C:/Users/Devia/Documents/GitHub/wscanplus/desktop/main.js):244-245 keeps `contextIsolation` enabled and `nodeIntegration` disabled.
- [desktop/main.js](C:/Users/Devia/Documents/GitHub/wscanplus/desktop/main.js):251-257 blocks popup creation and unexpected navigation.
- [desktop/index.html](C:/Users/Devia/Documents/GitHub/wscanplus/desktop/index.html):5-8 defines a restrictive CSP with `script-src 'self'`.
- [desktop/renderer.mjs](C:/Users/Devia/Documents/GitHub/wscanplus/desktop/renderer.mjs) builds DOM content using explicit element creation and `textContent`, and this review did not find `innerHTML`, `insertAdjacentHTML`, `eval`, `new Function`, or web-storage use in first-party desktop sources.
- [desktop/scanner.mjs](C:/Users/Devia/Documents/GitHub/wscanplus/desktop/scanner.mjs):23 uses `spawn(cmd, args)` rather than shell interpolation for privileged scan commands.

## Recommended Next Steps

1. Reassess whether the desktop window can run with `sandbox: true` without breaking preload behavior.
2. Add main-process validation for `scan:start` interface arguments before they reach `startScanning()` and `runCommand()`.
3. Make non-loopback companion binding an explicit high-risk opt-in and log it clearly when used.
