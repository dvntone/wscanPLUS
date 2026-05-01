/* global document, MutationObserver */

const RISK_RANK = new Map([
  ['HIGH', 3],
  ['WATCH', 2],
  ['LOW', 1],
  ['INFO', 0],
]);

const state = {
  filter: 'all',
  sort: 'risk',
  direction: 'desc',
  applying: false,
};

function cellText(row, index) {
  return row.children?.[index]?.textContent?.trim() ?? '';
}

function rssiValue(row) {
  const value = Number.parseInt(cellText(row, 2), 10);
  return Number.isFinite(value) ? value : -999;
}

function channelValue(row) {
  const value = Number.parseInt(cellText(row, 3), 10);
  return Number.isFinite(value) ? value : 999;
}

function riskValue(row) {
  return RISK_RANK.get(cellText(row, 5).toUpperCase()) ?? 0;
}

function ageValue(row) {
  const text = cellText(row, 6).toLowerCase();
  if (text === 'now') return 0;
  const match = text.match(/^(\d+)(s|m|h)$/);
  if (!match) return Number.MAX_SAFE_INTEGER;
  const count = Number.parseInt(match[1], 10);
  const unit = match[2];
  if (unit === 's') return count;
  if (unit === 'm') return count * 60;
  return count * 3600;
}

function compareText(a, b, index) {
  return cellText(a, index).localeCompare(cellText(b, index), undefined, { sensitivity: 'base' });
}

function compareRows(a, b) {
  let result = 0;

  switch (state.sort) {
    case 'ssid':
      result = compareText(a, b, 0);
      break;
    case 'bssid':
      result = compareText(a, b, 1);
      break;
    case 'rssi':
      result = rssiValue(a) - rssiValue(b);
      break;
    case 'channel':
      result = channelValue(a) - channelValue(b);
      break;
    case 'lastSeen':
      result = ageValue(a) - ageValue(b);
      break;
    case 'risk':
    default:
      result = riskValue(a) - riskValue(b);
      break;
  }

  return state.direction === 'asc' ? result : -result;
}

function rowMatchesFilter(row) {
  const ssid = cellText(row, 0);
  const security = cellText(row, 4).toUpperCase();
  const risk = cellText(row, 5).toUpperCase();

  switch (state.filter) {
    case 'high':
      return risk === 'HIGH';
    case 'watch':
      return risk === 'WATCH';
    case 'open':
      return security === 'OPEN';
    case 'hidden':
      return ssid === '<hidden>' || ssid.trim() === '';
    case 'all':
    default:
      return true;
  }
}

function getRows() {
  return Array.from(document.querySelectorAll('#ap-table-body tr'));
}

function updateSummary(visibleRows, totalRows) {
  const summary = document.getElementById('ap-control-summary');
  if (!summary) return;
  summary.textContent = `${visibleRows}/${totalRows} visible`;
}

function applyControls() {
  if (state.applying) return;
  const body = document.getElementById('ap-table-body');
  if (!body) return;

  state.applying = true;
  const rows = getRows().sort(compareRows);
  let visibleRows = 0;

  for (const row of rows) {
    if (row.hidden) {
      row.style.display = '';
      body.appendChild(row);
      continue;
    }

    const visible = rowMatchesFilter(row);
    row.style.display = visible ? '' : 'none';
    if (visible) visibleRows += 1;
    body.appendChild(row);
  }

  updateSummary(visibleRows, rows.filter((row) => !row.hidden).length);
  state.applying = false;
}

function mountControls() {
  if (document.getElementById('ap-inventory-controls')) return;

  const panel = document.querySelector('.ap-panel');
  const header = panel?.querySelector('.panel-header');
  if (!panel || !header) return;

  const controls = document.createElement('section');
  controls.id = 'ap-inventory-controls';
  controls.className = 'ap-inventory-controls';
  controls.setAttribute('aria-label', 'AP inventory filters and sorting');

  const filterLabel = document.createElement('label');
  filterLabel.textContent = 'Filter';
  filterLabel.setAttribute('for', 'ap-filter-select');

  const filter = document.createElement('select');
  filter.id = 'ap-filter-select';
  filter.innerHTML = `
    <option value="all">All</option>
    <option value="high">High risk</option>
    <option value="watch">Watch</option>
    <option value="open">Open security</option>
    <option value="hidden">Hidden SSID</option>
  `;

  const sortLabel = document.createElement('label');
  sortLabel.textContent = 'Sort';
  sortLabel.setAttribute('for', 'ap-sort-select');

  const sort = document.createElement('select');
  sort.id = 'ap-sort-select';
  sort.innerHTML = `
    <option value="risk">Risk</option>
    <option value="rssi">RSSI</option>
    <option value="lastSeen">Last seen</option>
    <option value="channel">Channel</option>
    <option value="ssid">SSID</option>
    <option value="bssid">BSSID</option>
  `;

  const direction = document.createElement('button');
  direction.id = 'ap-sort-direction';
  direction.className = 'secondary-action ap-sort-direction';
  direction.type = 'button';
  direction.textContent = 'DESC';
  direction.setAttribute('aria-label', 'Toggle sort direction');

  const summary = document.createElement('span');
  summary.id = 'ap-control-summary';
  summary.className = 'muted ap-control-summary';
  summary.textContent = '0/0 visible';

  filter.addEventListener('change', () => {
    state.filter = filter.value;
    applyControls();
  });

  sort.addEventListener('change', () => {
    state.sort = sort.value;
    applyControls();
  });

  direction.addEventListener('click', () => {
    state.direction = state.direction === 'asc' ? 'desc' : 'asc';
    direction.textContent = state.direction.toUpperCase();
    applyControls();
  });

  controls.append(filterLabel, filter, sortLabel, sort, direction, summary);
  header.insertAdjacentElement('afterend', controls);
}

function observeInventory() {
  const body = document.getElementById('ap-table-body');
  if (!body) return;

  const observer = new MutationObserver(() => {
    applyControls();
  });
  observer.observe(body, { childList: true, subtree: false });
}

mountControls();
observeInventory();
applyControls();
