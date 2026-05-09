const api = window.wscan ?? window.wscanplus ?? window.wscanPlus ?? {};

const state = {
  aps: new Map(),
  risks: [],
  events: [],
  selectedBssid: null,
  scanState: { running: false, scanning: false },
  companion: null,
  adb: { status: 'unknown' },
  historyLimit: 40,
};

const severityRank = { high: 3, critical: 3, threat: 3, watch: 2, medium: 2, warning: 2, low: 1, info: 0 };
const $ = (id) => document.getElementById(id);

function normalizeBssid(value) {
  return typeof value === 'string' ? value.trim().toUpperCase() : '';
}

function normalizeTimestamp(value) {
  if (value instanceof Date) return value.getTime();
  const n = Number(value);
  if (Number.isFinite(n)) return n < 10_000_000_000 ? n * 1000 : n;
  const parsed = Date.parse(String(value));
  return Number.isFinite(parsed) ? parsed : Date.now();
}

function normalizeRisk(value) {
  const risk = String(value ?? 'low').toLowerCase();
  if (['critical', 'high', 'threat'].includes(risk)) return 'high';
  if (['watch', 'medium', 'warning'].includes(risk)) return 'watch';
  if (['none', 'info'].includes(risk)) return 'info';
  return 'low';
}

function inferChannel(frequency) {
  if (!Number.isFinite(frequency)) return null;
  if (frequency === 2484) return 14;
  if (frequency >= 2412 && frequency <= 2472) return Math.round((frequency - 2407) / 5);
  if (frequency >= 5000 && frequency <= 5900) return Math.round((frequency - 5000) / 5);
  if (frequency >= 5955 && frequency <= 7115) return Math.round((frequency - 5950) / 5);
  return null;
}

function normalizeAp(input) {
  if (!input || typeof input !== 'object') return null;
  const bssid = normalizeBssid(input.bssid ?? input.BSSID ?? input.mac);
  if (!bssid) return null;
  const rssi = Number(input.rssi ?? input.signal ?? input.level);
  const frequency = Number(input.frequency ?? input.freq);
  const channel = Number(input.channel ?? input.ch);
  return {
    ssid: String(input.ssid ?? input.SSID ?? '<hidden>'),
    bssid,
    rssi: Number.isFinite(rssi) ? rssi : null,
    channel: Number.isFinite(channel) ? channel : inferChannel(frequency),
    frequency: Number.isFinite(frequency) ? frequency : null,
    security: String(input.security ?? input.capabilities ?? input.encryption ?? 'unknown'),
    risk: normalizeRisk(input.risk ?? input.severity ?? 'low'),
    lastSeen: normalizeTimestamp(input.lastSeen ?? input.timestamp ?? Date.now()),
    source: String(input.source ?? 'desktop'),
  };
}

function upsertAp(input) {
  const ap = normalizeAp(input);
  if (!ap) return;
  const existing = state.aps.get(ap.bssid);
  const history = Array.isArray(existing?.history) ? existing.history.slice(-state.historyLimit) : [];
  if (Number.isFinite(ap.rssi)) history.push({ rssi: ap.rssi, timestamp: ap.lastSeen });
  state.aps.set(ap.bssid, {
    ...existing,
    ...ap,
    seenCount: (existing?.seenCount ?? 0) + 1,
    firstSeen: existing?.firstSeen ?? ap.lastSeen,
    history,
  });
  state.selectedBssid ??= ap.bssid;
}

function setAps(payload) {
  const list = Array.isArray(payload) ? payload : Array.isArray(payload?.aps) ? payload.aps : Array.isArray(payload?.networks) ? payload.networks : [];
  for (const ap of list) upsertAp(ap);
  render();
}

function normalizeRiskEntry(input) {
  if (!input || typeof input !== 'object') return null;
  const bssid = normalizeBssid(input.bssid ?? input.BSSID ?? input.mac);
  const severity = normalizeRisk(input.severity ?? input.risk ?? input.level ?? 'watch');
  const confidenceRaw = Number(input.confidence ?? 0);
  const confidence = Number.isFinite(confidenceRaw) ? Math.max(0, Math.min(100, confidenceRaw <= 1 ? confidenceRaw * 100 : confidenceRaw)) : 0;
  const reason = String(input.reason ?? input.message ?? (Array.isArray(input.reasons) ? input.reasons.join('; ') : 'Detector event'));
  const timestamp = normalizeTimestamp(input.timestamp ?? input.ts ?? Date.now());

  return {
    id: String(input.id ?? `${bssid || 'risk'}:${severity}:${reason}`),
    bssid,
    ssid: String(input.ssid ?? ''),
    severity,
    confidence,
    reason,
    timestamp,
    status: String(input.status ?? 'open'),
    source: String(input.source ?? 'detector'),
  };
}

function riskKey(risk) {
  return `${risk.bssid}:${risk.severity}`;
}

function applyRiskToAp(risk) {
  const ap = state.aps.get(risk.bssid);
  if (ap && (severityRank[risk.severity] ?? 0) > (severityRank[ap.risk] ?? 0)) {
    state.aps.set(risk.bssid, { ...ap, risk: risk.severity });
  }
}

function emitRiskEvent(risk) {
  addEvent({
    level: risk.severity === 'high' ? 'HIGH' : risk.severity === 'watch' ? 'WATCH' : 'INFO',
    source: 'detector',
    message: `${risk.reason}${risk.bssid ? ` (${risk.bssid})` : ''}`,
  }, false);
}

