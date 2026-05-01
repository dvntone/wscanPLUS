/* global window, document, MutationObserver */

const api = window.wscan ?? window.wscanplus ?? window.wscanPlus ?? {};
const DEMO_BSSIDS = new Set([
  '9C:3A:AF:22:10:8B',
  '1E:89:41:77:A0:2C',
  'F2:1D:02:90:11:FE',
  '58:EF:68:41:90:7A',
]);

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
  badge.textContent = 'DEMO DATA';

  const text = document.createElement('p');
  text.id = 'data-mode-message';
  text.textContent = 'Sample AP rows and threat entries are layout placeholders only, not live evidence.';

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
    message.textContent = 'Live scanner or companion AP data has arrived. Seeded demo rows are hidden from the operator view.';
  }

  hideSeededDemoRows();
}

function hideSeededDemoRows() {
  const rows = document.querySelectorAll('#ap-table-body tr');
  for (const row of rows) {
    const bssid = row.children?.[1]?.textContent?.trim().toUpperCase();
    if (bssid && DEMO_BSSIDS.has(bssid)) {
      row.hidden = liveDataSeen;
      row.classList.add('demo-seeded-row');
      row.setAttribute('aria-label', 'Seeded demo row, hidden when live data is present');
    }
  }

  const threatItems = document.querySelectorAll('.threat-item');
  for (const item of threatItems) {
    const text = item.textContent ?? '';
    if (text.includes('F2:1D:02:90:11:FE') || text.includes('Unknown AP near trusted SSID pattern')) {
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
