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
  document.getElementById('pair-companion')?.addEventListener('click', () => {
    void pairCompanion();
  });
}

initPairing();
