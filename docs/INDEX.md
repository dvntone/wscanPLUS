# wscan+ Docs Index (Start Here)

## Current State
- docs/SESSION_STATE.md — architecture decisions, locked choices, current phase + next steps
- [docs/SESSION_STATE.md#2026-03-20-handoff-snapshot](docs/SESSION_STATE.md#2026-03-20-handoff-snapshot) — fastest re-entry point for the next AI session

## Device Testing
- docs/testing/README.md — testing layout, rules, and per-device organization
- docs/testing/shared/38_operational_checklist.md — repeatable adb/device validation checklist
- docs/testing/devices/motorola-g4-play-2024/README.md — archived prior-device testing context
- docs/testing/devices/revvl-tab-2/README.md — current Android 15 device prep and test entry point

## Roadmap
- docs/ROADMAP.md — phases and priorities

## Research
- docs/research/README.md — tracked research/doc standards and where local-only backlog belongs
- docs/research/verified_sources.md — curated verified external sources

## Tooling / Setup
- docs/DEPENDENCIES.md — required tools + versions + pinning policy + verification commands
- docs/SECRETS.md — required CI/CD and API secret names (no values)
- .env.example — desktop environment variables template
- secrets.defaults.properties — Android secrets defaults (no real secrets)

## Security / Threat Model
- docs/THREAT_CONTEXT.md — real-world threat context + product requirements
- docs/SECURITY.md — security headers, CORS, rate limiting, process rules

## Hardware / Lab
- docs/HARDWARE.md — lab hardware inventory, test setup, attack simulation mapping

## References
- docs/REFERENCES.md — reference repositories (gold standard, Android companion, attack patterns)

## Safety / Process
- AGENTS.md — AI agent guardrails (quick reference)
- docs/AGENTS.md — full universal guardrails for all agents
- KNOWN_ISSUES.md — incident history, repo configuration changes, known limitations; also records Phase 2 completion status as of 2026-03-20
- .github/copilot-instructions.md — Copilot-specific behavior rules
