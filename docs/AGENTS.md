# 🤖 AI Agent Collaboration Rules — wscan+

Universal guardrails for every AI agent contributing to this repository.

## Approved Coding Agents

Only these agents may write or modify code, docs, and config:

- **Claude** (Anthropic) — primary coding agent
- **Copilot / Codex** (GitHub) — secondary coding agent
- **Gemini is NOT a coding agent for this repo.** See AI Split section.

## Execution Environments & Permissions

AI agents can run in different environments with different GitHub API permissions:

### GitHub Copilot Integration (Current Environment)
- **How it works**: Claude/Copilot runs through GitHub's agent infrastructure
- **Authentication**: GitHub App with limited, scoped permissions
- **Can do**: Create PRs, commit code, comment, read repository, approve reviews (as code owner)
- **Cannot do**: Close PRs, merge PRs, modify repository settings, change branch protection
- **Why**: GitHub App permissions are restricted for security; the app doesn't have `closePullRequest`, `mergePullRequest`, or admin permissions

### Claude Code CLI (Native Anthropic Tool)
- **How it works**: Runs locally on user's machine with user's GitHub credentials
- **Authentication**: User's personal access token or OAuth (inherits user's permissions)
- **Can do**: Everything the authenticated user can do
- **Cannot do**: Nothing additional beyond user's GitHub account permissions
- **Why**: Direct user credentials = direct user permissions

### Implication for Guardrails

The rules in this file (e.g., "agent must close its own PR") were written assuming **native CLI context** where agents inherit user permissions. When running through **GitHub's Copilot integration**, these actions require human intervention:

- ❌ Agent cannot close PRs → ✅ Agent files issue, human closes PR
- ❌ Agent cannot merge PRs → ✅ Human manually merges all PRs
- ❌ Agent cannot modify settings → ✅ Only `@dvntone` modifies repository settings

**Current reality**: This repository uses GitHub Copilot integration, so all administrative actions require `@dvntone`.

## AI Split (Critical — do not change without maintainer approval)

| Role | AI | Where |
|------|----|--------|
| Coding agent | Claude / Copilot | PRs, commits, config |
| In-app threat analysis | Gemini / Vertex AI (Google AI Pro) | Android app only |
| Maps & location | MapLibre (LibreMaps) + OSM tiles | Android app only |

Coding agents must never remove, replace, or modify the Gemini/Vertex integration in the Android app. It is intentional, already paid for via Google AI Pro, and required for Play Store distribution. File an issue if you believe something needs changing — do not act unilaterally.

## Context & Why These Rules Exist

This project is built by AI agents under human oversight (review and direction only). A previous project with unrestricted parallel agent work and no clear AI boundaries produced a build-success rate of ~30%. These rules exist to prevent that pattern from repeating.

**Priority order: Reliability → Traceability → Safety → Velocity.**

## Section 0: Traceability (required for every change)

Every change must be fully traceable back to the agent that made it:

- Branch names must include the agent prefix (for example, `claude/...`, `copilot/...`, or `dvntone/...`)
- Commit messages must describe what changed and why — not just "update file" or "fix"
- Every PR description must include:
  - Which agent made it
  - What was changed and why
  - Which rule(s) guided the decisions
- No force-pushes to any branch
- No amending commits after they are merged

## Section 1: Single Work Unit at a Time

- Only one open feature or bugfix PR at any time across all agents
- Every PR must reference exactly one GitHub Issue
- Never open a new PR while CI is failing — fix the broken build first

## Section 1.1: Draft PR Workflow

- **All PRs must be opened as DRAFT** using `gh pr create --draft`.
- Draft status signals work in progress — CI must run, but reviewers must not review draft PRs.
- A PR may be marked **Ready for Review** only when ALL of the following are true:
  1. All CI checks have passed (no failures, no pending jobs)
  2. Every item in the PR description checklist is complete
  3. No unresolved `blocker` or `security` issues are open
  4. The PR references exactly one GitHub Issue
  5. No secrets are committed
- After marking Ready for Review, wait for Copilot's automated review to complete before merging. Copilot review is a quality gate — it is not enforced by CODEOWNERS but must be read and addressed before merge.
- Never mark a draft Ready for Review to force a faster merge — if CI is red, fix it first.

## 1a. Merge Strategy — Squash Only

- **Only squash merge is enabled on this repository.** Merge commits and rebase are disabled.
- This is intentional and must not be re-enabled without explicit maintainer approval.
- **Why squash is required for AI-authored work:** Squash merge produces a single
  GitHub-generated, **Verified** commit on the protected branch. This ensures the commit
  that lands on `main` is signed, even when the individual PR commits from AI agents are not.
  Enabling merge commits or rebase would allow unsigned intermediate commits to land on `main`
  directly, breaking commit verification.
- Agents must never instruct the maintainer to re-enable merge commits or rebase.

## Section 2: Keep Changes Small and Atomic

- One feature or one bugfix per PR — never combined
- Prefer < 200 lines changed (tests excluded) per PR
- No opportunistic refactors alongside feature/bug work
- Exception: a dedicated, test-only maintenance PR (see Section 7)

## Section 3: Dependency Management

- All dependency additions or version changes go in their own dedicated PR
- The PR description must include: why it's needed, its license, any known security advisories
- No dependency that introduces non-OSS or viral licenses without explicit maintainer approval

