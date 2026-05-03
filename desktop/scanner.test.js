import test from 'node:test';
import assert from 'node:assert/strict';
import {
  executeScanCycle,
  freqToChannel,
  parseIwDevOutput,
  parseScanOutput,
  resetScannerForTests,
  setCommandRunnerForTests,
  startScanning,
  stopScanning,
} from './scanner.mjs';
import { store } from './store.mjs';

const SCAN_OUTPUT = `
BSS aa:bb:cc:dd:ee:ff(on wlan0)
	freq: 2412
	signal: -42.00 dBm
	SSID: testnet
	RSN:
`;

async function flushMicrotasks() {
  await Promise.resolve();
  await Promise.resolve();
}

function resetStore() {
  store.update({ scanning: false, interface: null, scanIntervalMs: 30_000 });
  store.state.aps.clear();
  store.state.riskLog = [];
  store.state.errors = [];
}

test.afterEach(() => {
  resetScannerForTests();
  resetStore();
});

test('parseIwDevOutput extracts interface names', () => {
  const raw = `
phy#0
  Interface wlan0
    ifindex 3
phy#1
  Interface mon0
    ifindex 4
`;

  assert.deepEqual(parseIwDevOutput(raw), ['wlan0', 'mon0']);
});

test('parseScanOutput extracts AP details and security markers', () => {
  const raw = `
BSS aa:bb:cc:dd:ee:ff(on wlan0)
	freq: 2412
	signal: -42.00 dBm
	SSID: testnet
	RSN:
BSS 11:22:33:44:55:66(on wlan0)
	freq: 2462
	signal: -71.50 dBm
	SSID:
	capability: ESS Privacy ShortSlotTime
`;

  const aps = parseScanOutput(raw);

  assert.equal(aps.length, 2);
  assert.deepEqual(aps[0], {
    bssid: 'aa:bb:cc:dd:ee:ff',
    ssid: 'testnet',
    frequency: 2412,
    channel: 1,
    signal: -42,
    security: 'wpa2',
  });
  assert.equal(aps[1].security, 'wep');
  assert.equal(aps[1].ssid, '');
});

test('freqToChannel maps common Wi-Fi frequencies', () => {
  assert.equal(freqToChannel(2412), 1);
  assert.equal(freqToChannel(5180), 36);
  assert.equal(freqToChannel(5955), 1);
  assert.equal(freqToChannel(1234), 0);
});

test('manual scan can mutate store while continuous scanning is stopped', async () => {
  resetStore();
  store.update({ scanning: false, interface: 'wlan0' });
  setCommandRunnerForTests(async () => SCAN_OUTPUT);

  const aps = await executeScanCycle();

  assert.equal(aps.length, 1);
  assert.equal(store.state.aps.size, 1);
  assert.equal(store.state.aps.get('aa:bb:cc:dd:ee:ff')?.ssid, 'testnet');
});

test('active scan discards stale results after stop before store mutation', async () => {
  resetStore();
  let resolveScan;
  const scanPromise = new Promise((resolve) => {
    resolveScan = resolve;
  });
  setCommandRunnerForTests(async () => scanPromise);
  await startScanning('wlan0');

  stopScanning();
  resolveScan(SCAN_OUTPUT);
  await flushMicrotasks();

  assert.equal(store.state.scanning, false);
  assert.equal(store.state.aps.size, 0);
});

test('manual scan during active continuous scan reuses the in-flight promise', async () => {
  resetStore();
  let callCount = 0;
  setCommandRunnerForTests(async () => {
    callCount += 1;
    return SCAN_OUTPUT;
  });

  await startScanning('wlan0');
  await executeScanCycle();

  assert.equal(callCount, 1);
});

test('restart starts a fresh scan instead of waiting on stale in-flight promise', async () => {
  resetStore();
  let firstResolve;
  let callCount = 0;
  setCommandRunnerForTests(async () => {
    callCount += 1;
    if (callCount === 1) {
      return new Promise((resolve) => {
        firstResolve = resolve;
      });
    }
    return SCAN_OUTPUT;
  });

  await startScanning('wlan0');
  stopScanning();
  await startScanning('wlan0');
  firstResolve(SCAN_OUTPUT);
  await flushMicrotasks();

  assert.equal(callCount, 2);
  assert.equal(store.state.aps.size, 1);
});
