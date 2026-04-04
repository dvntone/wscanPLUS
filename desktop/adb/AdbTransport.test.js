import { jest } from '@jest/globals';
import { Buffer } from 'node:buffer';

// --- Mocks must be declared before importing the module under test ---

const mockGetDevices = jest.fn();
const mockCreateDeviceConnection = jest.fn();
const mockWaitForDisconnect = jest.fn();

jest.unstable_mockModule('@yume-chan/adb', () => ({
  AdbServerClient: jest.fn().mockImplementation(() => ({
    getDevices: mockGetDevices,
    createDeviceConnection: mockCreateDeviceConnection,
    waitForDisconnect: mockWaitForDisconnect,
  })),
  AdbServerStream: jest.fn(),
}));

jest.unstable_mockModule('@yume-chan/adb-server-node-tcp', () => ({
  AdbServerNodeTcpConnector: jest.fn(),
}));

const { AdbTransport } = await import('./AdbTransport.js');

// ---------------------------------------------------------------------------

const flushMicrotasks = () => new Promise((resolve) => setTimeout(resolve, 0));

const createSocket = ({ chunks = [], autoClose = true, transportId = 1n } = {}) => {
  let controller;
  let resolveClosed;
  let isClosed = false;
  const closed = new Promise((resolve) => {
    resolveClosed = resolve;
  });

  const readable = new ReadableStream({
    start(ctrl) {
      controller = ctrl;
      chunks.forEach((chunk) => ctrl.enqueue(chunk));
      if (autoClose) {
        ctrl.close();
        resolveClosed();
        isClosed = true;
      }
    },
  });

  const close = jest.fn().mockImplementation(() => {
    if (!isClosed) {
      try {
        controller?.close();
      } catch {
        // Stream may already be closed from reader.cancel; ignore
      }
      resolveClosed();
      isClosed = true;
    }
    return Promise.resolve();
  });

  return {
    transportId,
    readable,
    writable: {
      getWriter: () => ({
        write: jest.fn(),
        releaseLock: jest.fn(),
        close: jest.fn(),
      }),
    },
    closed,
    close,
  };
};

beforeEach(() => {
  jest.clearAllMocks();
});

describe('AdbTransport.listDevices', () => {
  test('returns serial numbers from connected devices', async () => {
    mockGetDevices.mockResolvedValue([
      { serial: 'emulator-5554' },
      { serial: 'R58M123ABC' },
    ]);

    const transport = new AdbTransport();
    const serials = await transport.listDevices();

    expect(serials).toEqual(['emulator-5554', 'R58M123ABC']);
  });

  test('returns empty array when no devices connected', async () => {
    mockGetDevices.mockResolvedValue([]);

    const transport = new AdbTransport();
    const serials = await transport.listDevices();

    expect(serials).toEqual([]);
  });
});

describe('AdbTransport.connect', () => {
  test('emits connect, data, and disconnect for WatchdogService', async () => {
    const socket = createSocket({ chunks: [new Uint8Array([0x48, 0x69])] });
    mockCreateDeviceConnection.mockResolvedValue(socket);
    mockWaitForDisconnect.mockResolvedValue();

    const transport = new AdbTransport();
    const events = [];

    transport.on('connect', ({ serial }) => events.push(`connect:${serial}`));
    transport.on('data', (buffer) => events.push(`data:${buffer.toString('utf8')}`));
    transport.on('disconnect', ({ serial }) => events.push(`disconnect:${serial}`));

    await transport.connect('serial-123');
    await flushMicrotasks();

    expect(mockCreateDeviceConnection).toHaveBeenCalledWith(
      { serial: 'serial-123' },
      'tcp:9000',
    );
    expect(events).toEqual(['connect:serial-123', 'data:Hi', 'disconnect:serial-123']);
  });

  test('accepts hello without capabilities and stores null on session', async () => {
    const hello = `${JSON.stringify({
      type: 'hello',
      deviceId: 'android-123',
    })}\n`;
    const socket = createSocket({
      chunks: [Buffer.from(hello, 'utf8')],
      autoClose: false,
    });
    mockCreateDeviceConnection.mockResolvedValue(socket);
    mockWaitForDisconnect.mockResolvedValue();

    const transport = new AdbTransport();
    const helloEvents = [];

    transport.on('hello', (session) => helloEvents.push(session));

    await transport.connect('serial-123');
    await flushMicrotasks();

    expect(transport.session).toEqual({
      serial: 'serial-123',
      deviceId: 'android-123',
      capabilities: null,
    });
    expect(helloEvents).toEqual([
      {
        serial: 'serial-123',
        deviceId: 'android-123',
        capabilities: null,
      },
    ]);
  });

  test('disconnect closes the socket and emits disconnect once', async () => {
    const socket = createSocket({ chunks: [], autoClose: false });
    mockCreateDeviceConnection.mockResolvedValue(socket);
    mockWaitForDisconnect.mockResolvedValue();

    const transport = new AdbTransport();
    const events = [];

    transport.on('disconnect', ({ serial }) => events.push(`disconnect:${serial}`));

    await transport.connect('serial-999');
    await transport.disconnect();
    await flushMicrotasks();

    expect(socket.close).toHaveBeenCalledTimes(1);
    expect(events).toEqual(['disconnect:serial-999']);
  });

  test('propagates errors from ADB client', async () => {
    const error = new Error('adb server not running');
    mockCreateDeviceConnection.mockRejectedValue(error);

    const transport = new AdbTransport();
    const errors = [];
    transport.on('error', (err) => errors.push(err.message));

    await expect(transport.connect('serial-000')).rejects.toThrow('adb server not running');
    expect(errors).toContain('adb server not running');
  });
});
