import test from 'node:test';
import assert from 'node:assert/strict';
import { freqToChannel, parseIwDevOutput, parseScanOutput } from './scanner.mjs';

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
