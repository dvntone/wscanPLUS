import test from 'node:test';
import assert from 'node:assert/strict';
import { scoreAP, scoreAPs, SEVERITY } from './detector.mjs';

test('scoreAP raises severity for WEP and strong signal', () => {
  const result = scoreAP({
    bssid: 'aa:bb:cc:dd:ee:ff',
    ssid: 'legacy',
    security: 'wep',
    signal: -25,
  });

  assert.equal(result.severity, SEVERITY.MEDIUM);
  assert.match(result.reasons.join(' '), /WEP encryption/);
  assert.match(result.reasons.join(' '), /Exceptionally strong signal/);
});

test('scoreAPs flags SSID collisions against the baseline', () => {
  const baseline = new Map([
    ['00:11:22:33:44:55', { ssid: 'CorpWiFi' }],
  ]);

  const results = scoreAPs([
    {
      bssid: '66:77:88:99:aa:bb',
      ssid: 'CorpWiFi',
      security: 'wpa2',
      signal: -60,
    },
  ], baseline);

  assert.equal(results.length, 1);
  assert.equal(results[0].severity, SEVERITY.HIGH);
  assert.match(results[0].reasons[0], /SSID collision/);
});
