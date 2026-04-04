import test from 'node:test';
import assert from 'node:assert/strict';
import {
  CompanionServer,
  isSequenceMonotonic,
  isTimestampFresh,
} from './companionServer.mjs';

test('isTimestampFresh rejects stale timestamps', () => {
  const now = 1_000_000;
  assert.equal(isTimestampFresh(now - 30_000, now), true);
  assert.equal(isTimestampFresh(now - 500_000, now), false);
});

test('isSequenceMonotonic enforces increasing sequences', () => {
  const sessions = new Map([
    ['device-1', { sequence: 4 }],
  ]);

  assert.equal(isSequenceMonotonic('device-1', 5, sessions), true);
  assert.equal(isSequenceMonotonic('device-1', 4, sessions), false);
  assert.equal(isSequenceMonotonic('device-2', 0, sessions), true);
});

test('CompanionServer generates a pairing token', () => {
  const server = new CompanionServer();
  const token = server.generateToken();

  assert.match(token, /^[a-f0-9]{32}$/);
  assert.equal(server.token, token);
});
