const apMap = new Map();
const riskMap = new Map();
let selectedBssid = null;

function formatRelTime(ts) {
  if (!ts) return '-';
  const d = Date.now() - ts;
  if (d < 5_000) return 'just now';
  if (d < 60_000) return `${Math.floor(d / 1_000)}s ago`;
  if (d < 3_600_000) return `${Math.floor(d / 60_000)}m ago`;
  return `${Math.floor(d / 3_600_000)}h ago`;
}

function formatTime(ts) {
  if (!ts) return '-';
  return new Date(ts).toLocaleTimeString();
}

function severityRank(sev) {
  return { high: 3, medium: 2, low: 1, info: 0 }[sev] ?? 0;
}

function signalClass(dbm) {
  if (dbm >= -50) return 'sig-strong';
  if (dbm >= -70) return '';
  return 'sig-weak';
}

function switchView(viewId) {
  for (const btn of document.querySelectorAll('.nav-btn')) {
    btn.classList.toggle('active', btn.dataset.view === viewId);
  }
  for (const view of document.querySelectorAll('.view')) {
    view.classList.toggle('active', view.id === `view-${viewId}`);
  }
}

function switchTab(tabId) {
  for (const btn of document.querySelectorAll('.tab')) {
    btn.classList.toggle('active', btn.dataset.tab === tabId);
  }
  for (const panel of document.querySelectorAll('.tab-panel')) {
    panel.classList.toggle('active', panel.id === `panel-${tabId}`);
  }
}

function updateScanControls(state) {
  document.getElementById('btn-start').disabled = state.scanning;
  document.getElementById('btn-stop').disabled = !state.scanning;
  const badge = document.getElementById('scan-badge');
  if (state.scanning) {
    badge.textContent = `Scanning - ${state.interface ?? '?'}`;
    badge.classList.add('scanning');
  } else {
    badge.textContent = 'Idle';
    badge.classList.remove('scanning');
  }
  document.getElementById('ap-count').textContent = apMap.size;
}

async function startScan() {
  const iface = document.getElementById('iface-select').value || undefined;
  const result = await window.wscan.startScan(iface);
  if (!result.ok) {
    appendLogEntry({ message: `Scan start failed: ${result.error}`, ts: Date.now() });
  }
}

async function stopScan() {
  await window.wscan.stopScan();
}

async function manualScan() {
  const btn = document.getElementById('btn-manual');
  btn.disabled = true;
  try {
    const result = await window.wscan.manualScan();
    if (!result.ok) {
      appendLogEntry({ message: `Manual scan failed: ${result.error}`, ts: Date.now() });
    }
  } finally {
    btn.disabled = false;
  }
}

async function populateInterfaces() {
  const result = await window.wscan.getInterfaces();
  if (!result.ok || result.interfaces.length === 0) return;
  const sel = document.getElementById('iface-select');
  for (const iface of result.interfaces) {
    const opt = document.createElement('option');
    opt.value = iface;
    opt.textContent = iface;
    sel.appendChild(opt);
  }
  if (result.interfaces.length === 1) {
    sel.value = result.interfaces[0];
  }
}

function handleAps(aps) {
  for (const ap of aps) {
    apMap.set(ap.bssid, ap);
  }
  document.getElementById('ap-count').textContent = apMap.size;
  renderApTable();
}

function renderApTable() {
  const tbody = document.getElementById('ap-tbody');
  const rows = Array.from(apMap.values())
    .sort((a, b) => b.signal - a.signal)
    .slice(0, 200);

  const frag = document.createDocumentFragment();
  for (const ap of rows) {
    frag.appendChild(buildApRow(ap, riskMap.get(ap.bssid)));
  }
  tbody.replaceChildren(frag);
}

function buildApRow(ap, risk) {
  const tr = document.createElement('tr');
  tr.dataset.bssid = ap.bssid;
  if (ap.bssid === selectedBssid) tr.classList.add('selected');

  const cells = [
    () => {
      const td = document.createElement('td');
      if (ap.ssid) {
        td.textContent = ap.ssid;
      } else {
        td.textContent = '(hidden)';
        td.classList.add('dim');
      }
      return td;
    },
    () => {
      const td = document.createElement('td');
      td.textContent = ap.bssid;
      td.classList.add('mono');
      return td;
    },
    () => {
      const td = document.createElement('td');
      td.textContent = `${ap.signal} dBm`;
      const cls = signalClass(ap.signal);
      if (cls) td.classList.add(cls);
      return td;
    },
    () => {
      const td = document.createElement('td');
      td.textContent = ap.channel || '?';
      td.classList.add('mono');
      return td;
    },
    () => {
      const td = document.createElement('td');
      td.textContent = ap.security || 'open';
      if (ap.security === 'open' || !ap.security) td.classList.add('sec-open');
      return td;
    },
    () => {
      const td = document.createElement('td');
      if (risk) {
        const span = document.createElement('span');
        span.className = `risk-badge risk-${risk.severity}`;
        span.textContent = risk.severity;
        td.appendChild(span);
      }
      return td;
    },
    () => {
      const td = document.createElement('td');
      td.textContent = formatRelTime(ap.lastSeen);
      td.classList.add('dim');
      return td;
    },
  ];

  for (const build of cells) tr.appendChild(build());

  tr.addEventListener('click', () => selectAp(ap.bssid));
  return tr;
}

