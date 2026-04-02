import assert from 'node:assert/strict';
import test from 'node:test';
import { describeIntel, getMacIntel, lookupOui, normalizeMac } from './ouiIntel.js';

test('normalizeMac strips separators and uppercases the value', () => {
  assert.equal(normalizeMac('24:0a:c4-aa.bb'), '240AC4AABB');
});

test('lookupOui returns curated vendor data for known prefixes', () => {
  assert.deepEqual(lookupOui('24:0A:C4:11:22:33'), {
    vendor: 'Espressif Systems',
    type: 'Spy Cam Chip',
    threat: 'HIGH',
  });
});

test('getMacIntel classifies locally administered addresses as private', () => {
  const intel = getMacIntel('DA:11:22:33:44:55');
  assert.equal(intel.status, 'private');
  assert.equal(intel.macProfile.isLocallyAdministered, true);
});

test('describeIntel returns analyst-facing copy for known OUIs', () => {
  assert.deepEqual(describeIntel(getMacIntel('40:CB:C0:11:22:33')), {
    tone: 'high',
    headline: 'Apple AirTag',
    detail: 'BLE Tracker · HIGH confidence bucket',
  });
});
