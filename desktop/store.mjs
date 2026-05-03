import { EventEmitter } from 'node:events';

const RISK_LOG_MAX = 1000;
const RISK_LOG_TTL_MS = 60_000;
const CELLULAR_LOG_MAX = 200;
const CELLULAR_TTL_MS = 5 * 60_000;

class Store extends EventEmitter {
  #state;

  constructor() {
    super();
    this.#state = {
      scanning: false,
      scanIntervalMs: 30_000,
      interface: null,
      aps: new Map(),
      riskLog: [],
      errors: [],
      companionDevices: new Map(),
      cellularThreats: [],
      rayhunterStats: null,
    };
  }

  get state() {
    return this.#state;
  }

  update(patch) {
    Object.assign(this.#state, patch);
    this.emit('change', this.#state);
  }

  addAps(newAps) {
    const now = Date.now();
    for (const ap of newAps) {
      this.#state.aps.set(ap.bssid, { ...ap, lastSeen: now });
    }
    this.emit('aps', Array.from(this.#state.aps.values()));
  }

  addRiskEntries(entries) {
    const now = Date.now();

    this.#state.riskLog = this.#state.riskLog.filter(
      (e) => now - e.ts < RISK_LOG_TTL_MS,
    );

    for (const entry of entries) {
      const key = `${entry.bssid}:${entry.severity}`;
      const idx = this.#state.riskLog.findIndex(
        (e) => `${e.bssid}:${e.severity}` === key,
      );
      if (idx >= 0) {
        this.#state.riskLog[idx] = { ...entry, ts: now };
      } else {
        this.#state.riskLog.push({ ...entry, ts: now });
      }
    }

    if (this.#state.riskLog.length > RISK_LOG_MAX) {
      this.#state.riskLog = this.#state.riskLog.slice(-RISK_LOG_MAX);
    }

    this.emit('riskLog', this.#state.riskLog);
  }

  addCellularThreats(entries) {
    const now = Date.now();

    this.#state.cellularThreats = this.#state.cellularThreats.filter(
      (e) => now - e.ts < CELLULAR_TTL_MS,
    );

    for (const entry of entries) {
      const key = `${entry.recordingName}:${entry.eventType}:${entry.message}`;
      const idx = this.#state.cellularThreats.findIndex(
        (e) => `${e.recordingName}:${e.eventType}:${e.message}` === key,
      );
      if (idx >= 0) {
        this.#state.cellularThreats[idx] = { ...entry, ts: now };
      } else {
        this.#state.cellularThreats.push({ ...entry, ts: now });
      }
    }

    if (this.#state.cellularThreats.length > CELLULAR_LOG_MAX) {
      this.#state.cellularThreats = this.#state.cellularThreats.slice(-CELLULAR_LOG_MAX);
    }

    this.emit('cellularThreats', this.#state.cellularThreats);
  }

  updateRayhunterStats(stats) {
    this.#state.rayhunterStats = stats;
    this.emit('rayhunterStats', stats);
  }

  addError(err) {
    const entry = {
      message: err instanceof Error ? err.message : String(err),
      ts: Date.now(),
    };
    this.#state.errors.push(entry);
    if (this.#state.errors.length > 100) {
      this.#state.errors = this.#state.errors.slice(-100);
    }
    this.emit('appError', entry);
  }
}

export const store = new Store();

export default store;
