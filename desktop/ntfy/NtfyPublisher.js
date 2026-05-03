import { readFile, writeFile, mkdir } from 'node:fs/promises';
import path from 'node:path';
import { randomUUID } from 'node:crypto';

const DEFAULT_BASE_URL = 'https://ntfy.sh';
const THROTTLE_MS = 5 * 60_000;
const TOPIC_FILE = 'ntfy-topic.json';

/**
 * Publishes threat alerts to an ntfy topic via HTTP POST.
 * Throttles identical alerts to one per 5 minutes.
 */
export class NtfyPublisher {
  /** @type {string} */
  #baseUrl;
  /** @type {string | null} */
  #topic;
  /** @type {Map<string, number>} */
  #throttleMap = new Map();

  constructor({ baseUrl = DEFAULT_BASE_URL, topic = null } = {}) {
    this.#baseUrl = baseUrl;
    this.#topic = topic;
  }

  get topic() {
    return this.#topic;
  }

  setTopic(topic) {
    this.#topic = topic;
  }

  async publish({ title, message, priority = 3, tags = ['warning'] }) {
    if (!this.#topic) return;

    const key = `${title}:${message}`;
    const last = this.#throttleMap.get(key);
    if (last !== undefined && Date.now() - last < THROTTLE_MS) return;
    this.#throttleMap.set(key, Date.now());

    try {
      await fetch(`${this.#baseUrl}/${this.#topic}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          title,
          message,
          priority,
          tags,
          actions: [{ action: 'view', label: 'Open wscan+', url: 'wscanplus://threats', clear: true }],
        }),
      });
    } catch (e) {
      console.warn('[ntfy] publish failed', e.message);
    }
  }
}

export function generateTopic() {
  return `wscanplus-${randomUUID().replace(/-/g, '').slice(0, 12)}`;
}

export async function loadTopic(configDir) {
  try {
    const raw = await readFile(path.join(configDir, TOPIC_FILE), 'utf8');
    return JSON.parse(raw).topic ?? null;
  } catch {
    return null;
  }
}

export async function saveTopic(configDir, topic) {
  try {
    await mkdir(configDir, { recursive: true });
    await writeFile(path.join(configDir, TOPIC_FILE), JSON.stringify({ topic }), 'utf8');
  } catch (e) {
    console.warn('[ntfy] failed to persist topic', e.message);
  }
}
