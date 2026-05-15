import assert from 'node:assert/strict';
import test from 'node:test';
import {
  ANOMALIES,
  SESSION,
  WIFI,
  buildAnomalyReport,
  findAnomaly,
  handoffRisks,
  handoffWifiAps,
  summarizeHandoffSession,
} from './handoffPrototypeModel.js';

test('handoff WiFi rows map to desktop AP rows', () => {
  const aps = handoffWifiAps();

  assert.equal(aps.length, WIFI.length);
  assert.deepEqual(aps[2], {
    ssid: 'xfinitywifi',
    bssid: 'AE:4F:91:CC:31:F0',
    rssi: -68,
    channel: 11,
    security: 'open',
    risk: 'high',
    lastSeen: '2026-05-15T19:59:48Z',
    source: 'handoff',
    vendor: 'Comcast (Xfinity)',
  });
});

test('handoff anomalies map to risk entries with confidence levels', () => {
  const risks = handoffRisks();

  assert.equal(risks.length, ANOMALIES.length);
  assert.equal(risks[0].severity, 'high');
  assert.equal(risks[0].confidence, 82);
  assert.match(risks[0].reason, /suspicious captive portal/i);
  assert.equal(risks[0].source, 'handoff');
});

test('findAnomaly resolves by id and by BSSID', () => {
  assert.equal(findAnomaly('wifi-open-portal')?.id, 'wifi-open-portal');
  assert.equal(findAnomaly('AE:4F:91:CC:31:F0')?.id, 'wifi-open-portal');
});

test('summarizeHandoffSession returns operator metrics', () => {
  assert.deepEqual(summarizeHandoffSession(), {
    id: SESSION.id,
    title: SESSION.name,
    duration: SESSION.duration,
    location: SESSION.location,
    totalWifi: 8,
    anomalousWifi: 3,
    totalAnomalies: 4,
    highestLevel: 'high',
  });
});

test('buildAnomalyReport preserves evidence boundaries', () => {
  const report = buildAnomalyReport(ANOMALIES[0]);

  assert.match(report, /wifi-open-portal/);
  assert.match(report, /no actor identification/i);
  assert.match(report, /no intent attribution/i);
});
