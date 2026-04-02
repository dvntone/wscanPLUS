const OUI_DB = {
  '600194': { vendor: 'Espressif Deauther', type: 'Attack Tool', threat: 'HIGH' },
  '2462AB': { vendor: 'Espressif Deauther', type: 'Attack Tool', threat: 'HIGH' },
  '240AC4': { vendor: 'Espressif Systems', type: 'Spy Cam Chip', threat: 'HIGH' },
  '30AEA4': { vendor: 'Espressif Systems', type: 'Spy Cam Chip', threat: 'HIGH' },
  '3CEF8C': { vendor: 'Dahua Technology', type: 'CCTV Brand', threat: 'HIGH' },
  'C056E3': { vendor: 'Hikvision', type: 'CCTV Brand', threat: 'HIGH' },
  '40CBC0': { vendor: 'Apple AirTag', type: 'BLE Tracker', threat: 'HIGH' },
  '087CBE': { vendor: 'Tile Inc', type: 'BLE Tracker', threat: 'HIGH' },
  '94A3DA': { vendor: 'Samsung SmartTag', type: 'BLE Tracker', threat: 'HIGH' },
  'EC71DB': { vendor: 'Reolink', type: 'IP Camera', threat: 'MED' },
  '001DB5': { vendor: 'Axis Comm', type: 'IP Camera', threat: 'MED' },
  '00E04C': { vendor: 'Realtek', type: 'WiFi Chip', threat: 'MED' },
  '1802F3': { vendor: 'Tuya Smart', type: 'IoT Device', threat: 'MED' },
  'DCA632': { vendor: 'Raspberry Pi', type: 'Dev Board', threat: 'LOW' },
  '50C7BF': { vendor: 'TP-Link', type: 'Router', threat: 'LOW' },
  '18D6C7': { vendor: 'Apple', type: 'Router/AP', threat: 'LOW' },
  '40B4CD': { vendor: 'Google Pixel', type: 'Mobile', threat: 'LOW' },
  'F8A9D0': { vendor: 'Samsung', type: 'Mobile', threat: 'LOW' },
};

export function normalizeMac(mac = '') {
  return mac.replace(/[^A-Fa-f0-9]/g, '').toUpperCase();
}

export function getMacProfile(mac = '') {
  const normalized = normalizeMac(mac);
  if (normalized.length < 2) {
    return { normalized, prefix: normalized.slice(0, 6), isLocallyAdministered: false };
  }

  const firstOctet = Number.parseInt(normalized.slice(0, 2), 16);
  return {
    normalized,
    prefix: normalized.slice(0, 6),
    isLocallyAdministered: !Number.isNaN(firstOctet) && (firstOctet & 0x02) === 0x02,
  };
}

export function lookupOui(mac = '') {
  const prefix = normalizeMac(mac).slice(0, 6);
  if (prefix.length < 6) {
    return null;
  }

  return OUI_DB[prefix] ?? null;
}

export function getMacIntel(mac = '') {
  const macProfile = getMacProfile(mac);
  const oui = lookupOui(mac);

  if (oui) {
    return { status: 'known', oui, macProfile };
  }

  if (macProfile.normalized.length < 6) {
    return { status: 'incomplete', oui: null, macProfile };
  }

  if (macProfile.isLocallyAdministered) {
    return { status: 'private', oui: null, macProfile };
  }

  return { status: 'unknown', oui: null, macProfile };
}

export function describeIntel(result) {
  if (result.status === 'known') {
    return {
      tone: result.oui.threat.toLowerCase(),
      headline: result.oui.vendor,
      detail: `${result.oui.type} · ${result.oui.threat} confidence bucket`,
    };
  }

  if (result.status === 'private') {
    return {
      tone: 'warn',
      headline: 'Private / randomized MAC',
      detail: 'Likely client privacy rotation rather than a missing public OUI.',
    };
  }

  if (result.status === 'unknown') {
    return {
      tone: 'muted',
      headline: 'Unknown vendor',
      detail: 'No curated OUI match in the local reference set.',
    };
  }

  return {
    tone: 'muted',
    headline: 'Enter at least 6 hex characters',
    detail: 'A full OUI prefix is needed before local classification is meaningful.',
  };
}
