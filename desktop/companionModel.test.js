import assert from 'node:assert/strict';
import test from 'node:test';
import { resolveLayout, summarizeDevices } from './companionModel.js';

test('resolveLayout maps narrow widths to portrait in auto mode', () => {
  assert.equal(resolveLayout(640, 'auto'), 'portrait');
});

test('resolveLayout maps medium widths to tablet in auto mode', () => {
  assert.equal(resolveLayout(900, 'auto'), 'tablet');
});

test('resolveLayout maps wide widths to wide in auto mode', () => {
  assert.equal(resolveLayout(1440, 'auto'), 'wide');
});

test('resolveLayout respects manual layout override', () => {
  assert.equal(resolveLayout(640, 'wide'), 'wide');
});

test('summarizeDevices returns warning summary when no devices are connected', () => {
  assert.deepEqual(summarizeDevices([]), {
    tone: 'warn',
    headline: 'No Android devices connected',
    detail: 'Attach a phone or emulator, then refresh the companion link.'
  });
});

test('summarizeDevices returns ok summary when devices are connected', () => {
  assert.deepEqual(summarizeDevices(['R58M123ABC']), {
    tone: 'ok',
    headline: '1 Android companion ready',
    detail: 'R58M123ABC'
  });
});

test('summarizeDevices surfaces adb errors explicitly', () => {
  assert.deepEqual(summarizeDevices([], 'adb server not running'), {
    tone: 'error',
    headline: 'ADB unavailable',
    detail: 'adb server not running'
  });
});
