import {
  buildAnomalyReport,
  findAnomaly,
  handoffRisks,
  handoffWifiAps,
  summarizeHandoffSession,
} from './handoffPrototypeModel.js';

const api = window.wscan ?? window.wscanplus ?? window.wscanPlus ?? {};

const state = {
  aps: new Map(),
  risks: [],
  events: [],
  selectedBssid: null,
  reviewedAnomalies: new Set(),
  scanState: { running: false, scanning: false },
  companion: null,
  adb: { status: 'unknown' },
  historyLimit: 40,
};

const severityRank = { high: 3, critical: 3, threat: 3, watch: 2, medium: 2, warning: 2, low: 1, info: 0 };
const $ = (id) => document.getElementById(id);
let anomalyModalCleanup = null;

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
    source: String(input.source ?? ''),
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

function renderTopStatus() {
  $('clock').textContent = new Date().toLocaleTimeString([], { hour12: false });
  const running = Boolean(state.scanState?.running ?? state.scanState?.scanning);
  const scanBadge = $('scan-state-badge');
  scanBadge.textContent = running ? 'SCAN LIVE' : 'SCAN IDLE';
  scanBadge.className = `badge ${running ? 'badge-success' : 'badge-neutral'}`;
  const online = state.companion?.status === 'online' || state.companion?.connected === true;
  $('companion-state-badge').textContent = online ? 'COMPANION ONLINE' : 'COMPANION OFFLINE';
  $('companion-state-badge').className = `badge ${online ? 'badge-success' : 'badge-neutral'}`;
  const adbReady = state.adb?.status === 'ready' || state.adb?.ready === true;
  $('adb-state-badge').textContent = adbReady ? 'ADB READY' : String(state.adb?.status ?? 'ADB UNKNOWN').toUpperCase();
  $('adb-state-badge').className = `badge ${adbReady ? 'badge-success' : 'badge-neutral'}`;
}

function renderMetrics(aps) {
  const high = aps.filter((ap) => ap.risk === 'high').length;
  const watched = aps.filter((ap) => ap.risk === 'watch').length;
  const strongest = aps.filter((ap) => Number.isFinite(ap.rssi)).sort((a, b) => b.rssi - a.rssi)[0];
  const channels = new Map();
  for (const ap of aps) if (Number.isFinite(ap.channel)) channels.set(ap.channel, (channels.get(ap.channel) ?? 0) + 1);
  const crowded = [...channels.entries()].sort((a, b) => b[1] - a[1])[0];
  $('metric-ap-count').textContent = String(aps.length);
  $('metric-ap-sub').textContent = `${aps.length - watched} known / ${watched} watched`;
  $('metric-high-risk').textContent = String(high);
  $('metric-strongest').textContent = strongest ? `${strongest.rssi} dBm` : '--';
  $('metric-channel-load').textContent = crowded ? `CH ${crowded[0]}` : '--';
  $('sample-count').textContent = `${aps.reduce((acc, ap) => acc + (ap.history?.length ?? 0), 0)} SAMPLES`;
}

function renderApTable(aps) {
  const body = $('ap-table-body');
  body.replaceChildren();
  for (const ap of aps) {
    const tr = document.createElement('tr');
    tr.className = `${ap.risk === 'high' ? 'high' : ap.risk === 'watch' ? 'watch' : ''} ${state.selectedBssid === ap.bssid ? 'selected' : ''}`;
    tr.tabIndex = 0;
    tr.dataset.bssid = ap.bssid;
    const cells = [ap.ssid, ap.bssid, Number.isFinite(ap.rssi) ? `${ap.rssi}` : '--', Number.isFinite(ap.channel) ? `${ap.channel}` : '--', summarizeSecurity(ap.security), ap.risk.toUpperCase(), formatAge(ap.lastSeen)];
    for (const cell of cells) {
      const td = document.createElement('td');
      td.textContent = cell;
      tr.appendChild(td);
    }
    tr.addEventListener('click', () => { state.selectedBssid = ap.bssid; render(); });
    tr.addEventListener('keydown', (event) => {
      if (event.key === 'Enter' || event.key === ' ') { state.selectedBssid = ap.bssid; render(); }
    });
    body.appendChild(tr);
  }
  $('inventory-count').textContent = `${aps.length} rows`;
}

