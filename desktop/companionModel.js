export const PANEL_SECTIONS = [
  {
    id: 'companion',
    title: 'Phone Companion',
    eyebrow: 'Android sync',
    items: [
      'Mirror recent scan sessions and Gemini narratives.',
      'Launch session detail, threat results, and consent state checks.',
      'Prepare the desktop for ADB transport on demand.'
    ]
  },
  {
    id: 'analysis',
    title: 'Analyst Console',
    eyebrow: 'Desktop workflow',
    items: [
      'Pin threat narratives, RF notes, and OUI vendor context side by side.',
      'Keep Wireshark, EveBox, CrowdSec, and field notes in one operator view.',
      'Surface fast summaries before the deeper Android evidence trail.'
    ]
  },
  {
    id: 'field',
    title: 'Field Layouts',
    eyebrow: 'Orientation aware',
    items: [
      'Portrait for tablet-on-desk and phone-emulation views.',
      'Tablet for split-pane review while tethered or traveling.',
      'Wide for full operator console and evidence comparison.'
    ]
  }
];

export function resolveLayout(width, selectedMode = 'auto') {
  if (selectedMode !== 'auto') {
    return selectedMode;
  }

  if (width < 760) {
    return 'portrait';
  }

  if (width < 1180) {
    return 'tablet';
  }

  return 'wide';
}

export function summarizeDevices(devices = [], error = '') {
  if (error) {
    return {
      tone: 'error',
      headline: 'ADB unavailable',
      detail: error
    };
  }

  if (!devices.length) {
    return {
      tone: 'warn',
      headline: 'No Android devices connected',
      detail: 'Attach a phone or emulator, then refresh the companion link.'
    };
  }

  return {
    tone: 'ok',
    headline: `${devices.length} Android companion${devices.length === 1 ? '' : 's'} ready`,
    detail: devices.join(', ')
  };
}
