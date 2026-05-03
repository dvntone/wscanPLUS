import { EventEmitter } from 'node:events';
import { spawn } from 'node:child_process';

const POLL_INTERVAL_MS = 15_000;
const DEFAULT_BASE_URL = 'http://localhost:8080';

/**
 * HTTP transport for the Rayhunter (EFF) cellular threat detection API.
 * Polls the Orbic RC400L on port 8080 (via adb forward) every 15s.
 *
 * Emits:
 *  - `connect`    → { url }
 *  - `disconnect` → { url }
 *  - `threats`    → { entries: CellularThreatEntry[] }
 *  - `stats`      → raw system-stats JSON
 *  - `error`      → Error instance
 *
 * CellularThreatEntry shape:
 *  { type: 'cellular', recordingName, severity, eventType, message, ts }
 */
export class RayhunterTransport extends EventEmitter {
  /** @type {string} */
  #baseUrl;
  /** @type {ReturnType<typeof setInterval> | null} */
  #intervalId = null;
  /** @type {boolean} */
  #connected = false;
  /** @type {{ name: string, eventCount: number } | null} */
  #lastReportedEntry = null;

  constructor(baseUrl = DEFAULT_BASE_URL) {
    super();
    this.#baseUrl = baseUrl;
  }

  get connected() {
    return this.#connected;
  }

  async init() {
    const proc = spawn('adb', ['forward', 'tcp:8080', 'tcp:8080']);
    proc.on('error', (e) => console.warn('[rayhunter] adb forward failed', e.message));
    await this.#poll();
    this.#intervalId = setInterval(() => void this.#poll(), POLL_INTERVAL_MS);
  }

  stop() {
    if (this.#intervalId !== null) {
      clearInterval(this.#intervalId);
      this.#intervalId = null;
    }
  }

  async #poll() {
    let manifest;
    try {
      const res = await fetch(`${this.#baseUrl}/api/qmdl-manifest`);
      if (!res.ok) throw new Error(`manifest HTTP ${res.status}`);
      manifest = await res.json();
    } catch (e) {
      this.emit('error', e instanceof Error ? e : new Error(String(e)));
      if (this.#connected) {
        this.#connected = false;
        this.emit('disconnect', { url: this.#baseUrl });
      }
      return;
    }

    if (!this.#connected) {
      this.#connected = true;
      this.emit('connect', { url: this.#baseUrl });
    }

    const current = manifest.current_entry;
    if (current?.name) {
      let report;
      try {
        const res = await fetch(`${this.#baseUrl}/api/analysis-report/${current.name}`);
        if (!res.ok) throw new Error(`analysis HTTP ${res.status}`);
        report = await res.json();
      } catch (e) {
        this.emit('error', e instanceof Error ? e : new Error(String(e)));
        report = null;
      }

      if (report) {
        const events = Array.isArray(report.events) ? report.events : [];
        const lastCount =
          this.#lastReportedEntry?.name === current.name
            ? this.#lastReportedEntry.eventCount
            : 0;

        if (events.length > lastCount) {
          const entries = events.slice(lastCount).map((ev) => ({
            type: 'cellular',
            recordingName: current.name,
            severity: ev.event_type?.severity ?? 'Informational',
            eventType: ev.event_type?.type ?? 'Unknown',
            message: ev.message ?? '',
            ts: Date.now(),
          }));
          this.emit('threats', { entries });
        }

        this.#lastReportedEntry = { name: current.name, eventCount: events.length };
      }
    }

    try {
      const res = await fetch(`${this.#baseUrl}/api/system-stats`);
      if (!res.ok) throw new Error(`stats HTTP ${res.status}`);
      const stats = await res.json();
      this.emit('stats', stats);
    } catch (e) {
      this.emit('error', e instanceof Error ? e : new Error(String(e)));
    }
  }
}
