/* global window, document */

const api = window.wscan ?? window.wscanplus ?? window.wscanPlus ?? {};

function text(id, value) {
  const el = document.getElementById(id);
  if (el) el.textContent = value;
}

function showTokenBlock() {
  const block = document.getElementById('pairing-token-block');
  if (block) block.hidden = false;
}

function logPairingEvent(level, message) {
  const log = document.getElementById('event-log');
  const count = document.getElementById('event-count');
  if (!log) return;

  const line = `[${new Date().toLocaleTimeString([], { hour12: false })}] ${level.padEnd(5)} companion  ${message}`;
  log.textContent = log.textContent ? `${log.textContent}\n${line}` : line;

  if (count) {
    const current = Number.parseInt(count.textContent, 10);
    count.textContent = `${Number.isFinite(current) ? current + 1 : 1} events`;
  }
}

function formatAddress(result) {
  if (typeof result?.endpoint === 'string') return result.endpoint;
  if (typeof result?.url === 'string') return result.url;
  if (result?.address?.address && result?.address?.port) {
    return `ws://${result.address.address}:${result.address.port}`;
  }
  if (result?.address && result?.port) return `ws://${result.address}:${result.port}`;
  return 'Pairing endpoint unavailable';
}

function mountPairingControls() {
  if (document.getElementById('pair-companion')) return;
  const panel = document.querySelector('.companion-panel');
  if (!panel) return;

  const section = document.createElement('section');
  section.className = 'pairing-box';
  section.setAttribute('aria-label', 'Companion pairing');

  const button = document.createElement('button');
  button.id = 'pair-companion';
  button.className = 'secondary-action pairing-action';
  button.type = 'button';
  button.textContent = 'Generate Pairing Token';

  const block = document.createElement('div');
  block.id = 'pairing-token-block';
  block.className = 'pairing-token-block';
  block.hidden = true;

  const label = document.createElement('div');
  label.className = 'muted pairing-label';
  label.textContent = 'Token';

  const token = document.createElement('div');
  token.id = 'pairing-token-value';
  token.className = 'pairing-token-value';

  const address = document.createElement('div');
  address.id = 'pairing-token-address';
  address.className = 'muted pairing-token-address';

  block.append(label, token, address);
  section.append(button, block);
  panel.appendChild(section);
}

async function pairCompanion() {
  const button = document.getElementById('pair-companion');
  if (button) button.disabled = true;

  try {
    if (typeof api.pairCompanion !== 'function') {
      throw new Error('pairCompanion preload method unavailable');
    }

    const result = await api.pairCompanion();
    if (result?.ok === false) {
      throw new Error(result.error ?? 'pairing failed');
    }

    text('pairing-token-value', String(result?.token ?? 'No token returned'));
    text('pairing-token-address', formatAddress(result));
    showTokenBlock();
    logPairingEvent('INFO', 'pairing token generated');
  } catch (error) {
    text('pairing-token-value', 'Pairing failed');
    text('pairing-token-address', error instanceof Error ? error.message : String(error));
    showTokenBlock();
    logPairingEvent('ERROR', error instanceof Error ? error.message : String(error));
  } finally {
    if (button) button.disabled = false;
  }
}

function initPairing() {
  mountPairingControls();
  document.getElementById('pair-companion')?.addEventListener('click', () => {
    void pairCompanion();
  });
}

initPairing();
