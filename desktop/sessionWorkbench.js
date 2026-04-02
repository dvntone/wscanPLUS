export const SAMPLE_SESSIONS = [
  {
    id: 104,
    startedAt: '2026-04-01 09:18',
    endedAt: '2026-04-01 09:26',
    environmentType: 'MULTI_UNIT_HOUSING',
    deviceSerial: 'android-lab-01',
    signalCount: 4,
    modelName: 'gemini-2.5-flash',
    narrative:
      'Likely surveillance pressure near the entry corridor. Repeated ESPressif-class identifiers and one tracker-like BLE pattern justify a room-by-room physical follow-up.',
    status: 'suspect',
  },
  {
    id: 103,
    startedAt: '2026-04-01 08:41',
    endedAt: '2026-04-01 08:47',
    environmentType: 'COFFEE_SHOP',
    deviceSerial: 'android-lab-01',
    signalCount: 0,
    modelName: 'gemini-2.5-flash',
    narrative:
      'No persistent threat pattern stood out. The strongest signals resembled ordinary client rotation and shared hotspot churn rather than targeted surveillance behavior.',
    status: 'clear',
  },
  {
    id: 102,
    startedAt: '2026-03-31 22:06',
    endedAt: '2026-03-31 22:15',
    environmentType: 'HOTEL',
    deviceSerial: 'android-lab-02',
    signalCount: 2,
    modelName: 'gemini-2.5-flash',
    narrative:
      'Portal and RF conditions were mixed. The session should be paired with portal artifact review before escalation because the confidence is moderate rather than decisive.',
    status: 'review',
  },
];

function deriveStatus(signalCount) {
  if (signalCount >= 3) {
    return 'suspect';
  }

  if (signalCount > 0) {
    return 'review';
  }

  return 'clear';
}

function normalizeImportedSession(session, narrativeBySession) {
  const narrative = narrativeBySession.get(session.id) ?? {};
  return {
    id: session.id,
    startedAt: formatTimestamp(session.startedAt),
    endedAt: formatTimestamp(session.endedAt),
    environmentType: session.environmentType,
    deviceSerial: session.deviceSerial,
    signalCount: narrative.signalCount ?? session.signalCount ?? 0,
    modelName: narrative.modelName ?? session.modelName ?? 'gemini-2.5-flash',
    narrative: narrative.narrative ?? session.narrative ?? 'No Gemini narrative stored for this session yet.',
    status: session.status ?? deriveStatus(narrative.signalCount ?? session.signalCount ?? 0),
  };
}

export function formatTimestamp(value) {
  if (value === null || value === undefined || value === '') {
    return null;
  }

  if (typeof value === 'number') {
    return new Date(value).toISOString().slice(0, 16).replace('T', ' ');
  }

  return String(value);
}

export function parseSessionArtifact(raw) {
  const parsed = JSON.parse(raw);
  const sessions = Array.isArray(parsed) ? parsed : parsed.sessions;
  const narratives = Array.isArray(parsed?.narratives) ? parsed.narratives : [];

  if (!Array.isArray(sessions)) {
    throw new Error('Artifact must contain a sessions array.');
  }

  const narrativeBySession = new Map(
    narratives
      .filter((item) => typeof item?.sessionId === 'number')
      .map((item) => [
        item.sessionId,
        {
          narrative: item.narrative,
          modelName: item.modelName,
          signalCount: item.signalCount,
        },
      ])
  );

  const normalized = sessions
    .filter(
      (session) =>
        typeof session?.id === 'number' &&
        typeof session?.startedAt !== 'undefined' &&
        typeof session?.environmentType === 'string' &&
        typeof session?.deviceSerial === 'string'
    )
    .map((session) => normalizeImportedSession(session, narrativeBySession))
    .sort((a, b) => String(b.startedAt).localeCompare(String(a.startedAt)));

  if (!normalized.length) {
    throw new Error('Artifact did not contain any valid scan sessions.');
  }

  return normalized;
}

export function describeSession(session) {
  return {
    title: `Session ${session.id}`,
    subtitle: `${session.environmentType} · ${session.deviceSerial}`,
    detail: `${session.signalCount} signal${session.signalCount === 1 ? '' : 's'} · ${session.modelName}`,
  };
}

export function narrativeTone(session) {
  if (session.status === 'suspect') {
    return 'high';
  }

  if (session.status === 'review') {
    return 'warn';
  }

  return 'ok';
}
