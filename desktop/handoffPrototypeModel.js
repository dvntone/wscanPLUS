export const ANOMALY_CONFIDENCE = { low: 34, medium: 61, high: 82, critical: 95 };

export const SESSION = {
  id: 'sess-7c41a',
  name: 'Apt 4-W · Tuesday evening sweep',
  duration: '00:18:42',
  location: 'home · indoor',
};

export const WIFI = [
  {
    bssid: '4C:5E:0C:91:8A:42',
    ssid: 'PRIVATE-HOME-5G',
    ch: 36,
    sec: 'WPA3',
    rssi: -42,
    vendor: 'Cisco Meraki',
    lastSeen: '2026-05-15T19:59:48Z',
  },
  {
    bssid: '78:8A:20:11:E8:0A',
    ssid: 'PRIVATE-HOME-2G',
    ch: 6,
    sec: 'WPA2',
    rssi: -54,
    vendor: 'Cisco Meraki',
    lastSeen: '2026-05-15T19:59:48Z',
  },
  {
    bssid: 'AE:4F:91:CC:31:F0',
    ssid: 'xfinitywifi',
    ch: 11,
    sec: 'open',
    rssi: -68,
    vendor: 'Comcast (Xfinity)',
    lastSeen: '2026-05-15T19:59:48Z',
    anomaly: 'high',
  },
  {
    bssid: 'AE:4F:91:CC:31:F1',
    ssid: 'xfinitywifi',
    ch: 36,
    sec: 'open',
    rssi: -71,
    vendor: 'Comcast (Xfinity)',
    lastSeen: '2026-05-15T19:59:48Z',
    anomaly: 'high',
  },
  {
    bssid: '02:8C:5D:7F:11:01',
    ssid: 'Setup-7A11',
    ch: 1,
    sec: 'open',
    rssi: -77,
    vendor: 'unknown (locally-administered)',
    lastSeen: '2026-05-15T19:48:30Z',
    anomaly: 'medium',
  },
  {
    bssid: '88:AC:C1:0E:5B:22',
    ssid: 'NETGEAR55',
    ch: 6,
    sec: 'WPA2',
    rssi: -83,
    vendor: 'Netgear',
    lastSeen: '2026-05-15T19:59:48Z',
  },
  {
    bssid: '00:1B:11:9A:03:CC',
    ssid: 'TP-LINK_C3',
    ch: 11,
    sec: 'WPA2',
    rssi: -85,
    vendor: 'TP-Link',
    lastSeen: '2026-05-15T19:59:48Z',
  },
  {
    bssid: '50:C7:BF:01:22:81',
    ssid: '<hidden>',
    ch: 149,
    sec: 'WPA2',
    rssi: -76,
    vendor: 'unknown',
    lastSeen: '2026-05-15T19:59:48Z',
  },
];

