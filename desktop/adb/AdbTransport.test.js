import { jest } from '@jest/globals';

// --- Mocks must be declared before importing the module under test ---

const mockGetDevices = jest.fn();
const mockCreateConnection = jest.fn();
const mockDispose = jest.fn();

jest.unstable_mockModule('@yume-chan/adb', () => ({
    AdbServerClient: jest.fn().mockImplementation(() => ({
        getDevices: mockGetDevices,
        createConnection: mockCreateConnection,
    })),
    AdbServerStream: jest.fn(),
}));

jest.unstable_mockModule('@yume-chan/adb-server-node-tcp', () => ({
    AdbServerNodeTcpConnector: jest.fn(),
}));

const { AdbTransport } = await import('./AdbTransport.js');

// ---------------------------------------------------------------------------

beforeEach(() => {
    jest.clearAllMocks();
    mockCreateConnection.mockResolvedValue({ dispose: mockDispose });
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

describe('AdbTransport.forwardWatchdog', () => {
    test('sends correct host-serial forward service string', async () => {
        const transport = new AdbTransport();
        await transport.forwardWatchdog('R58M123ABC', 19000);

        expect(mockCreateConnection).toHaveBeenCalledWith(
            'host-serial:R58M123ABC:forward:tcp:19000;tcp:9000',
        );
        expect(mockDispose).toHaveBeenCalled();
    });

    test('propagates error if createConnection rejects', async () => {
        mockCreateConnection.mockRejectedValue(new Error('adb server not running'));

        const transport = new AdbTransport();
        await expect(transport.forwardWatchdog('R58M123ABC', 19000)).rejects.toThrow(
            'adb server not running',
        );
    });
});

describe('AdbTransport.removeForward', () => {
    test('sends correct host-serial killforward service string', async () => {
        const transport = new AdbTransport();
        await transport.removeForward('R58M123ABC', 19000);

        expect(mockCreateConnection).toHaveBeenCalledWith(
            'host-serial:R58M123ABC:killforward:tcp:19000',
        );
        expect(mockDispose).toHaveBeenCalled();
    });

    test('propagates error if createConnection rejects', async () => {
        mockCreateConnection.mockRejectedValue(new Error('no such forward'));

        const transport = new AdbTransport();
        await expect(transport.removeForward('R58M123ABC', 19000)).rejects.toThrow(
            'no such forward',
        );
    });
});
