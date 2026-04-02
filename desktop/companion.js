import { PANEL_SECTIONS, resolveLayout, summarizeDevices } from './companionModel.js';
import { describeIntel, getMacIntel } from './ouiIntel.js';
import { PORTAL_VARIANTS, formatScore } from './portalWorkbench.js';
import { SAMPLE_SESSIONS, describeSession, narrativeTone, parseSessionArtifact } from './sessionWorkbench.js';

const state = {
  manualLayout: 'auto',
  devices: [],
  error: '',
  refreshedAt: '',
  macInput: '',
  intelHistory: [],
  sessionNotes: {},
  selectedPortal: PORTAL_VARIANTS[0].id,
  selectedSessionId: SAMPLE_SESSIONS[0].id,
  importedSessions: [],
  importedArtifactPath: '',
  importError: '',
};

const STORAGE_KEYS = {
  sessionNotes: 'wscanplus.desktop.sessionNotes',
  intelHistory: 'wscanplus.desktop.intelHistory',
  selectedPortal: 'wscanplus.desktop.selectedPortal',
  selectedSessionId: 'wscanplus.desktop.selectedSessionId',
};

let macRenderTimer;

function esc(s) {
  return String(s)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

const timelineEvents = [
  {
    time: 'Now',
    title: 'Threat results companion',
    body: 'Desktop mirrors scan narratives, investigator notes, and future export workflows without replacing the Android evidence path.',
    badges: ['Gemini narratives', 'Session context', 'Read-only first']
  },
  {
    time: 'Next',
    title: 'Companion transport',
    body: 'ADB device discovery is live now. Watchdog forwarding and desktop-side session views can build on the same bridge safely.',
    badges: ['ADB', 'Local only', 'No root']
  },
  {
    time: 'Later',
    title: 'Operator workspace',
    body: 'Wireshark, EveBox, CrowdSec, and portal/OUI tools can become sidecars rather than separate one-off windows.',
    badges: ['Flexible layout', 'Side-by-side review', 'Field workflow']
  }
];

function appRoot() {
  return document.querySelector('#app');
}

function currentSummary() {
  return summarizeDevices(state.devices, state.error);
}

function selectedLayout() {
  return resolveLayout(window.innerWidth, state.manualLayout);
}

function renderModes(layout) {
  return ['auto', 'portrait', 'tablet', 'wide']
    .map((mode) => `
      <button type="button" data-mode="${mode}" data-active="${state.manualLayout === mode}">
        ${mode === 'auto' ? `Auto (${layout})` : mode}
      </button>
    `)
    .join('');
}

function renderSection(section) {
  return `
    <article class="section-card">
      <div class="eyebrow">${section.eyebrow}</div>
      <h3>${section.title}</h3>
      <ul class="list muted">
        ${section.items.map((item) => `<li><span>${item}</span></li>`).join('')}
      </ul>
    </article>
  `;
}

function renderTimeline() {
  return timelineEvents
    .map(
      (event) => `
        <article class="event-card">
          <time>${event.time}</time>
          <h3>${event.title}</h3>
          <p class="muted">${event.body}</p>
          <div class="badge-row">
            ${event.badges.map((badge) => `<span class="badge">${badge}</span>`).join('')}
          </div>
        </article>
      `
    )
    .join('');
}

function currentIntel() {
  return describeIntel(getMacIntel(state.macInput));
}

function selectedPortalArtifact() {
  return PORTAL_VARIANTS.find((variant) => variant.id === state.selectedPortal) ?? PORTAL_VARIANTS[0];
}

function availableSessions() {
  return state.importedSessions.length ? state.importedSessions : SAMPLE_SESSIONS;
}

function selectedSession() {
  const sessions = availableSessions();
  return sessions.find((session) => session.id === state.selectedSessionId) ?? sessions[0];
}

function currentSessionNote() {
  return state.sessionNotes[selectedSession().id] ?? '';
}

function persistState() {
  try {
    window.localStorage.setItem(STORAGE_KEYS.sessionNotes, JSON.stringify(state.sessionNotes));
    window.localStorage.setItem(STORAGE_KEYS.intelHistory, JSON.stringify(state.intelHistory));
    window.localStorage.setItem(STORAGE_KEYS.selectedPortal, state.selectedPortal);
    window.localStorage.setItem(STORAGE_KEYS.selectedSessionId, String(state.selectedSessionId));
  } catch {
    // Local persistence is opportunistic in the desktop shell.
  }
}

function hydrateState() {
  try {
    state.selectedPortal = window.localStorage.getItem(STORAGE_KEYS.selectedPortal) ?? state.selectedPortal;
    state.selectedSessionId = Number.parseInt(
      window.localStorage.getItem(STORAGE_KEYS.selectedSessionId) ?? String(state.selectedSessionId),
      10
    );
    const intelHistory = JSON.parse(window.localStorage.getItem(STORAGE_KEYS.intelHistory) ?? '[]');
    const sessionNotes = JSON.parse(window.localStorage.getItem(STORAGE_KEYS.sessionNotes) ?? '{}');
    state.intelHistory = Array.isArray(intelHistory) ? intelHistory : [];
    state.sessionNotes = sessionNotes && typeof sessionNotes === 'object' ? sessionNotes : {};
  } catch {
    state.intelHistory = [];
    state.sessionNotes = {};
  }
}

function renderIntelHistory() {
  if (!state.intelHistory.length) {
    return '<p class="muted">No recent lookups yet. Use this panel to triage MACs from Wireshark, Kismet, Fing, or Android scan notes.</p>';
  }

  return state.intelHistory
    .map(
      (entry) => `
        <div class="keyline">
          <div>
            <div class="mono">${esc(entry.mac)}</div>
            <div class="muted tiny">${esc(entry.headline)}</div>
          </div>
          <span class="badge">${esc(entry.tone)}</span>
        </div>
      `
    )
    .join('');
}

function render() {
  const layout = selectedLayout();
  const summary = currentSummary();
  const intel = currentIntel();
  const portal = selectedPortalArtifact();
  const session = selectedSession();
  const sessionSummary = describeSession(session);
  const sessions = availableSessions();

  appRoot().innerHTML = `
    <div class="shell">
      <section class="hero">
        <div class="stack">
          <div class="eyebrow">wscan+ companion desktop</div>
          <h1>One operator surface for Android evidence, desktop review, and field-side layouts.</h1>
          <p class="subcopy">
            This companion shell is built to host the existing Android flow, ADB transport, and WebUI-style
            analyst tools in one responsive desktop surface. It stays local-first and orientation-aware.
          </p>
          <div class="hero-actions">
            <button class="button primary" id="refresh-devices" type="button">Refresh Android companions</button>
            <span class="chip">Current layout: <strong>${layout}</strong></span>
            <span class="chip">Last refresh: <span id="last-refresh">${state.refreshedAt || 'not yet'}</span></span>
          </div>
        </div>
        <div class="stack">
          <div class="eyebrow">View modes</div>
          <div class="mode-toggle">${renderModes(layout)}</div>
          <div class="stats">
            <div class="stat">
              <span class="muted tiny">Android companions</span>
              <strong>${state.devices.length}</strong>
              <span class="muted tiny">${summary.headline}</span>
            </div>
            <div class="stat">
              <span class="muted tiny">Primary role</span>
              <strong>Companion</strong>
              <span class="muted tiny">Desktop augments Android, not replaces it.</span>
            </div>
            <div class="stat">
              <span class="muted tiny">Flexible surfaces</span>
              <strong>3</strong>
              <span class="muted tiny">Portrait, tablet, and wide operator modes.</span>
            </div>
          </div>
        </div>
      </section>

      <main class="workspace" data-layout="${layout}">
        <section class="panel stack">
          <div class="keyline">
            <div>
              <div class="eyebrow">Android bridge</div>
              <h2>Companion status</h2>
            </div>
            <span class="chip mono">adb → tcp:9000 next</span>
          </div>
          <article class="status-card" data-tone="${summary.tone}">
            <div class="keyline">
              <strong>${esc(summary.headline)}</strong>
              <span class="badge">${summary.tone}</span>
            </div>
            <p class="muted">${esc(summary.detail)}</p>
          </article>
          <div class="surface">
            <div class="eyebrow">Active devices</div>
            <div id="device-list" class="stack">
              ${
                state.devices.length
                  ? state.devices
                      .map(
                        (serial) => `
                          <div class="keyline">
                            <span class="mono">${esc(serial)}</span>
                            <span class="badge">Ready</span>
                          </div>
                        `
                      )
                      .join('')
                  : '<p class="muted">No companion device yet. Connect a handset, emulator, or leave this shell in desktop-only review mode.</p>'
              }
            </div>
          </div>
          <div class="surface">
            <div class="eyebrow">Integration route</div>
            <ul class="list muted">
              <li><span>Reuse Android session and narrative data instead of reimplementing analysis logic.</span></li>
              <li><span>Host WebUI-style OUI, portal, and field-note tools as desktop companion panels.</span></li>
              <li><span>Keep transport local and explicit through ADB forwarding or exported session artifacts.</span></li>
            </ul>
          </div>
          <div class="surface stack">
            <div class="keyline">
              <div class="eyebrow">Data source</div>
              <button class="button primary" id="import-artifact" type="button">Import session artifact</button>
            </div>
            <p class="muted">${state.importedArtifactPath ? `Imported from ${esc(state.importedArtifactPath)}` : 'Using built-in sample sessions until an exported JSON artifact is imported.'}</p>
            ${state.importError ? `<p class="muted" style="color: var(--error);">${esc(state.importError)}</p>` : ''}
          </div>
          <div class="surface stack">
            <div class="eyebrow">Recent sessions</div>
            <div class="session-list">
              ${sessions.map((entry) => {
                const info = describeSession(entry);
                return `
                  <button class="session-card" data-session-id="${esc(entry.id)}" data-source="${state.importedSessions.length ? 'imported' : 'sample'}" data-active="${entry.id === session.id}">
                    <div class="keyline">
                      <strong>${esc(info.title)}</strong>
                      <span class="badge">${esc(entry.status)}</span>
                    </div>
                    <div class="muted tiny">${esc(info.subtitle)}</div>
                    <div class="muted tiny">${esc(info.detail)}</div>
                  </button>
                `;
              }).join('')}
            </div>
          </div>
        </section>

        <section class="panel stack">
          <div class="keyline">
            <div>
              <div class="eyebrow">Companion panels</div>
              <h2>Flexible workspace</h2>
            </div>
            <span class="chip">Field-ready</span>
          </div>
          ${PANEL_SECTIONS.map(renderSection).join('')}
        </section>

        <section class="panel stack">
          <div class="keyline">
            <div>
              <div class="eyebrow">Desktop intel</div>
              <h2>Local MAC / OUI desk</h2>
            </div>
            <span class="chip">Companion-side triage</span>
          </div>
          <div class="surface">
            <label class="field-label" for="mac-intel">MAC or BSSID</label>
            <div class="input-row">
              <input class="text-input mono" id="mac-intel" type="text" value="${esc(state.macInput)}" placeholder="24:0A:C4:11:22:33" />
              <button class="button primary" id="save-intel" type="button">Save lookup</button>
            </div>
          </div>
          <article class="intel-card" data-tone="${intel.tone}">
            <div class="keyline">
              <strong>${intel.headline}</strong>
              <span class="badge">${getMacIntel(state.macInput).macProfile.prefix || 'pending'}</span>
            </div>
            <p class="muted">${intel.detail}</p>
          </article>
          <div class="surface stack">
            <div class="eyebrow">Recent lookups</div>
            ${renderIntelHistory()}
          </div>
        </section>

        <section class="panel stack">
          <div class="keyline">
            <div>
              <div class="eyebrow">Operator review</div>
              <h2>Session narrative + portal</h2>
            </div>
            <span class="chip">${formatScore(portal.fakeScore)}</span>
          </div>
          <article class="intel-card" data-tone="${narrativeTone(session)}">
            <div class="keyline">
              <div>
                <strong>${esc(sessionSummary.title)}</strong>
                <div class="muted tiny">${esc(sessionSummary.subtitle)}</div>
              </div>
              <span class="badge">${esc(sessionSummary.detail)}</span>
            </div>
            <p class="muted">${esc(session.narrative)}</p>
            <div class="quick-actions">
              <span class="chip">Started ${esc(session.startedAt)}</span>
              <span class="chip">Ended ${esc(session.endedAt ?? 'in progress')}</span>
            </div>
          </article>
          <div class="surface stack">
            <label class="field-label" for="portal-variant">Portal artifact</label>
            <select class="text-input" id="portal-variant">
              ${PORTAL_VARIANTS.map((variant) => `<option value="${variant.id}" ${variant.id === portal.id ? 'selected' : ''}>${variant.title}</option>`).join('')}
            </select>
            <div class="intel-card" data-tone="warn">
              <div class="keyline">
                <strong>${portal.title}</strong>
                <span class="badge mono">${portal.artifactId}</span>
              </div>
              <p class="muted">${portal.summary}</p>
            </div>
            <ul class="portal-signal-list">
              ${portal.signals.map((signal) => `<li class="muted">${signal}</li>`).join('')}
            </ul>
          </div>
          <div class="surface stack">
            <label class="field-label" for="session-notes">Notes for ${esc(sessionSummary.title)}</label>
            <textarea class="text-area" id="session-notes" placeholder="Capture analyst notes, suspected rooms, portal observations, follow-up actions...">${esc(currentSessionNote())}</textarea>
            <div class="quick-actions">
              <span class="chip">Local persistence</span>
              <span class="chip">Bound to session ${session.id}</span>
              <span class="chip">Desktop companion context</span>
            </div>
          </div>
          <div class="timeline">${renderTimeline()}</div>
        </section>
      </main>

      <p class="footer-note">Desktop companion shell is local-first and intended to grow around the Android evidence path, not fork it.</p>
    </div>
  `;

  bindEvents();
}

function bindEvents() {
  document.querySelector('#refresh-devices')?.addEventListener('click', refreshDevices);
  document.querySelector('#import-artifact')?.addEventListener('click', importArtifact);
  document.querySelectorAll('[data-session-id]').forEach((button) => {
    button.addEventListener('click', () => {
      state.selectedSessionId = Number.parseInt(button.dataset.sessionId ?? String(availableSessions()[0].id), 10);
      persistState();
      render();
    });
  });
  document.querySelector('#mac-intel')?.addEventListener('input', (event) => {
    state.macInput = event.target.value;
    clearTimeout(macRenderTimer);
    macRenderTimer = setTimeout(() => render(), 150);
  });
  document.querySelector('#portal-variant')?.addEventListener('change', (event) => {
    state.selectedPortal = event.target.value;
    persistState();
    render();
  });
  document.querySelector('#session-notes')?.addEventListener('input', (event) => {
    state.sessionNotes = {
      ...state.sessionNotes,
      [selectedSession().id]: event.target.value,
    };
    persistState();
  });
  document.querySelector('#save-intel')?.addEventListener('click', () => {
    const result = getMacIntel(state.macInput);
    if (result.status === 'incomplete') {
      return;
    }

    const intel = describeIntel(result);
    state.intelHistory = [
      {
        mac: state.macInput.trim(),
        tone: intel.tone,
        headline: intel.headline,
      },
      ...state.intelHistory.filter((entry) => entry.mac !== state.macInput.trim()),
    ].slice(0, 6);
    persistState();
    render();
  });
  document.querySelectorAll('[data-mode]').forEach((button) => {
    button.addEventListener('click', () => {
      state.manualLayout = button.dataset.mode ?? 'auto';
      render();
    });
  });
}

async function refreshDevices() {
  try {
    const devices = await window.wscanDesktop.listAdbDevices();
    state.devices = devices;
    state.error = '';
  } catch (error) {
    state.devices = [];
    state.error = error instanceof Error ? error.message : String(error);
  }

  state.refreshedAt = new Date().toLocaleTimeString([], {
    hour: 'numeric',
    minute: '2-digit'
  });

  render();
}

async function importArtifact() {
  try {
    const payload = await window.wscanDesktop.importSessionArtifact();
    if (!payload) {
      return;
    }

    const sessions = parseSessionArtifact(payload.raw);
    state.importedSessions = sessions;
    state.importedArtifactPath = payload.filePath;
    state.importError = '';
    state.selectedSessionId = sessions[0].id;
    persistState();
  } catch (error) {
    state.importError = error instanceof Error ? error.message : String(error);
  }

  render();
}

window.addEventListener('resize', () => {
  if (state.manualLayout === 'auto') {
    render();
  }
});

hydrateState();
render();
refreshDevices();
