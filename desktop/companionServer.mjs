import { createServer } from 'node:http';
import { randomBytes, timingSafeEqual } from 'node:crypto';
import { WebSocketServer } from 'ws';

const MAX_STALE_MS = 2 * 60 * 1000;
const RATE_LIMIT_PER_WINDOW = 10;
const RATE_LIMIT_WINDOW_MS = 1_000;

export function isTimestampFresh(
  timestamp,
  nowMs = Date.now(),
  maxStalenessMs = MAX_STALE_MS,
) {
  return typeof timestamp === 'number' &&
    Math.abs(nowMs - timestamp) <= maxStalenessMs;
}

export function isSequenceMonotonic(deviceId, sequence, sessions) {
  const session = sessions.get(deviceId);
  return !session || sequence > session.sequence;
}

function newSession() {
  return { sequence: -1, rateCount: 0, rateWindowStart: Date.now() };
}

function formatAddress(addressInfo) {
  if (!addressInfo || typeof addressInfo === 'string') {
    return addressInfo ?? null;
  }

  const host = addressInfo.family === 'IPv6'
    ? `[${addressInfo.address}]`
    : addressInfo.address;
  return `${host}:${addressInfo.port}`;
}

function tokenMatches(candidate, expected) {
  if (typeof candidate !== 'string' || typeof expected !== 'string') {
    return false;
  }

  const candidateBuffer = Buffer.from(candidate);
  const expectedBuffer = Buffer.from(expected);
  return candidateBuffer.length === expectedBuffer.length &&
    timingSafeEqual(candidateBuffer, expectedBuffer);
}

export class CompanionServer {
  #token = null;
  #sessions = new Map();
  #httpServer = null;
  #wss = null;
  #onData = null;

  constructor({ onData } = {}) {
    this.#onData = onData ?? null;
  }

  generateToken() {
    this.#token = randomBytes(16).toString('hex');
    // Clear per-device sequence state so reconnects after token rotation
    // are not permanently rejected by the monotonic-sequence check.
    this.#sessions.clear();
    return this.#token;
  }

  get token() {
    return this.#token;
  }

  get address() {
    return formatAddress(this.#httpServer?.address() ?? null);
  }

  start(port = 0, host = '127.0.0.1') {
    if (this.#httpServer) return Promise.resolve(this.address);
    return new Promise((resolve, reject) => {
      this.#httpServer = createServer();
      this.#wss = new WebSocketServer({ server: this.#httpServer });
      this.#wss.on('connection', (ws) => this.#handleConnection(ws));
      this.#httpServer.on('error', reject);
      this.#httpServer.listen(port, host, () => resolve(this.address));
    });
  }

  stop() {
    return new Promise((resolve) => {
      if (!this.#httpServer) {
        resolve();
        return;
      }
      this.#sessions.clear();
      this.#wss?.close();
      this.#httpServer.close(() => {
        this.#httpServer = null;
        this.#wss = null;
        resolve();
      });
    });
  }

  #handleConnection(ws) {
    let authenticated = false;

    ws.on('message', (raw) => {
      let msg;
      try {
        msg = JSON.parse(raw.toString());
      } catch {
        ws.close(1008, 'Invalid JSON');
        return;
      }

      if (!authenticated) {
        if (
          msg.type !== 'auth' ||
          !this.#token ||
          !tokenMatches(msg.token, this.#token)
        ) {
          ws.close(1008, 'Authentication failed');
          return;
        }
        authenticated = true;
        ws.send(JSON.stringify({ type: 'auth_ok' }));
        return;
      }

      if (msg.type !== 'scan') return;
      if (!this.#validatePayload(ws, msg.payload)) return;

      const { deviceId, sequence } = msg.payload;
      const session = this.#sessions.get(deviceId) ?? newSession();
      session.sequence = sequence;
      this.#sessions.set(deviceId, session);

      this.#onData?.(msg.payload);
    });

    ws.on('error', () => {
      // Connection teardown is handled elsewhere.
    });
  }

  #validatePayload(ws, payload) {
    if (!payload || typeof payload !== 'object') return false;

    const { deviceId, sequence, timestamp, networks } = payload;

    if (typeof deviceId !== 'string' || !deviceId) return false;
    if (!Number.isInteger(sequence)) return false;
    if (!Array.isArray(networks)) return false;
    if (!isTimestampFresh(timestamp)) return false;
    if (!isSequenceMonotonic(deviceId, sequence, this.#sessions)) return false;

    if (!this.#checkRateLimit(deviceId)) {
      ws.close(1008, 'Rate limit exceeded');
      return false;
    }

    return true;
  }

  #checkRateLimit(deviceId) {
    const session = this.#sessions.get(deviceId) ?? newSession();
    const now = Date.now();

    if (now - session.rateWindowStart > RATE_LIMIT_WINDOW_MS) {
      session.rateCount = 0;
      session.rateWindowStart = now;
    }

    session.rateCount += 1;
    this.#sessions.set(deviceId, session);

    return session.rateCount <= RATE_LIMIT_PER_WINDOW;
  }
}
