import assert from 'node:assert/strict';
import test from 'node:test';
import { PORTAL_VARIANTS, formatScore } from './portalWorkbench.js';

test('portal variants expose at least one operator-class artifact', () => {
  assert.equal(PORTAL_VARIANTS[0].id, 'operator-generic');
  assert.equal(PORTAL_VARIANTS[0].artifactId, 'wscanplus-portal-test-001');
  assert.ok(PORTAL_VARIANTS[0].signals.length > 3);
});

test('formatScore returns a percent string', () => {
  assert.equal(formatScore(0.96), '96% fake confidence');
});