function renderInspector() {
  const ap = state.selectedBssid ? state.aps.get(state.selectedBssid) : null;
  const risk = ap?.risk ?? 'none';
  $('selected-bssid').textContent = ap?.bssid ?? 'No AP selected';
  $('selected-ssid').textContent = ap ? ap.ssid : 'Select a row from AP Inventory.';
  $('selected-rssi').textContent = Number.isFinite(ap?.rssi) ? `${ap.rssi} dBm` : '--';
  $('selected-channel').textContent = Number.isFinite(ap?.channel) ? `CH ${ap.channel}` : '--';
  $('selected-security').textContent = ap?.security ?? '--';
  $('selected-last-seen').textContent = ap?.lastSeen ? formatAge(ap.lastSeen) : '--';
  const badge = $('selected-risk');
  badge.textContent = risk.toUpperCase();
  badge.className = `badge ${risk === 'high' ? 'badge-threat' : risk === 'watch' ? 'badge-warning' : risk === 'low' ? 'badge-success' : 'badge-neutral'}`;
}

function renderThreats() {
  const list = $('threat-list');
  list.replaceChildren();
  const risks = state.risks.slice(0, 12);
  for (const risk of risks) {
    const li = document.createElement('li');
    li.className = 'threat-item';
    if (risk.source) li.dataset.source = risk.source;
    const anomaly = findAnomaly(risk.id) ?? findAnomaly(risk.bssid);
    const reviewed = anomaly && state.reviewedAnomalies.has(anomaly.id);
    if (anomaly) {
      li.tabIndex = 0;
      li.setAttribute('role', 'button');
      li.setAttribute('aria-label', `Open anomaly detail for ${anomaly.title}`);
      li.classList.toggle('reviewed', Boolean(reviewed));
      li.addEventListener('click', () => openAnomalyDetail(anomaly.id, li));
      li.addEventListener('keydown', (event) => {
        if (event.key === 'Enter' || event.key === ' ') {
          event.preventDefault();
          openAnomalyDetail(anomaly.id, li);
        }
      });
    }
    const badge = document.createElement('span');
    badge.className = `badge ${risk.severity === 'high' ? 'badge-threat' : risk.severity === 'watch' ? 'badge-warning' : 'badge-neutral'}`;
    badge.textContent = reviewed ? 'REVIEWED' : `${risk.severity.toUpperCase()} ${risk.confidence ? `${Math.round(risk.confidence)}%` : ''}`.trim();
    const title = document.createElement('strong');
    title.textContent = anomaly?.title ?? risk.bssid ?? risk.ssid ?? 'Detector event';
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

function openAnomalyDetail(identifier, returnFocusTo = document.activeElement) {
  const anomaly = findAnomaly(identifier);
  if (!anomaly) return;

  anomalyModalCleanup?.();
  document.getElementById('anomaly-detail-modal')?.remove();

  const overlay = document.createElement('section');
  overlay.id = 'anomaly-detail-modal';
  overlay.className = 'anomaly-detail-modal';
  overlay.setAttribute('role', 'dialog');
  overlay.setAttribute('aria-modal', 'true');
  overlay.setAttribute('aria-label', anomaly.title);

  const sheet = document.createElement('article');
  sheet.className = 'anomaly-detail-sheet';
  sheet.addEventListener('click', (event) => event.stopPropagation());

  const closeModal = ({ rerender = false, restoreFocus = true } = {}) => {
    overlay.remove();
    anomalyModalCleanup?.();
    anomalyModalCleanup = null;
    if (rerender) render();
    if (restoreFocus && returnFocusTo?.isConnected && typeof returnFocusTo.focus === 'function') returnFocusTo.focus();
  };

  const onKeydown = (event) => {
    if (event.key !== 'Escape') return;
    event.preventDefault();
    closeModal();
  };
  document.addEventListener('keydown', onKeydown);
  anomalyModalCleanup = () => document.removeEventListener('keydown', onKeydown);

  const close = document.createElement('button');
  close.className = 'secondary-action';
  close.type = 'button';
  close.textContent = 'Close';
  close.addEventListener('click', closeModal);

  const badge = document.createElement('span');
  badge.className = `badge ${anomaly.level === 'high' ? 'badge-threat' : anomaly.level === 'medium' ? 'badge-warning' : 'badge-neutral'}`;
  badge.textContent = anomaly.level.toUpperCase();

  const heading = document.createElement('h2');
  heading.textContent = anomaly.title;

  const summary = document.createElement('p');
  summary.textContent = anomaly.summary;

  const reasons = document.createElement('ul');
  reasons.className = 'anomaly-reasons';
  for (const reason of anomaly.reasons) {
    const item = document.createElement('li');
    item.textContent = reason;
    reasons.appendChild(item);
  }

  const meta = document.createElement('p');
  meta.className = 'muted';
  meta.textContent = `${anomaly.sourceId} · ${anomaly.t} · ${anomaly.signals.join(' + ')} · ${anomaly.recurrence}`;

  const recommendation = document.createElement('p');
  recommendation.className = 'anomaly-recommendation';
  recommendation.textContent = `${anomaly.investigation} No actor identification or intent attribution.`;

  const report = document.createElement('button');
  report.className = 'secondary-action';
  report.type = 'button';
  report.textContent = 'Copy Report';
  report.addEventListener('click', async () => {
    const text = buildAnomalyReport(anomaly);
    try {
      const clipboard = window.navigator?.clipboard;
      if (!clipboard?.writeText) throw new Error('Clipboard API unavailable');
      await clipboard.writeText(text);
      addEvent({ level: 'INFO', source: 'handoff', message: `report copied for ${anomaly.id}` });
    } catch (error) {
      report.textContent = 'Copy failed';
      report.disabled = true;
      addEvent({ level: 'WARN', source: 'handoff', message: `report copy failed for ${anomaly.id}: ${error?.message ?? error}` });
      setTimeout(() => {
        report.textContent = 'Copy Report';
        report.disabled = false;
      }, 2000);
    }
  });

  const reviewed = document.createElement('button');
  reviewed.className = 'primary-action';
  reviewed.type = 'button';
  reviewed.textContent = state.reviewedAnomalies.has(anomaly.id) ? 'Reviewed' : 'Mark Reviewed';
  reviewed.addEventListener('click', () => {
    state.reviewedAnomalies.add(anomaly.id);
    addEvent({ level: 'INFO', source: 'handoff', message: `${anomaly.id} marked reviewed` }, false);
    closeModal({ rerender: true });
  });

  const actions = document.createElement('div');
  actions.className = 'anomaly-actions';
  actions.append(report, reviewed, close);

  overlay.addEventListener('click', closeModal);
  sheet.append(badge, heading, meta, summary, reasons, recommendation, actions);
  overlay.appendChild(sheet);
  document.body.appendChild(overlay);
  close.focus();
}

function renderCompanion() {
  const c = state.companion ?? {};
  const online = c.status === 'online' || c.connected === true;
  $('companion-summary').textContent = online ? `${c.deviceId ?? 'Companion'} online. Last heartbeat ${formatAge(normalizeTimestamp(c.heartbeat ?? c.timestamp ?? Date.now()))} ago.` : 'No companion feed yet. Pair Android sensor or run ADB preflight.';
  $('companion-device').textContent = c.deviceId ?? '--';
  $('companion-heartbeat').textContent = c.heartbeat ? `${formatAge(normalizeTimestamp(c.heartbeat))} ago` : '--';
  $('companion-sequence').textContent = Number.isFinite(Number(c.sequence)) ? String(c.sequence) : '--';
  $('companion-rejects').textContent = String(c.rejects ?? c.rejected ?? 0);
  $('payload-rate').textContent = `${c.payloadRate ?? 0}/min`;
}

function renderChannelBars(aps) {
  const wrap = $('channel-bars');
  wrap.replaceChildren();
  const counts = new Map();
  for (const ap of aps) if (Number.isFinite(ap.channel)) counts.set(ap.channel, (counts.get(ap.channel) ?? 0) + 1);
  const entries = [...counts.entries()].sort((a, b) => Number(a[0]) - Number(b[0]));
  const max = Math.max(1, ...entries.map(([, count]) => count));
  for (const [channel, count] of entries) {
    const row = document.createElement('div');
    row.className = 'channel-row';
    const label = document.createElement('span');
    label.textContent = `CH ${channel}`;
    const track = document.createElement('div');
    track.className = 'channel-track';
    const fill = document.createElement('div');
    fill.className = 'channel-fill';
    fill.style.width = `${Math.max(6, (count / max) * 100)}%`;
    const value = document.createElement('span');
    value.textContent = String(count);
    track.appendChild(fill);
    row.append(label, track, value);
    wrap.appendChild(row);
  }
  if (!entries.length) {
    const empty = document.createElement('p');
    empty.className = 'muted';
    empty.textContent = 'No channel data yet.';
    wrap.appendChild(empty);
  }
}

function renderChart(aps) {
  const grid = $('chart-grid');
  const line = $('rssi-line');
  const points = $('rssi-points');
  grid.replaceChildren();
  points.replaceChildren();
  for (let y = 30; y <= 190; y += 40) {
    const el = document.createElementNS('http://www.w3.org/2000/svg', 'line');
    el.setAttribute('x1', '28');
    el.setAttribute('x2', '732');
    el.setAttribute('y1', String(y));
    el.setAttribute('y2', String(y));
    el.setAttribute('stroke', '#26334A');
    el.setAttribute('stroke-width', '1');
    el.setAttribute('opacity', '0.72');
    grid.appendChild(el);
  }
  const selected = state.selectedBssid ? state.aps.get(state.selectedBssid) : aps[0];
  const history = selected?.history?.length ? selected.history.slice(-24) : aps.filter((ap) => Number.isFinite(ap.rssi)).map((ap) => ({ rssi: ap.rssi, timestamp: ap.lastSeen })).slice(-24);
  if (!history.length) { line.setAttribute('points', ''); return; }
  const coords = history.map((sample, index) => {
    const x = 28 + (history.length === 1 ? 352 : (index / (history.length - 1)) * 704);
    const rssi = Math.max(-95, Math.min(-25, Number(sample.rssi)));
    const y = 30 + 160 - ((rssi + 95) / 70) * 160;
    return [x, y];
  });
  line.setAttribute('points', coords.map(([x, y]) => `${x},${y}`).join(' '));
  for (const [x, y] of coords) {
    const circle = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
    circle.setAttribute('cx', String(x));
    circle.setAttribute('cy', String(y));
    circle.setAttribute('r', '3.5');
    circle.setAttribute('fill', '#2196F3');
    points.appendChild(circle);
  }
}

function renderEvents() {
  $('event-log').textContent = state.events.slice().reverse().map((event) => `[${formatTime(event.time)}] ${event.level.padEnd(5)} ${event.source.padEnd(10)} ${event.message}`).join('\n');
  $('event-count').textContent = `${state.events.length} events`;
}

function render() {
  const aps = sortedAps();
  renderTopStatus();
  renderMetrics(aps);
  renderApTable(aps);
  renderInspector();
  renderThreats();
  renderCompanion();
  renderChannelBars(aps);
  renderChart(aps);
  renderEvents();
}

async function safeCall(name, ...args) {
  if (typeof api?.[name] !== 'function') {
    addEvent({ level: 'INFO', source: 'ui', message: `preload method unavailable: ${name}` });
    return null;
  }
  try {
    return await api[name](...args);
  } catch (error) {
    addEvent({ level: 'ERROR', source: 'ui', message: `${name} failed: ${error?.message ?? error}` });
    return null;
  }
}

function subscribe(name, handler) {
  if (typeof api?.[name] !== 'function') return;
  try {
    const unsubscribe = api[name](handler);
    if (typeof unsubscribe === 'function') window.addEventListener('beforeunload', unsubscribe, { once: true });
  } catch (error) {
    addEvent({ level: 'ERROR', source: 'ui', message: `${name} subscription failed: ${error?.message ?? error}` });
  }
}

async function initInterfaces() {
  const result = await safeCall('getInterfaces');
  const interfaces = Array.isArray(result) ? result : Array.isArray(result?.interfaces) ? result.interfaces : [];
  const select = $('iface-select');
  for (const iface of interfaces) {
    const name = typeof iface === 'string' ? iface : iface?.name;
    if (!name) continue;
    const option = document.createElement('option');
    option.value = name;
    option.textContent = name;
    select.appendChild(option);
  }
}

function wireControls() {
  $('start-scan').addEventListener('click', async () => {
    const iface = $('iface-select').value || undefined;
    const result = await safeCall('startScan', iface);
    state.scanState = { ...state.scanState, running: true, scanning: true, ...(result && typeof result === 'object' ? result : {}) };
    addEvent({ level: 'INFO', source: 'scanner', message: `start requested${iface ? ` on ${iface}` : ''}` });
    render();
  });
  $('stop-scan').addEventListener('click', async () => {
    const result = await safeCall('stopScan');
    state.scanState = { ...state.scanState, running: false, scanning: false, ...(result && typeof result === 'object' ? result : {}) };
    addEvent({ level: 'INFO', source: 'scanner', message: 'stop requested' });
    render();
  });
  $('manual-scan').addEventListener('click', async () => {
    const result = await safeCall('manualScan');
    if (Array.isArray(result) || Array.isArray(result?.aps) || Array.isArray(result?.networks)) setAps(result);
    addEvent({ level: 'INFO', source: 'scanner', message: 'manual scan requested' });
  });
  $('adb-preflight').addEventListener('click', async () => {
    const result = await safeCall('runAdbPreflight');
    if (result && typeof result === 'object') {
      state.adb = { ...result, status: String(result.status ?? (result.ready ? 'ready' : result.adbFound === false ? 'missing' : 'unknown')).toLowerCase() };
      addEvent({ level: state.adb.status === 'ready' ? 'INFO' : 'WATCH', source: 'adb', message: `ADB preflight: ${state.adb.status}` });
    }
    render();
  });
  for (const button of document.querySelectorAll('.nav-item')) {
    button.addEventListener('click', () => {
      document.querySelectorAll('.nav-item').forEach((item) => item.classList.remove('active'));
      button.classList.add('active');
      const view = button.dataset.view ?? 'overview';
      $('workspace-title').textContent = view === 'overview' ? 'Live RF Workspace' : button.textContent;
      $('workspace-subtitle').textContent = {
        overview: 'Observed APs, signal trends, channel pressure, and detector output.',
        inventory: 'Sortable AP/BSSID state from the shared scan store.',
        threats: 'Detector events separated from observed telemetry.',
        companion: 'Android companion and ADB ingestion health.',
        baseline: 'Known, new, missing, and changed network state.',
        exports: 'Session artifacts, redaction state, and reporting flow.',
      }[view] ?? 'Observed APs, signal trends, channel pressure, and detector output.';
    });
  }
}

function wireSubscriptions() {
  subscribe('onAps', setAps);
  subscribe('onRiskLog', setRiskSnapshot);
  subscribe('onScanState', (payload) => { if (payload && typeof payload === 'object') { state.scanState = { ...state.scanState, ...payload }; render(); } });
  subscribe('onCompanionUpdate', (payload) => { if (payload && typeof payload === 'object') { state.companion = { ...state.companion, ...payload }; addEvent({ level: 'INFO', source: 'companion', message: `update from ${payload.deviceId ?? 'device'}` }, false); render(); } });
  subscribe('onAppError', (payload) => addEvent({ level: 'ERROR', source: 'app', message: payload?.message ?? payload }));
}

function seedDemoIfEmpty() {
  if (state.aps.size) return;
  const session = summarizeHandoffSession();
  handoffWifiAps().forEach(upsertAp);
  handoffRisks().forEach((risk) => addRisk(risk, false));
  state.companion = {
    status: 'offline',
    deviceId: 'handoff prototype',
    payloadRate: 0,
    rejects: 0,
  };
  addEvent({
    level: 'INFO',
    source: 'handoff',
    message: `${session.title}: ${session.totalWifi} APs, ${session.totalAnomalies} anomalies seeded until live scanner data arrives`,
  }, false);
  $('workspace-subtitle').textContent = `${session.id} · ${session.duration} · ${session.totalAnomalies} anomalies · ${session.location}`;
}

async function boot() {
  wireControls();
  wireSubscriptions();
  const scanState = await safeCall('getScanState');
  if (scanState && typeof scanState === 'object') state.scanState = { ...state.scanState, ...scanState };
  await initInterfaces();
  seedDemoIfEmpty();
  render();
  setInterval(renderTopStatus, 1000);
}

boot().catch((error) => {
  console.error(error);
  addEvent({ level: 'ERROR', source: 'ui', message: `boot failed: ${error?.message ?? error}` });
  render();
});
