import { AdbServerClient } from '@yume-chan/adb';
import { AdbServerNodeTcpConnector } from '@yume-chan/adb-server-node-tcp';

const DEFAULT_HOST = '127.0.0.1';
const DEFAULT_PORT = 5037;

/**
 * Thin wrapper around AdbServerClient for wscan+ desktop.
 *
 * Responsibilities:
 *  - List connected devices via the local ADB server.
 *  - Forward the WatchdogService TCP port (device:9000 → localhost:PORT).
 *  - Remove the forward when done.
 */
export class AdbTransport {
    /** @type {AdbServerClient} */
    #client;

    constructor(host = DEFAULT_HOST, port = DEFAULT_PORT) {
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
     * Forwards localhost:<localPort> → <serial> device port 9000.
     * Equivalent to: adb -s <serial> forward tcp:<localPort> tcp:9000
     *
     * @param {string} serial    ADB device serial
     * @param {number} localPort Host-side port to bind
     * @returns {Promise<void>}
     */
    async forwardWatchdog(serial, localPort) {
        const service = `host-serial:${serial}:forward:tcp:${localPort};tcp:9000`;
        const stream = await this.#client.createConnection(service);
        await stream.dispose();
    }

    /**
     * Removes the TCP forward for the given serial and local port.
     * Equivalent to: adb -s <serial> forward --remove tcp:<localPort>
     *
     * @param {string} serial
     * @param {number} localPort
     * @returns {Promise<void>}
     */
    async removeForward(serial, localPort) {
        const service = `host-serial:${serial}:killforward:tcp:${localPort}`;
        const stream = await this.#client.createConnection(service);
        await stream.dispose();
    }
}
