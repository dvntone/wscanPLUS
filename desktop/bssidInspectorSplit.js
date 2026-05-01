/* global document, MutationObserver */

const EMPTY = '--';

function text(id) {
  return document.getElementById(id)?.textContent?.trim() || EMPTY;
}

function createDetail(label, value) {
  const row = document.createElement('div');
  const dt = document.createElement('dt');
  const dd = document.createElement('dd');
  dt.textContent = label;
  dd.textContent = value || EMPTY;
  row.append(dt, dd);
  return row;
}

function createSection(id, title, accentClass) {
  const section = document.createElement('section');
  section.id = id;
  section.className = `inspector-split-section ${accentClass}`;

  const heading = document.createElement('h4');
  heading.textContent = title;

  const list = document.createElement('dl');
  list.className = 'detail-list inspector-split-list';

  section.append(heading, list);
  return { section, list };
}

function selectedBssid() {
  const value = text('selected-bssid');
  return value === 'No AP selected' ? '' : value;
}

function findDetectorAssessment(bssid) {
  if (!bssid) return { reason: 'Select a BSSID to inspect detector output.', confidence: EMPTY, events: '0' };

  const items = Array.from(document.querySelectorAll('.threat-item'));
  const match = items.find((item) => item.textContent?.includes(bssid));
  if (!match) {
    return {
      reason: 'No visible detector event matched this BSSID.',
      confidence: EMPTY,
      events: '0',
    };
  }

  const badge = match.querySelector('.badge')?.textContent?.trim() ?? '';
  const reason = match.querySelector('p')?.textContent?.trim() ?? match.textContent?.trim() ?? '';
  const confidence = badge.match(/(\d+%)/)?.[1] ?? EMPTY;

  return {
    reason,
    confidence,
    events: '1+',
  };
}

function renderSplitInspector() {
  const panel = document.querySelector('.selected-panel');
  if (!panel) return;

  panel.classList.add('selected-panel-inspector-split');

  let facts = document.getElementById('observed-facts-section');
  let assessment = document.getElementById('detector-assessment-section');

  if (!facts || !assessment) {
    const factSection = createSection('observed-facts-section', 'Observed Facts', 'accent-blue');
    const assessmentSection = createSection('detector-assessment-section', 'Detector Assessment', 'accent-amber');
    facts = factSection.section;
    assessment = assessmentSection.section;
    panel.append(facts, assessment);
  }

  const bssid = selectedBssid();
  const factList = facts.querySelector('dl');
  const assessmentList = assessment.querySelector('dl');
  if (!factList || !assessmentList) return;

  factList.replaceChildren(
    createDetail('SSID', text('selected-ssid')),
    createDetail('BSSID', bssid || EMPTY),
    createDetail('RSSI', text('selected-rssi')),
    createDetail('Channel', text('selected-channel')),
    createDetail('Security', text('selected-security')),
    createDetail('Last seen', text('selected-last-seen')),
  );

  const assessmentData = findDetectorAssessment(bssid);
  assessmentList.replaceChildren(
    createDetail('Severity', text('selected-risk')),
    createDetail('Confidence', assessmentData.confidence),
    createDetail('Events', assessmentData.events),
    createDetail('Reason', assessmentData.reason),
  );
}

function observeInspectorChanges() {
  const root = document.querySelector('.inspector');
  if (!root) return;

  const observer = new MutationObserver(() => {
    renderSplitInspector();
  });

  observer.observe(root, {
    childList: true,
    subtree: true,
    characterData: true,
  });
}

renderSplitInspector();
observeInspectorChanges();
