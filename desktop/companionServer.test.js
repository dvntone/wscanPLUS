import test from 'node:test';
import assert from 'node:assert/strict';
import { WebSocket } from 'ws';
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

test('CompanionServer.address returns null before start and a formatted host:port string after start', async () => {
  const server = new CompanionServer();
  assert.equal(server.address, null);

  await server.start();
  try {
    assert.match(server.address, /^127\.0\.0\.1:\d+$/);
  } finally {
    await server.stop();
  }
});

test('CompanionServer authenticates successfully with a valid token', async () => {
  const server = new CompanionServer();
  const token = server.generateToken();
  const address = await server.start();
  try {
    const response = await new Promise((resolve, reject) => {
      const ws = new WebSocket(`ws://${address}`);
      ws.once('open', () => ws.send(JSON.stringify({ type: 'auth', token })));
      ws.once('message', (data) => { ws.close(); resolve(JSON.parse(data.toString())); });
      ws.once('error', reject);
    });
    assert.equal(response.type, 'auth_ok');
  } finally {
    await server.stop();
  }
});

test('CompanionServer closes connection on invalid token', async () => {
  const server = new CompanionServer();
  server.generateToken();
  const address = await server.start();
  try {
    const closeCode = await new Promise((resolve, reject) => {
      const ws = new WebSocket(`ws://${address}`);
      ws.once('open', () => ws.send(JSON.stringify({ type: 'auth', token: 'not-the-right-token' })));
      ws.once('close', (code) => resolve(code));
      ws.once('error', reject);
    });
    assert.equal(closeCode, 1008);
  } finally {
    await server.stop();
  }
});

test('CompanionServer closes connection on mismatched-length token', async () => {
  const server = new CompanionServer();
  server.generateToken();
  const address = await server.start();
  try {
    const closeCode = await new Promise((resolve, reject) => {
      const ws = new WebSocket(`ws://${address}`);
      ws.once('open', () => ws.send(JSON.stringify({ type: 'auth', token: 'short' })));
      ws.once('close', (code) => resolve(code));
      ws.once('error', reject);
    });
    assert.equal(closeCode, 1008);
  } finally {
    await server.stop();
  }
});
