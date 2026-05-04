import test from 'node:test';
import assert from 'node:assert/strict';
import {
  PREFLIGHT_CLASSIFICATIONS,
  classifyPreflight,
  parseAdbDevices,
  summarizePreflight,
} from './adbPreflight.mjs';

test('parseAdbDevices captures adb metadata fields', () => {
  const devices = parseAdbDevices(`
List of devices attached
emulator-5554 device product:sdk_gphone64_x86_64 model:Pixel_8 device:emu64xa transport_id:1
`);

  assert.equal(devices.length, 1);
  assert.equal(devices[0].serial, 'emulator-5554');
  assert.equal(devices[0].model, 'Pixel_8');
  assert.equal(devices[0].product, 'sdk_gphone64_x86_64');
});

test('classifyPreflight chooses readiness based on device states', () => {
  assert.equal(
    classifyPreflight({ ok: true, devices: [{ state: 'unauthorized' }] }).level,
    PREFLIGHT_CLASSIFICATIONS.unauthorized.level,
  );
  assert.equal(
    classifyPreflight({ ok: true, devices: [{ state: 'device' }] }).level,
    PREFLIGHT_CLASSIFICATIONS.ready.level,
  );
});

test('summarizePreflight builds adb version summary and device list', () => {
  const summary = summarizePreflight(
    'Android Debug Bridge version 1.0.41\nVersion 35.0.1',
    'List of devices attached\nabc123\tdevice model:Pixel_9 product:husky device:husky\n',
  );

  assert.equal(summary.ok, true);
  assert.equal(summary.adbVersion, 'Android Debug Bridge version 1.0.41');
  assert.equal(summary.devices[0].serial, 'abc123');
  assert.equal(summary.classification.level, PREFLIGHT_CLASSIFICATIONS.ready.level);
});