export const ANOMALIES = [
  {
    id: 'wifi-open-portal',
    sourceId: 'a-019',
    bssid: 'AE:4F:91:CC:31:F0',
    title: 'Suspicious captive portal',
    level: 'high',
    t: '2026-05-15T19:47:11Z',
    summary:
      'Open WiFi captive portal for xfinitywifi does not match known-good baseline.',
    reasons: [
      'TLS certificate issuer differs from baseline',
      'Redirect chain has one extra hop through an unfamiliar host',
      'Form action URL does not match baseline form host',
    ],
    signals: ['wifi', 'portal'],
    recurrence: 'first-seen at this location',
    investigation: 'Avoid auto-connect until the portal is confirmed.',
  },
  {
    id: 'ble-rotating-burst',
    sourceId: 'a-018',
    title: 'BLE advertisement burst - rotating identifier',
    level: 'medium',
    t: '2026-05-15T19:45:08Z',
    summary:
      'Rotating BLE advertisement pattern with sub-second cadence recurred across recent sessions.',
    reasons: [
      'Advertisement interval at or below 220ms sustained for 2+ minutes',
      'Random-resolvable address rotation rate above baseline',
      'Similar pattern observed in 4 prior sessions at this location',
    ],
    signals: ['ble'],
    recurrence: 'recurring - 5 sessions - same approximate location',
    investigation: 'Pattern alone does not confirm a device class.',
  },
  {
    id: 'wifi-new-open-cluster',
    sourceId: 'a-017',
    bssid: '02:8C:5D:7F:11:01',
    title: 'New AP cluster on busy channel',
    level: 'medium',
    t: '2026-05-15T19:44:32Z',
    summary:
      'Short-lived BSSIDs broadcast open networks within the same scan window.',
    reasons: [
      'BSSIDs not in baseline and using open security',
      'Co-occurrent appearance pattern',
      'High RSSI variance suggests mobility',
    ],
    signals: ['wifi'],
    recurrence: 'first-seen',
    investigation: 'Watch and re-correlate if the pattern repeats.',
  },
  {
    id: 'wifi-baseline-rssi-drift',
    sourceId: 'a-016',
    bssid: '4C:5E:0C:91:8A:42',
    title: 'Baseline RSSI drift - PRIVATE-HOME-5G',
    level: 'low',
    t: '2026-05-15T19:43:50Z',
    summary:
      'Trusted AP RSSI is 8 dB below the 7-day median for this hour.',
    reasons: ['RSSI median delta is -8 dB within the urban damping band'],
    signals: ['wifi'],
    recurrence: 'occasional - environmental',
    investigation: 'No action recommended.',
  },
];

function riskForWifi(row) {
  if (row.anomaly === 'high') return 'high';
  if (row.anomaly === 'medium') return 'watch';
  return 'low';
}

export function handoffWifiAps() {
  return WIFI.map((row) => ({
    ssid: row.ssid,
    bssid: row.bssid,
    rssi: row.rssi,
    channel: row.ch,
    security: row.sec,
    risk: riskForWifi(row),
    lastSeen: row.lastSeen,
    source: 'handoff',
    vendor: row.vendor,
  }));
}

export function handoffRisks() {
  return ANOMALIES.map((anomaly) => ({
    id: anomaly.id,
    bssid: anomaly.bssid ?? '',
    ssid: WIFI.find((row) => row.bssid === anomaly.bssid)?.ssid ?? '',
    severity: anomaly.level === 'medium' ? 'watch' : anomaly.level,
    confidence: ANOMALY_CONFIDENCE[anomaly.level] ?? 50,
    reason: `${anomaly.title}: ${anomaly.summary}`,
    timestamp: anomaly.t,
    status: 'open',
    source: 'handoff',
  }));
}

export function findAnomaly(value) {
  const normalized = String(value ?? '').toUpperCase();
  return (
    ANOMALIES.find(
      (anomaly) =>
        anomaly.id === value ||
        anomaly.sourceId === value ||
        anomaly.bssid?.toUpperCase() === normalized,
    ) ?? null
  );
}

export function summarizeHandoffSession() {
  const levels = ['low', 'medium', 'high', 'critical'];
  const highest = ANOMALIES.reduce((max, anomaly) => {
    return levels.indexOf(anomaly.level) > levels.indexOf(max)
      ? anomaly.level
      : max;
  }, 'low');

  return {
    id: SESSION.id,
    title: SESSION.name,
    duration: SESSION.duration,
    location: SESSION.location,
    totalWifi: WIFI.length,
    anomalousWifi: WIFI.filter((row) => row.anomaly).length,
    totalAnomalies: ANOMALIES.length,
    highestLevel: highest,
  };
}

export function buildAnomalyReport(anomaly) {
  if (!anomaly) return '';
  return [
    `Anomaly: ${anomaly.title}`,
    `ID: ${anomaly.id} (${anomaly.sourceId})`,
    `Level: ${anomaly.level}`,
    `Signals: ${anomaly.signals.join(', ')}`,
    `Summary: ${anomaly.summary}`,
    `Reasons: ${anomaly.reasons.join('; ')}`,
    `Recurrence: ${anomaly.recurrence}`,
    `Recommendation: ${anomaly.investigation}`,
    'Boundary: no actor identification; no intent attribution.',
  ].join('\n');
}
