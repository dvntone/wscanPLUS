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

  const narrativeBySession = new Map();
  for (const item of narratives) {
    if (typeof item?.sessionId === 'number' && !narrativeBySession.has(item.sessionId)) {
      narrativeBySession.set(item.sessionId, {
        narrative: item.narrative,
        modelName: item.modelName,
        signalCount: item.signalCount,
      });
    }
  }

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

export function summarizeSessions(sessions = []) {
  return sessions.reduce(
    (summary, session) => {
      summary.total += 1;
      summary.signals += session.signalCount ?? 0;
      if (session.status === 'suspect') {
        summary.suspect += 1;
      } else if (session.status === 'review') {
        summary.review += 1;
      } else {
        summary.clear += 1;
      }
      return summary;
    },
    { total: 0, suspect: 0, review: 0, clear: 0, signals: 0 }
  );
}

export function matchesSessionQuery(session, query = '', status = 'all') {
  if (status !== 'all' && session.status !== status) {
    return false;
  }

  const trimmed = query.trim().toLowerCase();
  if (!trimmed) {
    return true;
  }

  const haystack = [
    session.id,
    session.environmentType,
    session.deviceSerial,
    session.modelName,
    session.narrative,
    session.startedAt,
    session.endedAt ?? '',
  ]
    .join(' ')
    .toLowerCase();

  return haystack.includes(trimmed);
}
