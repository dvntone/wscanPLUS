/* global global, queueMicrotask */
import test from 'node:test';
import assert from 'node:assert/strict';

// Minimal DOM stubs — must be set before the module executes.
function makeElement() {
  return {
    style: {},
    textContent: '',
    className: '',
    querySelectorAll: () => [],
    querySelector: () => null,
    addEventListener: () => {},
    children: [],
  };
}

global.document = {
  getElementById: () => null,
  querySelector: () => null,
  querySelectorAll: () => [],
};
global.MutationObserver = class {
  constructor(_cb) {}
  observe() {}
  disconnect() {}
};

const { inspectorState, releaseObserverSuppression } = await import('./bssidInspector.js');

function resetState() {
  inspectorState.rendering = false;
  inspectorState.suppressObserver = false;
  inspectorState.pendingRender = false;
}

test('pendingRender is false initially', () => {
  resetState();
  assert.equal(inspectorState.pendingRender, false);
});

test('releaseObserverSuppression clears suppressObserver via microtask', async () => {
  resetState();
  inspectorState.suppressObserver = true;

  releaseObserverSuppression();
  assert.equal(inspectorState.suppressObserver, true, 'still suppressed before microtask flushes');

  await new Promise(queueMicrotask);
  assert.equal(inspectorState.suppressObserver, false);
});

test('releaseObserverSuppression clears pendingRender after replay', async () => {
  resetState();
  inspectorState.suppressObserver = true;
  inspectorState.pendingRender = true;

  releaseObserverSuppression();
  await new Promise(queueMicrotask);

  assert.equal(inspectorState.suppressObserver, false);
  assert.equal(inspectorState.pendingRender, false);
});

test('suppressObserver stays false when released without suppression', async () => {
  resetState();
  inspectorState.suppressObserver = false;

  releaseObserverSuppression();
  await new Promise(queueMicrotask);

  assert.equal(inspectorState.suppressObserver, false);
  assert.equal(inspectorState.pendingRender, false);
});