function addRisk(input, rerender = true) {
  const risk = normalizeRiskEntry(input);
  if (!risk) return;
  const key = riskKey(risk);
  const existingIndex = state.risks.findIndex((item) => riskKey(item) === key);
  const isNew = existingIndex === -1;
  if (isNew) {
    state.risks.unshift(risk);
  } else {
    state.risks[existingIndex] = risk;
  }
  state.risks = state.risks
    .sort((a, b) => b.timestamp - a.timestamp)
    .slice(0, 80);
  applyRiskToAp(risk);
  if (isNew) emitRiskEvent(risk);
  if (rerender) render();
}

function setRiskSnapshot(payload) {
  const list = Array.isArray(payload) ? payload : Array.isArray(payload?.risks) ? payload.risks : Array.isArray(payload?.events) ? payload.events : payload ? [payload] : [];
  const existingKeys = new Set(state.risks.map(riskKey));
  const nextRisks = new Map();

  for (const input of list) {
    const risk = normalizeRiskEntry(input);
    if (!risk) continue;
    nextRisks.set(riskKey(risk), risk);
  }

  state.risks = [...nextRisks.values()]
    .sort((a, b) => b.timestamp - a.timestamp)
    .slice(0, 80);

  for (const risk of state.risks) {
    applyRiskToAp(risk);
    if (!existingKeys.has(riskKey(risk))) emitRiskEvent(risk);
  }

  render();
}

function addEvent(input, rerender = true) {
  const event = {
    time: normalizeTimestamp(input?.time ?? input?.timestamp ?? input?.ts ?? Date.now()),
    level: String(input?.level ?? input?.severity ?? 'INFO').toUpperCase(),
    source: String(input?.source ?? 'ui'),
    message: String(input?.message ?? input?.reason ?? input ?? ''),
  };
  state.events.unshift(event);
  state.events = state.events.slice(0, 220);
  if (rerender) renderEvents();
}

function formatAge(ts) {
  const delta = Math.max(0, Date.now() - ts);
  if (delta < 1000) return 'now';
  if (delta < 60_000) return `${Math.round(delta / 1000)}s`;
  if (delta < 3_600_000) return `${Math.round(delta / 60_000)}m`;
  return `${Math.round(delta / 3_600_000)}h`;
}

function formatTime(ts) {
  return new Date(ts).toLocaleTimeString([], { hour12: false });
}

function summarizeSecurity(value) {
  const text = String(value ?? 'unknown');
  if (/WPA3/i.test(text)) return 'WPA3';
  if (/WPA2/i.test(text)) return 'WPA2';
  if (/WPA/i.test(text)) return 'WPA';
  if (/OPEN|ESS/i.test(text) && !/PRIVACY/i.test(text)) return 'OPEN';
  return text.length > 12 ? `${text.slice(0, 12)}…` : text;
}

function sortedAps() {
  return [...state.aps.values()].sort((a, b) => ((severityRank[b.risk] ?? 0) - (severityRank[a.risk] ?? 0)) || ((b.rssi ?? -999) - (a.rssi ?? -999)));
}

function renderTopStatus() { /* unchanged */ }
function renderMetrics(aps) { /* unchanged */ }
function renderApTable(aps) { /* unchanged */ }
function renderInspector() { /* unchanged */ }

function renderThreats() {
  const list = $('threat-list');
  list.replaceChildren();
  const risks = state.risks.slice(0, 12);

  for (const risk of risks) {
    const li = document.createElement('li');
    li.className = 'threat-item';
    li.dataset.evidenceId = risk.id;
    li.dataset.bssid = risk.bssid;
    li.dataset.severity = risk.severity;
    li.dataset.confidence = String(Math.round(risk.confidence));
    li.dataset.reason = risk.reason;
    li.dataset.timestamp = String(risk.timestamp);
    li.dataset.source = risk.source;

    const badge = document.createElement('span');
    badge.className = `badge ${risk.severity === 'high' ? 'badge-threat' : risk.severity === 'watch' ? 'badge-warning' : 'badge-neutral'}`;
    badge.textContent = `${risk.severity.toUpperCase()} ${risk.confidence ? `${Math.round(risk.confidence)}%` : ''}`.trim();

    const title = document.createElement('strong');
    title.textContent = risk.bssid || risk.ssid || 'Detector event';

    const body = document.createElement('p');
    body.textContent = `${risk.reason} • ${formatAge(risk.timestamp)} ago`;

    li.append(badge, title, body);
    list.appendChild(li);
  }

  if (!risks.length) {
    const li = document.createElement('li');
    li.className = 'threat-item';
    li.innerHTML = '<strong>No active detector events</strong><p>Threat queue will populate from RiskEvent data.</p>';
    list.appendChild(li);
  }

  $('threat-count').textContent = `${state.risks.length} events`;
}

function renderCompanion() { /* unchanged */ }
function renderChannelBars(aps) { /* unchanged */ }
function renderChart(aps) { /* unchanged */ }
function renderEvents() { /* unchanged */ }
function render() { /* unchanged */ }
async function safeCall(name, ...args) { /* unchanged */ }
function subscribe(name, handler) { /* unchanged */ }
async function initInterfaces() { /* unchanged */ }
function wireControls() { /* unchanged */ }
function wireSubscriptions() { /* unchanged */ }
function seedDemoIfEmpty() { /* unchanged */ }
async function boot() { /* unchanged */ }

boot().catch((error) => {
  console.error(error);
  addEvent({ level: 'ERROR', source: 'ui', message: `boot failed: ${error?.message ?? error}` });
  render();
});
