export const SEVERITY = Object.freeze({
  HIGH: 'high',
  MEDIUM: 'medium',
  LOW: 'low',
  INFO: 'info',
});

const SEVERITY_ORDER = [SEVERITY.INFO, SEVERITY.LOW, SEVERITY.MEDIUM, SEVERITY.HIGH];

function bumpSeverity(current, next) {
  const a = SEVERITY_ORDER.indexOf(current);
  const b = SEVERITY_ORDER.indexOf(next);
  return SEVERITY_ORDER[Math.max(a, b)];
}

export function scoreAP(ap, baselineAps = new Map()) {
  const reasons = [];
  let severity = SEVERITY.INFO;

  if (!ap.ssid || ap.ssid.length === 0) {
    reasons.push('Hidden SSID');
    severity = bumpSeverity(severity, SEVERITY.LOW);
  }

  if (ap.security === 'open') {
    reasons.push('No encryption (open network)');
    severity = bumpSeverity(severity, SEVERITY.LOW);
  }

  if (ap.security === 'wep') {
    reasons.push('WEP encryption (deprecated, trivially broken)');
    severity = bumpSeverity(severity, SEVERITY.MEDIUM);
  }

  if (ap.signal > -30) {
    reasons.push(
      `Exceptionally strong signal (${ap.signal} dBm) - possible co-located rogue`,
    );
    severity = bumpSeverity(severity, SEVERITY.MEDIUM);
  }

  if (ap.ssid && ap.ssid.length > 0) {
    for (const [knownBssid, knownAp] of baselineAps) {
      if (knownBssid !== ap.bssid && knownAp.ssid === ap.ssid) {
        reasons.push(`SSID collision: same name as known AP ${knownBssid}`);
        severity = bumpSeverity(severity, SEVERITY.HIGH);
        break;
      }
    }
  }

  const confidence =
    reasons.length === 0 ? 0 : Math.min(0.9, 0.3 + reasons.length * 0.2);

  return {
    bssid: ap.bssid,
    ssid: ap.ssid,
    severity,
    reasons,
    confidence,
  };
}

export function scoreAPs(aps, baselineAps = new Map()) {
  return aps
    .map((ap) => scoreAP(ap, baselineAps))
    .filter((s) => s.reasons.length > 0);
}