function handleRiskLog(log) {
  riskMap.clear();
  for (const entry of log) riskMap.set(entry.bssid, entry);
  document.getElementById('threat-count').textContent = log.length;
  const badge = document.getElementById('threat-count');
  if (log.length > 0) {
    badge.classList.add('badge-danger');
  } else {
    badge.classList.remove('badge-danger');
  }
  renderApTable();
  renderThreatQueue();
  if (selectedBssid) renderInspector(selectedBssid);
}

function renderThreatQueue() {
  const list = document.getElementById('threat-list');
  const sorted = Array.from(riskMap.values())
    .sort((a, b) => severityRank(b.severity) - severityRank(a.severity));

  const frag = document.createDocumentFragment();
  for (const entry of sorted) {
    const ap = apMap.get(entry.bssid);

    const li = document.createElement('li');
    li.className = `threat-item ${entry.severity}`;

    const header = document.createElement('div');
    header.className = 'threat-header';

    const ssidEl = document.createElement('span');
    ssidEl.className = 'threat-ssid';
    ssidEl.textContent = ap?.ssid || entry.ssid || '(hidden)';

    const sevEl = document.createElement('span');
    sevEl.className = `risk-badge risk-${entry.severity}`;
    sevEl.textContent = entry.severity;

    header.appendChild(ssidEl);
    header.appendChild(sevEl);
    li.appendChild(header);

    const bssidEl = document.createElement('div');
    bssidEl.className = 'threat-bssid mono';
    bssidEl.textContent = entry.bssid;
    li.appendChild(bssidEl);

    for (const reason of entry.reasons) {
      const r = document.createElement('div');
      r.className = 'threat-reason';
      r.textContent = reason;
      li.appendChild(r);
    }

    li.addEventListener('click', () => {
      selectAp(entry.bssid);
      switchView('scan');
    });

    frag.appendChild(li);
  }
  list.replaceChildren(frag);
}

function selectAp(bssid) {
  selectedBssid = bssid;
  for (const tr of document.querySelectorAll('#ap-tbody tr')) {
    tr.classList.toggle('selected', tr.dataset.bssid === bssid);
  }
  renderInspector(bssid);
}

function renderInspector(bssid) {
  const ap = apMap.get(bssid);
  const risk = riskMap.get(bssid);
  const content = document.getElementById('inspector-content');

  if (!ap) {
    const p = document.createElement('p');
    p.className = 'inspector-empty';
    p.textContent = 'Select an AP to inspect';
    content.replaceChildren(p);
    return;
  }

  const frag = document.createDocumentFragment();

  const fields = [
    ['SSID', ap.ssid || '(hidden)'],
    ['BSSID', ap.bssid],
    ['Signal', `${ap.signal} dBm`],
    ['Frequency', ap.frequency ? `${ap.frequency} MHz` : '-'],
    ['Channel', ap.channel || '-'],
    ['Security', ap.security || 'open'],
    ['Source', ap.source || 'desktop'],
    ['Last seen', formatRelTime(ap.lastSeen)],
  ];

  for (const [label, value] of fields) {
    const row = document.createElement('div');
    row.className = 'insp-row';

    const lbl = document.createElement('div');
    lbl.className = 'insp-label';
    lbl.textContent = label;

    const val = document.createElement('div');
    val.className = 'insp-value mono';
    val.textContent = value;

    row.appendChild(lbl);
    row.appendChild(val);
    frag.appendChild(row);
  }

  if (risk) {
    const divider = document.createElement('div');
    divider.className = 'insp-divider';
    frag.appendChild(divider);

    const section = document.createElement('div');
    section.className = 'insp-section';

    const badge = document.createElement('span');
    badge.className = `risk-badge risk-${risk.severity}`;
    badge.textContent = risk.severity;

    const conf = document.createElement('span');
    conf.className = 'insp-conf';
    conf.textContent = `${Math.round(risk.confidence * 100)}% confidence`;

    section.appendChild(badge);
    section.appendChild(conf);
    frag.appendChild(section);

    for (const reason of risk.reasons) {
      const r = document.createElement('div');
      r.className = 'insp-reason';
      r.textContent = `- ${reason}`;
      frag.appendChild(r);
    }
  }

  content.replaceChildren(frag);
}

