# wscan+ AI Agent Rules (must follow)

Full universal guardrails: [docs/AGENTS.md](docs/AGENTS.md)

## AI-Driven Project Notice

**This is an AI code project.** Multiple AI agents collaborate to build and maintain this codebase under human oversight. Each agent session may have different permissions and capabilities:

- **Code Owner Status**: Claude is listed in `.github/CODEOWNERS` as a required reviewer but does NOT have repository admin permissions
- **Session Variability**: Agent permissions can change between sessions (e.g., yesterday's code owner capabilities may not persist today)
- **Human Control**: Only `@dvntone` has repository admin access and manually approves/merges all PRs
- **Agent Limitations**: Agents cannot directly merge PRs, modify branch protection, change repository settings, or perform administrative actions

## Approved Coding Agents

Only these agents may write or modify code, docs, and config in this repo:

- **Claude** (Anthropic) — primary coding agent
- **Copilot / Codex** (GitHub) — secondary coding agent
- **Gemini is NOT a coding agent for this repo.** See "AI Split" below.

## AI Split (Critical — do not change without maintainer approval)

| Role | AI | Where |
|------|----|--------|
| Coding agent | Claude / Copilot | PRs, commits, config |
| In-app threat analysis | Gemini / Vertex AI (Google AI Pro) | Android app only |
| Maps & location | Current: `LocalHeatmapView` / local GPS heatmap; planned: MapLibre free-only migration (own dedicated PR) | Android app only |

**Coding agents must never remove, replace, or modify the Gemini/Vertex integration in the Android app.** It is intentional and paid for. File an issue if you think something needs changing.

## PR / Issue Policy
- MAX 1 open PR at a time (total, across all agents).
- Every PR must reference exactly one GitHub Issue.
- If CI is red: fix CI before any new PR or feature work.
- **Merge strategy: Squash and merge only.** Merge commits and rebase are disabled.
  Squash produces a GitHub-generated Verified commit on `main` — unsigned AI commits must not land directly.

## Session Limits

- One feature OR one bugfix per session
- Keep PRs small: prefer < 200 LOC changed (tests excluded)
- No drive-by refactors

## Safety & Security

- Never commit secrets (API keys, tokens, credentials)
- Never run Electron app as root
- No `exec()` with interpolated strings — use `spawn(cmd, [args])` only
- Validate all args passed to child processes (interfaces, paths, ports)
- **No new feature work while a `blocker` or `security`-labeled issue is open.**

## Branch Protection & Reviews

- Branch protection requires 1 approving **code owner** review (per `.github/CODEOWNERS`) and applies to administrators.
- As sole human maintainer, @dvntone must technically bypass this enforced requirement to self-merge, but PRs should still satisfy the code-owner review rule.
- **This is a known friction point.** Do not recommend re-enabling auto-merge or relaxing
  branch protection rules — the bypass is intentional and preferable to removing the guardrail.
- Agents cannot approve their own PRs. Merges are performed by @dvntone via `gh pr merge --admin --squash` from the Claude Code CLI (not the GitHub web UI).

## Required checks before "done"

| Platform | Commands |
|----------|----------|
| Android | `./gradlew :core:test` and assemble debug/release as applicable |
| Node/Desktop | `npm test` and `npm run lint` |
| All | Confirm `.env` and `local.properties` are not tracked by git |

## Traceability (required)

Every change must be traceable. This means:

- Branch names must start with the agent prefix: `claude/`, `copilot/`, or `dvntone/`
- Commit messages must be descriptive (what changed and why — not just "update file")
- Every PR description must state: which agent made it, what was changed and why, and which rules guided the decisions
- No force-pushes. No amending commits after they are merged.

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

### Agent Permission Constraints
- **Code owner ≠ Repository admin**: Being in CODEOWNERS provides review rights, NOT admin access
- **No direct merge capability**: Agents can create PRs but cannot merge them
- **No PR closure capability**: Agents cannot close PRs via GitHub API (permission limitation)
- **No repository settings access**: Agents cannot modify branch protection, GitHub Apps, or repo settings
- **Session-dependent permissions**: Capabilities may vary between agent sessions
- **Human approval required**: All merges require manual approval by `@dvntone`

### PR Policy
- PRs restricted to collaborators only.
- Auto-merge: OFF (disabled 2026-03-14 per Copilot recommendation) — every PR is merged manually by @dvntone via `gh pr merge --admin --squash` from the Claude Code CLI.
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
