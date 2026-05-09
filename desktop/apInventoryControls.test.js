/* global global, queueMicrotask */
import test from 'node:test';
import assert from 'node:assert/strict';

// Minimal DOM stubs — must be set before the module executes.
function makeElement(id = '') {
  return {
    id,
    children: [],
    style: {},
    textContent: '',
    querySelectorAll: () => [],
    addEventListener: () => {},
  };
}

global.document = {
  getElementById: () => makeElement(),
  querySelectorAll: () => [],
  createElement: (tag) => makeElement(),
};
global.MutationObserver = class {
  constructor(_cb) {}
  observe() {}
  disconnect() {}
};

const { state, releaseObserverSuppression } = await import('./apInventoryControls.js');

function resetState() {
  state.filter = 'all';
  state.sort = 'risk';
  state.direction = 'desc';
  state.applying = false;
  state.suppressObserver = false;
  state.pendingUpdate = false;
}

test('pendingUpdate is false initially', () => {
  resetState();
  assert.equal(state.pendingUpdate, false);
});

test('releaseObserverSuppression clears suppressObserver via microtask', async () => {
  resetState();
  state.suppressObserver = true;

  releaseObserverSuppression();
  assert.equal(state.suppressObserver, true, 'still suppressed before microtask flushes');

  await new Promise(queueMicrotask);
  assert.equal(state.suppressObserver, false);
});

test('releaseObserverSuppression clears pendingUpdate after replay', async () => {
  resetState();
  state.suppressObserver = true;
  state.pendingUpdate = true;

  releaseObserverSuppression();
  await new Promise(queueMicrotask);

  assert.equal(state.suppressObserver, false);
  assert.equal(state.pendingUpdate, false);
});

test('suppressObserver stays false when releaseObserverSuppression is called without suppression', async () => {
  resetState();
  state.suppressObserver = false;

  releaseObserverSuppression();
  await new Promise(queueMicrotask);

  assert.equal(state.suppressObserver, false);
  assert.equal(state.pendingUpdate, false);
});
