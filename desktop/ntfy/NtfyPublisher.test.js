import { jest } from '@jest/globals';

// --- Mocks must be declared before importing the module under test ---

global.fetch = jest.fn();

jest.unstable_mockModule('node:fs/promises', () => ({
  readFile: jest.fn(),
  writeFile: jest.fn().mockResolvedValue(undefined),
  mkdir: jest.fn().mockResolvedValue(undefined),
}));

jest.unstable_mockModule('node:crypto', () => ({
  randomUUID: jest.fn().mockReturnValue('12345678-1234-1234-1234-123456789abc'),
}));

const { readFile, writeFile, mkdir } = await import('node:fs/promises');
const { NtfyPublisher, generateTopic, loadTopic, saveTopic } = await import('./NtfyPublisher.js');

// ---------------------------------------------------------------------------

beforeEach(() => {
  jest.clearAllMocks();
  jest.useFakeTimers();
});

afterEach(() => {
  jest.useRealTimers();
});

// 1. generateTopic() returns correct format
test('generateTopic() returns wscanplus- prefixed 12-char hex topic', () => {
  const topic = generateTopic();
  expect(topic).toMatch(/^wscanplus-[0-9a-f]{12}$/);
});

// 2. publish() calls fetch with correct URL and body
test('publish() posts to correct ntfy URL with JSON body', async () => {
  global.fetch.mockResolvedValue({ ok: true });

  const pub = new NtfyPublisher({ topic: 'wscanplus-test' });
  await pub.publish({ title: 'Rogue AP', message: 'DE:AD:BE:EF:FE:ED', priority: 4 });

  expect(global.fetch).toHaveBeenCalledWith(
    'https://ntfy.sh/wscanplus-test',
    expect.objectContaining({
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
    }),
  );
  const body = JSON.parse(global.fetch.mock.calls[0][1].body);
  expect(body.title).toBe('Rogue AP');
  expect(body.message).toBe('DE:AD:BE:EF:FE:ED');
  expect(body.priority).toBe(4);
  expect(Array.isArray(body.actions)).toBe(true);
});

// 3. publish() with no topic is a no-op
test('publish() with no topic skips fetch', async () => {
  const pub = new NtfyPublisher({});
  await pub.publish({ title: 'Test', message: 'body', priority: 4 });
  expect(global.fetch).not.toHaveBeenCalled();
});

// 4. Throttle: second publish within 5 min is skipped
test('second publish for same key within throttle window is skipped', async () => {
  global.fetch.mockResolvedValue({ ok: true });

  const pub = new NtfyPublisher({ topic: 'wscanplus-test' });
  await pub.publish({ title: 'Rogue AP', message: 'aa:bb:cc:dd:ee:ff', priority: 4 });
  await pub.publish({ title: 'Rogue AP', message: 'aa:bb:cc:dd:ee:ff', priority: 4 });

  expect(global.fetch).toHaveBeenCalledTimes(1);
});

// 5. Throttle: fires again after window expires
test('publish fires again after throttle window expires', async () => {
  global.fetch.mockResolvedValue({ ok: true });

  const pub = new NtfyPublisher({ topic: 'wscanplus-test' });
  await pub.publish({ title: 'Rogue AP', message: 'aa:bb:cc:dd:ee:ff', priority: 4 });

  jest.advanceTimersByTime(5 * 60_000 + 1);
  await pub.publish({ title: 'Rogue AP', message: 'aa:bb:cc:dd:ee:ff', priority: 4 });

  expect(global.fetch).toHaveBeenCalledTimes(2);
});

// 6. loadTopic returns null when file missing
test('loadTopic() returns null when file does not exist', async () => {
  readFile.mockRejectedValue(Object.assign(new Error('ENOENT'), { code: 'ENOENT' }));

  const result = await loadTopic('/fake/path');
  expect(result).toBeNull();
});

// 7. saveTopic + loadTopic round-trip
test('saveTopic() + loadTopic() round-trip works', async () => {
  readFile.mockResolvedValue(JSON.stringify({ topic: 'wscanplus-mytest12' }));

  await saveTopic('/fake/path', 'wscanplus-mytest12');
  const loaded = await loadTopic('/fake/path');

  expect(loaded).toBe('wscanplus-mytest12');
  expect(writeFile).toHaveBeenCalledWith(
    expect.stringContaining('ntfy-topic.json'),
    JSON.stringify({ topic: 'wscanplus-mytest12' }),
    'utf8',
  );
  expect(mkdir).toHaveBeenCalledWith('/fake/path', { recursive: true });
});

// 8. publish() fetch error is non-fatal
test('publish() swallows fetch errors without throwing', async () => {
  global.fetch.mockRejectedValue(new Error('Network error'));

  const pub = new NtfyPublisher({ topic: 'wscanplus-test' });
  await expect(
    pub.publish({ title: 'Test', message: 'body', priority: 4 }),
  ).resolves.toBeUndefined();
});
