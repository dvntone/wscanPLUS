export function parseAdbDevices(output) {
  return output
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter((line) => line && !line.startsWith('List of devices attached'))
    .map((line) => {
      const segments = line.split(/\s+/);
      const [serial = '', state = 'unknown', ...details] = segments;
      const metadata = Object.fromEntries(
        details
          .filter((segment) => segment.includes(':'))
          .map((segment) => {
            const separatorIndex = segment.indexOf(':');
            return [
              segment.slice(0, separatorIndex),
              segment.slice(separatorIndex + 1),
            ];
          }),
      );

      return {
        serial,
        state,
        model: metadata.model ?? '',
        product: metadata.product ?? '',
        device: metadata.device ?? '',
        companion: {
          status: 'unchecked',
          packageName: '',
          versionName: '',
          versionCode: '',
        },
      };
    });
}

export function validateDeviceSelector(serial) {
  return /^[A-Za-z0-9._:-]+$/.test(serial);
}

export function parseCompanionPackagePath(output, packageName) {
  const packageLine = `package:${packageName}`;
  const lines = output
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean);

  if (lines.includes(packageLine)) {
    return {
      status: 'installed',
      packageName,
      versionName: '',
      versionCode: '',
    };
  }

  return {
    status: 'missing',
    packageName,
    versionName,
    versionCode: '',
  };
}

export function parseCompanionVersionInfo(output) {
  const versionNameMatch = output.match(/versionName=([^\r\n]+)/);
  const versionCodeMatch = output.match(/versionCode=(\S+)/);

  return {
    versionName: versionNameMatch?.[1] ?? '',
    versionCode: versionCodeMatch?.[1] ?? '',
  };
}

export function describeDeviceReadiness(device) {
  const packageName = device.companion?.packageName || 'the companion package';
  if (device.state === 'unauthorized') {
    return {
      label: 'Authorization required',
      guidance:
        "Unlock the device, accept the USB debugging prompt, and only use 'Always allow from this computer' on a private trusted desktop.",
    };
  }

  if (device.state === 'offline') {
    return {
      label: 'Connection interrupted',
      guidance:
        'Reconnect the USB cable, confirm USB debugging is still enabled, and rerun the preflight.',
    };
  }

  if (device.state !== 'device') {
    return {
      label: 'ADB not ready',
      guidance:
        'Bring the device into the Android OS with USB debugging enabled before continuing.',
    };
  }

  if (device.companion?.status === 'missing') {
    return {
      label: 'Companion missing',
      guidance:
        `The device is authorized, but \`${packageName}\` is not installed yet. Install the companion app before later desktop checks.`,
    };
  }

  if (device.companion?.status === 'installed') {
    const versionSuffix = device.companion.versionName
      ? ` Version ${device.companion.versionName} is present.`
      : '';

    return {
      label: 'Companion ready',
      guidance: `The device is authorized and the Android companion is installed.${versionSuffix}`,
    };
  }

  if (device.companion?.status === 'unknown') {
    return {
      label: 'Companion check incomplete',
      guidance:
        'The device is authorized, but the desktop could not confirm companion package details. Retry the preflight before moving on.',
    };
  }

  return {
    label: 'Device ready',
    guidance:
      'The device is authorized. Continue with the next desktop onboarding step when available.',
  };
}

export const PREFLIGHT_CLASSIFICATIONS = {
  adbMissing: {
    level: 'adb-missing',
    title: 'ADB unavailable',
    guidance:
      'Install Android Platform Tools and ensure `adb` is on your PATH before continuing.',
  },
  preflightFailed: {
    level: 'preflight-failed',
    title: 'ADB preflight failed',
    guidance:
      'ADB appears to be installed, but a preflight command failed. Check that the ADB server can start, that devices are connected and authorized, and that you have permission to run `adb`.',
  },
  noDevices: {
    level: 'no-devices',
    title: 'No Android devices detected',
    guidance:
      'Connect a device over USB, enable USB debugging, and confirm the desktop is an allowed host.',
  },
  unauthorized: {
    level: 'unauthorized',
    title: 'Device authorization required',
    guidance:
      "Unlock the device, accept the USB debugging prompt, and use 'Always allow from this computer' on trusted systems.",
  },
  offline: {
    level: 'offline',
    title: 'ADB device offline',
    guidance:
      'Reconnect the USB cable, verify USB debugging stays enabled, and retry the preflight.',
  },
  ready: {
    level: 'ready',
    title: 'ADB ready',
    guidance:
      'At least one device is connected and authorized. The desktop can proceed to later companion checks.',
  },
  notReady: {
    level: 'not-ready',
    title: 'ADB devices not ready',
    guidance:
      'Connected devices are not in a normal ADB session state. Put at least one device into the OS with USB debugging enabled and retry.',
  },
};

export const PRELIGHT_CLASSIFICATIONS = PREFLIGHT_CLASSIFICATIONS;

export function classifyPreflight(result) {
  if (result.ok === false) {
    return result.classification ?? PREFLIGHT_CLASSIFICATIONS.adbMissing;
  }

  const devices = Array.isArray(result.devices) ? result.devices : [];

  if (devices.length === 0) {
    return PREFLIGHT_CLASSIFICATIONS.noDevices;
  }

  if (devices.some((device) => device.state === 'unauthorized')) {
    return PREFLIGHT_CLASSIFICATIONS.unauthorized;
  }

  if (devices.some((device) => device.state === 'offline')) {
    return PREFLIGHT_CLASSIFICATIONS.offline;
  }

  if (devices.some((device) => device.state === 'device')) {
    return PREFLIGHT_CLASSIFICATIONS.ready;
  }

  return PREFLIGHT_CLASSIFICATIONS.notReady;
}

export function summarizePreflight(versionOutput, devicesOutput) {
  const adbVersion = versionOutput.split(/\r?\n/)[0]?.trim() ?? '';
  const devices = parseAdbDevices(devicesOutput);
  const summary = {
    ok: true,
    adbVersion,
    devices,
  };

  return {
    ...summary,
    classification: classifyPreflight(summary),
  };
}
