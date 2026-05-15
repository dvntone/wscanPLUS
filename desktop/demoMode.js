/* global window, document, MutationObserver */

import { WIFI } from './handoffPrototypeModel.js';

const api = window.wscan ?? window.wscanplus ?? window.wscanPlus ?? {};

// Normalize to uppercase so dataset.bssid lookups always match regardless of model casing.
const HANDOFF_BSSIDS = new Set(WIFI.map((row) => row.bssid.toUpperCase()));

let liveDataSeen = false;
let domObserver = null;

function createBanner() {
  if (document.getElementById('demo-data-banner')) return;

  const workspace = document.querySelector('.workspace');
  const header = document.querySelector('.workspace-header');
  if (!workspace || !header) return;

  const banner = document.createElement('section');
  banner.id = 'demo-data-banner';
  banner.className = 'demo-data-banner';
  banner.setAttribute('role', 'status');
  banner.setAttribute('aria-live', 'polite');

  const badge = document.createElement('span');
  badge.id = 'data-mode-badge';
  badge.className = 'badge badge-warning';
  badge.textContent = 'HANDOFF DATA';

  const text = document.createElement('p');
  text.id = 'data-mode-message';
  text.textContent = 'Prototype AP rows and anomaly entries are seeded from the implementation handoff, not live evidence.';

  banner.append(badge, text);
  header.insertAdjacentElement('afterend', banner);
  workspace.classList.add('workspace-with-demo-banner');
}

function normalizePayload(payload) {
  if (Array.isArray(payload)) return payload;
  if (Array.isArray(payload?.aps)) return payload.aps;
  if (Array.isArray(payload?.networks)) return payload.networks;
  return [];
}

function markLiveMode() {
  if (liveDataSeen) return;
  liveDataSeen = true;

  const badge = document.getElementById('data-mode-badge');
  if (badge) {
    badge.className = 'badge badge-success';
    badge.textContent = 'LIVE DATA';
  }

  const message = document.getElementById('data-mode-message');
  if (message) {
    message.textContent = 'Live scanner or companion AP data has arrived. Seeded handoff rows are hidden from the operator view.';
  }

  hideSeededDemoRows();

  // Once live, seeded rows are permanently hidden — no further rerenders need observation.
  if (domObserver) {
    domObserver.disconnect();
    domObserver = null;
  }
}

function isHandoffThreatItem(item) {
  return item.dataset.source === 'handoff';
}

function hideSeededDemoRows() {
  const rows = document.querySelectorAll('#ap-table-body tr');
  for (const row of rows) {
    // dataset.bssid is the canonical identifier written by renderApTable.
    const bssid = row.dataset.bssid?.toUpperCase();
    if (bssid && HANDOFF_BSSIDS.has(bssid)) {
      row.hidden = liveDataSeen;
      row.classList.add('demo-seeded-row');
      row.setAttribute('aria-label', 'Seeded handoff row, hidden when live data is present');
    }
  }

  const threatItems = document.querySelectorAll('.threat-item');
  for (const item of threatItems) {
    if (isHandoffThreatItem(item)) {
      item.hidden = liveDataSeen;
      item.classList.add('demo-seeded-threat');
    }
  }
}

function subscribeToLiveAps() {
  if (typeof api.onAps !== 'function') return;
  try {
    api.onAps((payload) => {
      const aps = normalizePayload(payload);
      if (aps.length > 0) markLiveMode();
    });
  } catch {
    // Renderer already reports preload/subscription failures. Keep this guard silent.
  }
}

function observeTableRerenders() {
  const target = document.querySelector('.workspace');
  if (!target) return;
  domObserver = new MutationObserver(() => hideSeededDemoRows());
  domObserver.observe(target, { childList: true, subtree: true });
}

createBanner();
hideSeededDemoRows();
subscribeToLiveAps();
observeTableRerenders();
