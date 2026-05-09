/* global document, MutationObserver, queueMicrotask */

export const inspectorState = {
  rendering: false,
  suppressObserver: false,
  pendingRender: false,
};

function text(id) {
  return document.getElementById(id)?.textContent?.trim() ?? '--';
}

function riskClass(risk) {
  const normalized = risk.toLowerCase();
  if (normalized === 'high') return 'badge badge-threat';
  if (normalized === 'watch') return 'badge badge-warning';
  if (normalized === 'low') return 'badge badge-success';
  return 'badge badge-neutral';
}

function createFact(label, value) {
  const row = document.createElement('div');
  row.className = 'inspector-fact-row';

  const dt = document.createElement('dt');
  dt.textContent = label;

  const dd = document.createElement('dd');
  dd.textContent = value || '--';

  row.append(dt, dd);
  return row;
}

function findSelectedThreat(bssid) {
  const items = Array.from(document.querySelectorAll('#threat-list .threat-item'));
  if (!bssid || bssid === 'No AP selected') return null;
  return items.find((item) => item.textContent?.includes(bssid)) ?? null;
}

function parseThreatText(item) {
  const badge = item?.querySelector('.badge')?.textContent?.trim() ?? '';
  const body = item?.querySelector('p')?.textContent?.trim() ?? '';
  const confidence = badge.match(/(\d+)%/)?.[1];
  const reason = body.split('•')[0]?.trim() || 'No matched detector reason for the selected BSSID.';
  return {
    confidence: confidence ? `${confidence}%` : '--',
    reason,
  };
}

function mountInspectorSections() {
  if (document.getElementById('bssid-inspector-split')) return;

  const panel = document.querySelector('.selected-panel');
  if (!panel) return;

  const existingDetails = panel.querySelector('.detail-list');
  if (existingDetails) existingDetails.hidden = true;

  const wrapper = document.createElement('section');
  wrapper.id = 'bssid-inspector-split';
  wrapper.className = 'bssid-inspector-split';
  wrapper.setAttribute('aria-label', 'BSSID observed facts and detector assessment');

  const facts = document.createElement('article');
  facts.className = 'inspector-section inspector-section-facts';
  facts.innerHTML = `
    <div class="inspector-section-header">
      <h4>Observed Facts</h4>
      <span class="section-accent section-accent-blue">FACTS</span>
    </div>
    <dl id="observed-facts-list" class="inspector-section-list"></dl>
  `;

  const assessment = document.createElement('article');
  assessment.className = 'inspector-section inspector-section-assessment';
  assessment.innerHTML = `
    <div class="inspector-section-header">
      <h4>Detector Assessment</h4>
      <span class="section-accent section-accent-amber">INFERENCE</span>
    </div>
    <dl id="detector-assessment-list" class="inspector-section-list"></dl>
  `;

  wrapper.append(facts, assessment);
  panel.appendChild(wrapper);
}

function renderObservedFacts() {
  const list = document.getElementById('observed-facts-list');
  if (!list) return;

  list.replaceChildren(
    createFact('SSID', text('selected-ssid')),
    createFact('BSSID', text('selected-bssid')),
    createFact('RSSI', text('selected-rssi')),
    createFact('Channel', text('selected-channel')),
    createFact('Security', text('selected-security')),
    createFact('Last seen', text('selected-last-seen')),
  );
}

function renderAssessment() {
  const list = document.getElementById('detector-assessment-list');
  if (!list) return;

  const bssid = text('selected-bssid');
  const risk = text('selected-risk');
  const threat = findSelectedThreat(bssid);
  const parsed = parseThreatText(threat);

  const severityRow = createFact('Severity', risk);
  const severityValue = severityRow.querySelector('dd');
  if (severityValue) {
    severityValue.className = riskClass(risk);
    severityValue.textContent = risk;
  }

  list.replaceChildren(
    severityRow,
    createFact('Confidence', parsed.confidence),
    createFact('Reason', parsed.reason),
    createFact('Basis', threat ? 'Matched detector queue item for selected BSSID.' : 'No matched detector queue item visible.'),
  );
}

export function releaseObserverSuppression() {
  queueMicrotask(() => {
    inspectorState.suppressObserver = false;
    if (inspectorState.pendingRender) {
      inspectorState.pendingRender = false;
      renderInspectorSplit();
    }
  });
}

function renderInspectorSplit() {
  if (inspectorState.rendering) return;

  inspectorState.rendering = true;
  inspectorState.suppressObserver = true;

  try {
    mountInspectorSections();
    renderObservedFacts();
    renderAssessment();
  } finally {
    inspectorState.rendering = false;
    releaseObserverSuppression();
  }
}

function observeInspector() {
  const inspector = document.querySelector('.inspector');
  if (!inspector) return;

  const observer = new MutationObserver(() => {
    // Mid-render mutations are self-caused; renderInspectorSplit() will re-run via pendingRender if needed.
    if (inspectorState.rendering) return;
    // Between render completion and microtask release: record for replay instead of dropping.
    if (inspectorState.suppressObserver) {
      inspectorState.pendingRender = true;
      return;
    }
    renderInspectorSplit();
  });

  observer.observe(inspector, {
    childList: true,
    subtree: true,
    characterData: true,
  });
}

renderInspectorSplit();
observeInspector();
