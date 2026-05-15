/* global window, document, MutationObserver */

const api = window.wscan ?? window.wscanplus ?? window.wscanPlus ?? {};

let liveDataSeen = false;

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
}

function isHandoffThreatItem(item) {
  return item.dataset.source === 'handoff';
}

function hideSeededDemoRows() {
  const rows = document.querySelectorAll('#ap-table-body tr');
  for (const row of rows) {
    if (row.dataset.source === 'handoff') {
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
  const observer = new MutationObserver(() => hideSeededDemoRows());
  observer.observe(target, { childList: true, subtree: true });
}

createBanner();
hideSeededDemoRows();
subscribeToLiveAps();
observeTableRerenders();
