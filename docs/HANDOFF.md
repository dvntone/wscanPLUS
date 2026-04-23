# wscan+ Handoff Pathways

This file is the durable re-entry point for future AI agent sessions and human operators.

## Start Here

1. Read `docs/AGENTS.md`
2. Read `AGENTS.md`
3. Read `docs/INDEX.md`
4. Read this file
5. Read `docs/SESSION_STATE.md` if present
6. Check `docs/capture/INBOX.md` for newly approved findings waiting to be normalized
7. Check `docs/capture/DUMPING_AREA.md` for low-confidence, incomplete, or parking-lot material

## Current durable project truths

- Active repository: `dvntone/wscanplus`
- Deprecated repository: `dvntone/wscanplus_desktop` unless an AC release explicitly brings it back into scope
- The repo, not scattered chat history, is the authoritative long-term memory layer
- Important project findings should be captured in markdown quickly after discovery, with user approval

## What belongs in immediate capture

Use immediate markdown capture for:

- architecture decisions
- repo scope changes
- active/deprecated component status
- handoff-critical findings
- verified research that changes implementation direction
- evidence summaries from uploaded exports or logs
- blockers and next-step decisions

## Where to put captured information

- `docs/SESSION_STATE.md` for current execution state and next-step handoff
- `docs/research/` for durable research or technical reference material
- `docs/testing/` for test procedures, device-specific validation, and evidence-oriented operational notes
- `docs/capture/INBOX.md` for newly approved, fast-capture notes that still need normalization
- `docs/capture/DUMPING_AREA.md` for incomplete, uncertain, or temporarily parked information

## Workflow for future agents

When a new finding appears materially important, the agent should ask the user whether to capture it immediately in markdown.

Suggested prompt pattern:

> This looks like durable project information. Approve immediate markdown capture into the repo?

If approved:
- record the note immediately in `docs/capture/INBOX.md` or the correct durable file
- keep the entry concise, timestamped, and searchable
- later normalize or move it into the proper durable destination

If the information is useful but incomplete or not yet confirmed:
- place it in `docs/capture/DUMPING_AREA.md`
- mark it clearly as provisional, unverified, or parked

## Current handoff sources already gathered

- WiFi scanning deep-dive
- BLE engineering playbook
- exported wireless scan evidence

These should be normalized into durable repo docs as implementation and analysis proceed.