## Section 4: Test Quality and Reporting

- Every code-changing PR must include or update meaningful tests
- No placeholder assertions (`assert true`, `assertTrue(1 == 1)`, empty test bodies)
- If a change causes test coverage to drop, flag it explicitly in the PR description

## Section 5: Error Handling and Logs

- Never catch and ignore exceptions silently
- Every caught error must be logged with enough context to reproduce and debug
- If an unexpected error appears during build, test, or runtime, create a GitHub Issue before continuing

## Section 6: Design and Documentation Alignment

- After every 5 merged PRs, one agent must open a "design checkpoint" issue comparing current state to `docs/SESSION_STATE.md`
- Any change that affects public APIs, architecture, or user-facing behavior must include a documentation update in the same PR

## Section 7: Scheduled Maintenance Sessions

- Every 5th PR must be maintenance-only: cleanup, deduplication, style, doc improvements
- No new features or behavior changes in maintenance PRs
- All existing tests must continue to pass

## Section 8: Conflict and Escalation

- If two agents produce conflicting PRs, the second must close its own and file an issue
  - **Note**: AI agents cannot close PRs via GitHub API (permission limitation). Agent must file an issue and request human closure.
  - Human (@dvntone) must close the violating PR and ensure only one PR is open at a time
- After 3 consecutive failed PRs on the same area, stop and file an escalation issue tagged `agent-escalation`

## Section 9: Ethics and Security

- Never commit secrets: no API keys, tokens, passwords, or machine-specific config
  - Android: use `local.properties` + secrets-gradle-plugin
  - Desktop: use `.env` (git-ignored) + dotenv
- Never include device serials or other persistent device identifiers in tracked docs, issues, PR text, or commit messages during device testing
  - Raw identifiers may remain only in local git-ignored artifacts when operationally necessary
- Never run the Electron app as root
- Never use `exec()` with interpolated strings — use `spawn(cmd, [args])` only
- Validate every argument passed to child processes
- No offensive capabilities (deauth injection, credential cracking, etc.) without explicit maintainer approval

## Required Checks Before Marking a PR "Done"

| Platform | Commands |
|----------|----------|
| Android | `./gradlew :core:test` and assemble debug/release as applicable |
| Node/Desktop | `npm test` and `npm run lint` |
| All | Confirm `.env` and `local.properties` are not tracked by git |

## Quick Reference Checklist (copy into every PR description)

- [ ] Made by: [Claude / Copilot / dvntone]
- [ ] References exactly one GitHub Issue
- [ ] CI is green before marking Ready for Review (draft PRs open first, CI runs, then mark ready)
- [ ] One feature OR one bugfix (not both)
- [ ] < 200 LOC changed (tests excluded)
- [ ] No new dependencies mixed in (or this IS a dependency-only PR)
- [ ] Meaningful tests included/updated
- [ ] Errors are logged, not silently swallowed
- [ ] Documentation updated (or doc issue filed)
- [ ] `.env` / `local.properties` NOT committed
- [ ] No secrets of any kind committed
- [ ] Required platform checks passed (see table above)
- [ ] Merged via **Squash and merge** only (merge commits and rebase are disabled)
- [ ] Gemini/Vertex integration untouched (or issue filed if change needed)

## Repository Settings (as of 2026-03-14)

### Merge Strategy
- Squash merge only ✅
- Merge commits: disabled ❌
- Rebase merging: disabled ❌
- Reason: squash merge keeps linear history and ensures web-flow signs the squash commit, working around the agent commit signing limitation.

### Agent Signing Limitation
- Copilot (copilot-swe-agent) cannot sign individual commits.
- Claude (anthropic-code-agent) cannot sign individual commits.
- Squash merge via GitHub UI or `gh pr merge --admin --squash` from the Claude Code CLI both result in a verified, web-flow signed commit — either is an approved merge method.

### PR Policy
- PRs restricted to collaborators only.
- Auto-merge: OFF (disabled 2026-03-14 per Copilot recommendation) — every PR requires manual merge by `@dvntone`.
- Auto-delete head branches: ON — branches auto-delete after merge.

### Branch Protection (main)
- Require PR before merging ✅
- Require 1 approval ✅
- Require code owner review ✅
- Require signed commits ✅
- Require linear history ✅
- Dismiss stale reviews: OFF
- Secondary rulesets: none (deleted 2026-03-14)

### Web Commit Signoff
- Required ✅ (enabled 2026-03-14)

### GitHub Pages
- Disabled intentionally on 2026-03-14. Was set up in a prior agent session (pre-fiasco) and never completed. **Do not re-enable without a tracked issue approved by `@dvntone`.**

### GitHub Apps (approved)
- Codex ✅
- Claude ✅
- GitGuardian ✅

### GitHub Apps (disabled)
- Google Cloud Build ❌ — connected during Gemini-era agent session, not intentionally configured, disabled 2026-03-14.
- Google Cloud Developer Connect ❌ — same reason.

### Wiki & Projects
- Wiki: disabled ❌ — all documentation lives in repo markdown files (AGENTS.md, docs/).
- Projects: disabled ❌ — work tracked via Issues only.

## Amending These Rules

Rules may only be relaxed when:

- Build/test success rate is consistently ≥ 80% over at least 10 PRs
- A maintainer explicitly approves the change in a dedicated issue
- The amended rule is committed in its own PR with clear rationale
