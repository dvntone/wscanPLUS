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
  /** @type {ReadableStreamDefaultReader<Uint8Array> | undefined} */
  #reader;
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
            this.emit('data', Buffer.from(value));
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
}
