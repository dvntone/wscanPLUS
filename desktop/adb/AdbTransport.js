import { EventEmitter } from 'node:events';
import { AdbServerClient } from '@yume-chan/adb';
import { AdbServerNodeTcpConnector } from '@yume-chan/adb-server-node-tcp';

const DEFAULT_HOST = '127.0.0.1';
const DEFAULT_PORT = 5037;
const WATCHDOG_PORT = 9000;

/**
 * ADB transport for WatchdogService (Android side ServerSocket on tcp:9000).
 *
 * Emits:
 *  - `connect`     → { serial }
 *  - `data`        → Buffer payload from WatchdogService
 *  - `disconnect`  → { serial }
 *  - `error`       → Error instance
 */
export class AdbTransport extends EventEmitter {
  /** @type {AdbServerClient} */
  #client;
  /** @type {string} */
  #bufferedText = '';
  /** @type {ReadableStreamDefaultReader<Uint8Array> | undefined} */
  #reader;
  /** @type {{ serial: string, deviceId: string | null, capabilities: object | null, seq: number | null, sentAt: number | null, receivedAt: number, latencyMs: number | null } | null} */
  #session = null;
  /** @type {{ readable: ReadableStream<Uint8Array>, writable: WritableStream<Uint8Array>, closed: Promise<undefined>, close: () => Promise<void>, transportId?: bigint } | undefined} */
  #socket;
  /** @type {string | undefined} */
  #connectedSerial;

  constructor(host = DEFAULT_HOST, port = DEFAULT_PORT) {
    super();
    const connector = new AdbServerNodeTcpConnector({ host, port });
    this.#client = new AdbServerClient(connector);
  }

  /**
   * Returns an array of connected device serial numbers.
   * @returns {Promise<string[]>}
   */
  async listDevices() {
    const devices = await this.#client.getDevices();
    return devices.map((d) => d.serial);
  }

  /**
   * Returns the last parsed hello session for the connected device.
   * `capabilities` is `null` when the Android side omits the key.
   * `seq` and `sentAt` are `null` when the Android side omits them (older builds).
   * `latencyMs` is `null` when `sentAt` was not present in the hello.
   *
   * @returns {{ serial: string, deviceId: string | null, capabilities: object | null, seq: number | null, sentAt: number | null, receivedAt: number, latencyMs: number | null } | null}
   */
  get session() {
    return this.#session;
  }

  /**
   * Connect to WatchdogService on tcp:9000 for the given device serial.
   * Emits `connect`, then `data` events as Buffers, and `disconnect` when closed.
   *
   * @param {string} serial
   * @returns {Promise<void>}
   */
  async connect(serial) {
    if (!serial) {
      throw new Error('serial is required to connect');
    }

    await this.disconnect();
    this.#bufferedText = '';
    this.#session = null;

    try {
      const socket = await this.#client.createDeviceConnection(
        { serial },
        `tcp:${WATCHDOG_PORT}`,
      );

      this.#connectedSerial = serial;
      this.#socket = socket;
      this.emit('connect', { serial });

      this.#startReadLoop(socket);
      this.#watchDisconnect(socket);
    } catch (error) {
      this.emit('error', error);
      throw error;
    }
  }

  async disconnect() {
    if (!this.#socket) {
      return;
    }

    const socket = this.#socket;
    this.#socket = undefined;

    const reader = this.#reader;
    this.#reader = undefined;

    if (reader) {
      try {
        await reader.cancel();
      } catch (error) {
        this.emit('error', error);
      }
    }

    try {
      await socket.close();
    } catch (error) {
      this.emit('error', error);
    } finally {
      await this.#handleDisconnect();
    }
  }

  #watchDisconnect(socket) {
    void Promise.allSettled([
      socket.closed.then(() => this.#reader?.cancel()),
      socket.transportId !== undefined
        ? this.#client
            .waitForDisconnect(socket.transportId)
            .then(() => this.#reader?.cancel())
        : Promise.resolve(),
    ]);
  }

  #startReadLoop(socket) {
    const reader = socket.readable.getReader();
    this.#reader = reader;

    const readLoop = async () => {
      try {
        // eslint-disable-next-line no-constant-condition
        while (true) {
          const { value, done } = await reader.read();
          if (done) {
            break;
          }

          if (value) {
            const buffer = Buffer.from(value);
            this.#handleIncomingBuffer(buffer);
            this.emit('data', buffer);
          }
        }
      } catch (error) {
        this.emit('error', error);
      } finally {
        reader.releaseLock();
        if (this.#reader === reader) {
          this.#reader = undefined;
        }
        await this.#handleDisconnect();
      }
    };

    void readLoop();
  }

  async #handleDisconnect() {
    if (!this.#connectedSerial) {
      return;
    }

    const serial = this.#connectedSerial;
    this.#connectedSerial = undefined;

    this.emit('disconnect', { serial });
  }

  #handleIncomingBuffer(buffer) {
    this.#bufferedText += buffer.toString('utf8');

    let newlineIndex = this.#bufferedText.indexOf('\n');
    while (newlineIndex >= 0) {
      const line = this.#bufferedText.slice(0, newlineIndex).trim();
      this.#bufferedText = this.#bufferedText.slice(newlineIndex + 1);

      if (line) {
        this.#handleJsonLine(line);
      }

      newlineIndex = this.#bufferedText.indexOf('\n');
    }
  }

  #handleJsonLine(line) {
    let message;
    try {
      message = JSON.parse(line);
    } catch {
      return;
    }

    if (message?.type !== 'hello') {
      return;
    }

    const receivedAt = Date.now();
    const sentAt = typeof message.sentAt === 'number' ? message.sentAt : null;
    this.#session = {
      serial: this.#connectedSerial ?? '',
      deviceId: typeof message.deviceId === 'string' ? message.deviceId : null,
      capabilities: message.capabilities ?? null,
      seq: typeof message.seq === 'number' ? message.seq : null,
      sentAt,
      receivedAt,
      latencyMs: sentAt !== null ? receivedAt - sentAt : null,
    };

    this.emit('hello', this.#session);
    void this.#sendAck(this.#session.seq ?? 0);
  }

  async #sendAck(seq) {
    if (!this.#socket) return;
    try {
      const ack = JSON.stringify({ type: 'ack', seq }) + '\n';
      const writer = this.#socket.writable.getWriter();
      try {
        await writer.write(new TextEncoder().encode(ack));
      } finally {
        writer.releaseLock();
      }
    } catch (error) {
      this.emit('error', error);
    }
  }
}
