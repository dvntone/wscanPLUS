import { jest } from '@jest/globals';

// --- Mocks must be declared before importing the module under test ---

global.fetch = jest.fn();

jest.unstable_mockModule('node:child_process', () => ({
  spawn: jest.fn().mockReturnValue({
    on: jest.fn(),
    stdout: { on: jest.fn() },
    stderr: { on: jest.fn() },
  }),
}));

const { spawn } = await import('node:child_process');
const { RayhunterTransport } = await import('./RayhunterTransport.js');

// ---------------------------------------------------------------------------

const makeManifest = (name = 'recording-1') => ({
  entries: [{ name }],
  current_entry: { name },
});

const makeReport = (events = []) => ({
  packet_timestamp: '2026-04-16T21:00:00Z',
  events,
});

const makeEvent = (
  severity = 'High',
  type = 'QualitativeWarning',
  message = 'Null cipher usage detected on NAS layer',
) => ({
  event_type: { type, severity },
  message,
});

const makeStats = () => ({
  battery_status: { level: 80, charging: false },
  disk_stats: { used: 100, total: 1000 },
  memory_stats: { used: 256, total: 512 },
});

const manifestOk = (name) => ({ ok: true, json: async () => makeManifest(name) });
const reportOk = (events) => ({ ok: true, json: async () => makeReport(events) });
const statsOk = () => ({ ok: true, json: async () => makeStats() });

beforeEach(() => {
  jest.clearAllMocks();
  jest.useFakeTimers();
});

afterEach(() => {
  jest.useRealTimers();
});

// 1. init() calls spawn with correct adb args
test('init() calls adb forward tcp:8080 tcp:8080', async () => {
  global.fetch.mockResolvedValue({ ok: false, status: 503, json: async () => ({}) });

  const transport = new RayhunterTransport();
  transport.on('error', () => {});  // required — EventEmitter throws unhandled error events
  await transport.init();
  transport.stop();

  expect(spawn).toHaveBeenCalledWith('adb', ['forward', 'tcp:8080', 'tcp:8080']);
});

// 2. adb forward error is non-fatal
test('adb forward error is non-fatal — init resolves without throwing', async () => {
  global.fetch.mockResolvedValue({ ok: false, status: 503, json: async () => ({}) });

  const errorProc = {
    on: jest.fn((event, cb) => {
      if (event === 'error') cb(new Error('adb not found'));
    }),
    stdout: { on: jest.fn() },
    stderr: { on: jest.fn() },
  };
  spawn.mockReturnValueOnce(errorProc);

  const transport = new RayhunterTransport();
  transport.on('error', () => {});  // required — EventEmitter throws unhandled error events
  await expect(transport.init()).resolves.toBeUndefined();
  transport.stop();
});

// 3. connect emitted on first successful manifest fetch
test('connect emitted on first successful manifest fetch', async () => {
  global.fetch
    .mockResolvedValueOnce(manifestOk())
    .mockResolvedValueOnce(reportOk())
    .mockResolvedValueOnce(statsOk());

  const transport = new RayhunterTransport();
  const events = [];
  transport.on('connect', ({ url }) => events.push(`connect:${url}`));

  await transport.init();
  transport.stop();

  expect(events).toEqual(['connect:http://localhost:8080']);
  expect(transport.connected).toBe(true);
});

// 4. threats emitted with correct CellularThreatEntry shape
test('threats emitted with correct shape for non-empty analysis report', async () => {
  global.fetch
    .mockResolvedValueOnce(manifestOk('rec-1'))
    .mockResolvedValueOnce(reportOk([makeEvent('High', 'QualitativeWarning', 'Null cipher usage')]))
    .mockResolvedValueOnce(statsOk());

  const transport = new RayhunterTransport();
  let capturedEntries;
  transport.on('threats', ({ entries }) => {
    capturedEntries = entries;
  });

  await transport.init();
  transport.stop();

  expect(capturedEntries).toHaveLength(1);
  expect(capturedEntries[0]).toMatchObject({
    type: 'cellular',
    recordingName: 'rec-1',
    severity: 'High',
    eventType: 'QualitativeWarning',
    message: 'Null cipher usage',
  });
  expect(typeof capturedEntries[0].ts).toBe('number');
});

// 5. analysis-report not re-emitted when event count unchanged across two polls
test('threats not re-emitted when event count unchanged on same recording', async () => {
  const event = makeEvent();
  global.fetch
    .mockResolvedValueOnce(manifestOk('rec-1'))
    .mockResolvedValueOnce(reportOk([event]))
    .mockResolvedValueOnce(statsOk())
    // second poll
    .mockResolvedValueOnce(manifestOk('rec-1'))
    .mockResolvedValueOnce(reportOk([event]))
    .mockResolvedValueOnce(statsOk());

  const transport = new RayhunterTransport();
  let threatCount = 0;
  transport.on('threats', () => {
    threatCount++;
  });

  await transport.init();
  await jest.advanceTimersByTimeAsync(15_000);
  transport.stop();

  expect(threatCount).toBe(1);
});

// 6. disconnect and error emitted after failure following connected state
test('error then disconnect emitted after manifest failure following connected state', async () => {
  global.fetch
    .mockResolvedValueOnce(manifestOk())
    .mockResolvedValueOnce(reportOk())
    .mockResolvedValueOnce(statsOk())
    // second poll: manifest fails
    .mockRejectedValueOnce(new Error('Connection refused'));

  const transport = new RayhunterTransport();
  const events = [];
  transport.on('connect', () => events.push('connect'));
  transport.on('error', () => events.push('error'));
  transport.on('disconnect', () => events.push('disconnect'));

  await transport.init();
  await jest.advanceTimersByTimeAsync(15_000);
  transport.stop();

  expect(events).toEqual(['connect', 'error', 'disconnect']);
});

// 7. stats event payload matches system-stats response
test('stats event payload matches system-stats response', async () => {
  const stats = makeStats();
  global.fetch
    .mockResolvedValueOnce(manifestOk())
    .mockResolvedValueOnce(reportOk())
    .mockResolvedValueOnce({ ok: true, json: async () => stats });

  const transport = new RayhunterTransport();
  let capturedStats;
  transport.on('stats', (s) => {
    capturedStats = s;
  });

  await transport.init();
  transport.stop();

  expect(capturedStats).toEqual(stats);
});

// 8. stop() prevents further polling after timers advance
test('stop() prevents further polling after timers advance', async () => {
  global.fetch
    .mockResolvedValueOnce(manifestOk())
    .mockResolvedValueOnce(reportOk())
    .mockResolvedValueOnce(statsOk());

  const transport = new RayhunterTransport();
  await transport.init();
  transport.stop();

  const callCount = global.fetch.mock.calls.length;
  await jest.advanceTimersByTimeAsync(60_000);

  expect(global.fetch.mock.calls.length).toBe(callCount);
});
