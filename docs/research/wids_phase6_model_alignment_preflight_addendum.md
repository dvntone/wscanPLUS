# Phase 6 Preflight Addendum — Web UI/Dashboard Wording Clarification

Related issue: #299

## Clarification

References in the Phase 6 preflight document to a `web UI/dashboard` surface are architectural planning references only.

They do **not** imply:

- an existing `web/` directory
- a currently implemented dashboard package
- a finalized filesystem layout
- a committed frontend runtime structure

## Canonical Source of Truth

Current repository structure and authoritative monorepo layout decisions remain governed by:

- `docs/SESSION_STATE.md`

Any future dashboard or web UI implementation paths must be introduced explicitly through tracked repository changes and associated implementation issues.

## Intent of the Original Wording

The original wording was intended to communicate ownership and governance alignment inside the monorepo boundary, not to document an already-existing runtime path.