function appendLogEntry(entry) {
  const container = document.getElementById('log-entries');

  const div = document.createElement('div');
  div.className = 'log-entry';

  const ts = document.createElement('span');
  ts.className = 'log-ts';
  ts.textContent = formatTime(entry.ts);

  const msg = document.createElement('span');
  msg.className = 'log-msg';
  msg.textContent = entry.message;

  div.appendChild(ts);
  div.appendChild(msg);

  container.insertBefore(div, container.firstChild);
  while (container.childElementCount > 200) {
    container.removeChild(container.lastChild);
  }
}

async function pairCompanion() {
  const result = await window.wscan.pairCompanion();
  if (!result.ok) {
    appendLogEntry({ message: `Companion pair failed: ${result.error}`, ts: Date.now() });
    return;
  }
  const block = document.getElementById('token-block');
  block.hidden = false;
  document.getElementById('token-value').textContent = result.token;
  const addr = result.address;
  document.getElementById('token-addr').textContent = addr
    ? `ws://${addr.address}:${addr.port}`
    : '-';
}

function handleCompanionUpdate(update) {
  const list = document.getElementById('companion-device-list');

  let item = null;
  for (const li of list.querySelectorAll('.companion-device-item')) {
    if (li.dataset.deviceId === update.deviceId) {
      item = li;
      break;
    }
  }

  if (!item) {
    const empty = list.querySelector('.companion-empty');
    if (empty) empty.remove();
    item = document.createElement('li');
    item.className = 'companion-device-item';
    item.dataset.deviceId = update.deviceId;
    list.appendChild(item);
  }

  item.textContent = '';

  const id = document.createElement('span');
  id.className = 'mono';
  id.textContent = update.deviceId.slice(0, 16);
  item.appendChild(id);

  const detail = document.createElement('span');
  detail.className = 'companion-detail';
  detail.textContent = ` - ${update.networkCount} networks - ${formatTime(update.timestamp)}`;
  item.appendChild(detail);
}

async function runPreflight() {
  const statusEl = document.getElementById('status');
  const guidanceEl = document.getElementById('guidance');
  const runBtn = document.getElementById('run-preflight');
  const listEl = document.getElementById('device-list');

  statusEl.textContent = 'Running ADB preflight...';
  guidanceEl.textContent = '';
  runBtn.disabled = true;
  listEl.replaceChildren();

  try {
    const result = await window.wscan.runAdbPreflight();
    const cls = result.classification;

    if (!result.ok) {
      statusEl.textContent = cls.title;
      guidanceEl.textContent = `${cls.guidance} ${result.error}`;
      return;
    }

    statusEl.textContent = `${result.adbVersion} | ${cls.title}`;
    guidanceEl.textContent = cls.guidance;

    for (const device of result.devices) {
      const li = document.createElement('li');

      const parts = [device.model || 'Unknown model', `state=${device.state}`];
      if (device.product) parts.push(`product=${device.product}`);
      if (device.device) parts.push(`device=${device.device}`);
      if (device.companion?.status && device.companion.status !== 'unchecked') {
        parts.push(`companion=${device.companion.status}`);
        if (device.companion.versionName) {
          parts.push(`version=${device.companion.versionName}`);
        }
      }

      const main = document.createElement('div');
      main.textContent = parts.join(' | ');
      li.appendChild(main);

      if (device.readiness?.label) {
        const next = document.createElement('div');
        next.textContent = `next: ${device.readiness.label}`;
        li.appendChild(next);
      }
      if (device.readiness?.guidance) {
        const guide = document.createElement('div');
        guide.textContent = device.readiness.guidance;
        li.appendChild(guide);
      }

      listEl.appendChild(li);
    }
  } finally {
    runBtn.disabled = false;
  }
}

async function init() {
  for (const btn of document.querySelectorAll('.nav-btn')) {
    btn.addEventListener('click', () => switchView(btn.dataset.view));
  }

  for (const btn of document.querySelectorAll('.tab')) {
    btn.addEventListener('click', () => switchTab(btn.dataset.tab));
  }

  document.getElementById('btn-start').addEventListener('click', () => void startScan());
  document.getElementById('btn-stop').addEventListener('click', () => void stopScan());
  document.getElementById('btn-manual').addEventListener('click', () => void manualScan());
  document.getElementById('run-preflight').addEventListener('click', () => void runPreflight());
  document.getElementById('btn-pair').addEventListener('click', () => void pairCompanion());

  window.wscan.onAps(handleAps);
  window.wscan.onRiskLog(handleRiskLog);
  window.wscan.onScanState(updateScanControls);
  window.wscan.onCompanionUpdate(handleCompanionUpdate);
  window.wscan.onAppError(appendLogEntry);

  const scanState = await window.wscan.getScanState();
  updateScanControls(scanState);
  await populateInterfaces();
}

void init();
