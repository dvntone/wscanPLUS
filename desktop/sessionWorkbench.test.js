import assert from 'node:assert/strict';
import test from 'node:test';
import {
  SAMPLE_SESSIONS,
  describeSession,
  narrativeTone,
  parseSessionArtifact,
  summarizeSessions,
  matchesSessionQuery,
} from './sessionWorkbench.js';

test('sample sessions reflect scan session ids and narrative fields', () => {
  assert.equal(typeof SAMPLE_SESSIONS[0].id, 'number');
  assert.equal(typeof SAMPLE_SESSIONS[0].narrative, 'string');
  assert.equal(typeof SAMPLE_SESSIONS[0].signalCount, 'number');
});

test('describeSession returns an operator-facing summary', () => {
  assert.deepEqual(describeSession(SAMPLE_SESSIONS[0]), {
    title: 'Session 104',
    subtitle: 'MULTI_UNIT_HOUSING · android-lab-01',
    detail: '4 signals · gemini-2.5-flash',
  });
});

test('narrativeTone maps session status to display tone', () => {
  assert.equal(narrativeTone(SAMPLE_SESSIONS[0]), 'high');
  assert.equal(narrativeTone(SAMPLE_SESSIONS[1]), 'ok');
  assert.equal(narrativeTone(SAMPLE_SESSIONS[2]), 'warn');
});

test('parseSessionArtifact normalizes sessions plus narratives from exported JSON', () => {
  const sessions = parseSessionArtifact(
    JSON.stringify({
      sessions: [
        {
          id: 7,
          startedAt: 1775047560000,
          endedAt: 1775047860000,
          environmentType: 'HOTEL',
          deviceSerial: 'android-lab-03',
        },
      ],
      narratives: [
        {
          sessionId: 7,
          narrative: 'Review front-desk portal conditions.',
          generatedAt: 1775047900000,
          signalCount: 2,
          modelName: 'gemini-2.5-flash',
        },
      ],
    })
  );

  assert.deepEqual(sessions[0], {
    id: 7,
    startedAt: '2026-04-01 12:46',
    endedAt: '2026-04-01 12:51',
    environmentType: 'HOTEL',
    deviceSerial: 'android-lab-03',
    signalCount: 2,
    modelName: 'gemini-2.5-flash',
    narrative: 'Review front-desk portal conditions.',
    status: 'review',
  });
});

test('parseSessionArtifact rejects invalid artifact shapes', () => {
  assert.throws(() => parseSessionArtifact(JSON.stringify({ nope: [] })), /sessions array/);
});

test('summarizeSessions totals session statuses and signal counts', () => {
  assert.deepEqual(summarizeSessions(SAMPLE_SESSIONS), {
    total: 3,
    suspect: 1,
    review: 1,
    clear: 1,
    signals: 6,
  });
});

test('matchesSessionQuery filters by query and status', () => {
  assert.equal(matchesSessionQuery(SAMPLE_SESSIONS[0], 'corridor', 'all'), true);
  assert.equal(matchesSessionQuery(SAMPLE_SESSIONS[1], 'corridor', 'all'), false);
  assert.equal(matchesSessionQuery(SAMPLE_SESSIONS[2], '', 'review'), true);
  assert.equal(matchesSessionQuery(SAMPLE_SESSIONS[2], '', 'suspect'), false);
});
