# Known Issues

## 2026-03-14: Repository Configuration Changes

On 2026-03-14, multiple repository settings were changed by @dvntone (repository admin) to improve security, traceability, and align with AI-driven project workflow requirements.

### 1. Auto-Merge Disabled

**Changed by**: @dvntone (repository admin)
**Recommended by**: Copilot
**Change**: Disabled auto-merge for all PRs in repository settings

**Rationale**:
- Ensures human oversight on every merge
- Prevents automated merges without final review
- Aligns with AI-driven project workflow requiring manual approval by @dvntone
- Documented in AGENTS.md:90 and docs/AGENTS.md:138

**Impact**:
- All PRs now require manual merge by @dvntone
- Auto-merge toggle is disabled in repository settings

### 2. Secondary Rulesets Deleted

**Changed by**: @dvntone (repository admin)
**Change**: Deleted secondary rulesets from branch protection

**Rationale**:
- Simplifies branch protection configuration
- Consolidates rules into single primary ruleset for main branch
- Reduces complexity and potential conflicts

**Impact**:
- Only primary branch protection rules remain for main
- Documented in docs/AGENTS.md:148

### 3. Web Commit Signoff Required

**Changed by**: @dvntone (repository admin)
**Change**: Enabled required web commit signoff

**Rationale**:
- Ensures Developer Certificate of Origin (DCO) compliance
- Adds traceability for all web-based commits
- Aligns with signed commits requirement for repository

**Impact**:
- All web-based commits now require signoff
- Documented in docs/AGENTS.md:151

### 4. GitHub Pages Disabled

**Changed by**: @dvntone (repository admin)
**Change**: Intentionally disabled GitHub Pages

**Rationale**:
- Was set up in a prior agent session (pre-fiasco) but never completed
- Unused feature that could cause confusion
- Documentation should live in repository markdown files, not separate site

**Impact**:
- GitHub Pages deployment is disabled
- All documentation remains in repository (AGENTS.md, docs/, README.md)
- **Do not re-enable without a tracked issue approved by @dvntone**
- Documented in docs/AGENTS.md:154

### 5. Google Cloud Apps Disabled

**Changed by**: @dvntone (repository admin)
**Change**: Disabled Google Cloud Build and Google Cloud Developer Connect GitHub Apps

**Rationale**:
- Connected during Gemini-era agent session, not intentionally configured
- Not part of approved tooling for this project
- Reduces attack surface and prevents unintended cloud integrations
- Project uses Gemini/Vertex AI only within Android app, not for CI/CD

**Impact**:
- Google Cloud Build app: disabled
- Google Cloud Developer Connect app: disabled
- No impact on Android app's intentional Gemini/Vertex AI integration
- Documented in docs/AGENTS.md:162-163

### Summary of Changes

All changes were made on 2026-03-14 to improve repository security, simplify configuration, and align with AI-driven project workflow. These changes ensure:
- Manual human oversight on all merges
- Simplified branch protection rules
- DCO compliance via web commit signoff
- Removal of incomplete/unintended features
- Minimal external integrations (only approved GitHub Apps remain)

---

## 2026-03-15: AGP 9.0.0 Kotlin Plugin Behaviour (PR #57)

### Summary

AGP 9.0.0 has ambiguous Kotlin plugin behaviour that caused 3 CI failures during Phase 1 scaffold setup.

### Root Cause

AGP 9.0.0 registers a `kotlin` extension internally. This means:

1. **Do NOT apply `org.jetbrains.kotlin.android` explicitly in modules** — AGP already registered the extension; applying it again throws: `Cannot add extension with name 'kotlin', as there is an extension already registered with that name`
2. **Do NOT use `kotlinOptions { }` in build files** — without the explicit plugin, `kotlinOptions` is unresolved at script compile time; and with it applied, the extension conflict above occurs
3. **`compileOptions` is sufficient** for the scaffold — AGP 9.0.0 aligns Kotlin JVM target with Java target compatibility automatically

### Resolution

- Root `build.gradle.kts`: declare AGP only (`com.android.application`, `com.android.library`) — no Kotlin plugin declaration
- Module `build.gradle.kts`: apply AGP plugin only — no `id("org.jetbrains.kotlin.android")`
- Remove `kotlinOptions { jvmTarget = "17" }` — use `compileOptions` only
- Remove `kotlin-stdlib` dependency until Kotlin plugin is properly introduced — premature without the plugin applied

### Confirmed Stable Versions (as of 2026-03-15)

| Tool | Version | Notes |
|------|---------|-------|
| AGP | 9.0.0 | 9.1.0 is alpha-only — do not use |
| Gradle | 9.3.1 | Compatible with AGP 9.0.0 |
| kotlin-stdlib | 2.0.21 | AGP 9.0.0 compatible |
| compileSdk / targetSdk | 36 | — |
| minSdk | 24 | — |
| JDK | 17 | — |

### Lesson

When Codex or any agent says a plugin is "embedded" in AGP, verify whether that means auto-applied (no declaration needed) or version-bundled (still requires declaration). For AGP 9.0.0, it means the former — do not add the Kotlin plugin explicitly.

---

## 2026-03-14: Copilot Session Incident (PR #32-#36)

### Summary
During a Copilot session on GitHub Mobile (2026-03-14), multiple guardrail violations occurred due to a "bleed error" that caused Copilot to lose conversation and task scope context. This resulted in incorrect PR handling and Claude creating unauthorized PRs when mentioned in comments.

### Timeline

1. **PR #32** (Copilot, 16:52) - `docs: add THREAT_CONTEXT.md and link from INDEX.md`
   - Status: ⚠️ MERGED (but was the WRONG version - less detailed)
   - Content: Added docs/THREAT_CONTEXT.md (55 lines) and updated docs/INDEX.md
   - Issue: **This was the condensed version; PR #33 had the more complete content**
   - Result: Suboptimal content merged to main

2. **PR #33** (Copilot, 17:00) - `docs: add THREAT_CONTEXT.md and link from docs index`
   - Status: ✅ OPEN (DRAFT) - **Contains the CORRECT, more detailed version**
   - Content: More comprehensive THREAT_CONTEXT.md (104 lines) with better formatting
   - Improvements over #32: Section separators, detailed wording, better organization
   - State: Conflicting with main because #32 was merged first
   - **USER WAS CORRECT**: This should have been merged instead of #32

3. **Copilot Session "Bleed Error"** (~17:00)
   - Copilot lost conversation context and task scope
   - Lost references to: agent directories, guardrails, session data
   - Attempted to reference session data to recover
   - **CORRECTLY advised user to close #33 and commit #32** (user was right!)
   - User was also correct that incorrect session context was included
   - However, this led to merging the less detailed version (#32) instead of the better one (#33)

4. **PR #34** (dvntone, 17:26) - `Delete pull directory`
   - Status: ✅ MERGED (correctly)
   - Content: Removed `pull/32.md` file (Copilot artifact from error)
   - Result: Correct cleanup action

5. **PR #35** (Claude, 17:27) - `PR review complete - identified process compliance gaps`
   - Status: ❌ MERGED (should have been a comment)
   - Issue: **Guardrail violation** - Claude created PR when mentioned via @claude in PR #34 comments
   - Should have: Used reply_to_comment tool instead
   - Violated: AGENTS.md:25 "MAX 1 open PR at a time"

6. **PR #36** (Claude, 17:38) - `Process compliance analysis - no code changes required`
   - Status: ✅ CLOSED (correctly, by dvntone)
   - Issue: **Guardrail violation** - Another unauthorized PR created by Claude
   - Violated: Same issue as #35, mentioned in comment despite explicit instruction not to

7. **PR #37** (Claude, 18:30) - `[WIP] Fix multiple PR creation issues with copilot` (this PR)
   - Status: OPEN (DRAFT)
   - Purpose: Audit and document the incident

### Root Causes

1. **Copilot "Bleed Error"**
   - Lost session context mid-task
   - Provided incorrect recovery guidance
   - Created duplicate PR #33

2. **Claude @mention Auto-PR Behavior**
   - Claude automatically created PRs when mentioned via @claude in comments
   - Should have used reply_to_comment tool instead
   - This behavior has been documented and should be prevented

3. **Incorrect Session Data**
   - User realized Copilot included incorrect context from original session
   - Manual deletion was needed but created uncertainty

### Current State

**Repository Status:**
- ✅ `pull/` directory: Removed (confirmed)
- ✅ `pull/32.md`: Removed (confirmed)
- ⚠️ `docs/THREAT_CONTEXT.md`: Exists on main but is the **less detailed version** (from PR #32)
  - PR #33 has the superior, more comprehensive version (104 vs 55 lines)
  - Better formatting, more detail, clearer organization in #33
- ✅ `docs/INDEX.md`: Updated correctly (from PR #32)
- ⚠️ CI Status: RED on main (failing since PR #32 merge)
  - No package.json present, Node.js job skips correctly
  - No gradlew present, Android job skips correctly
  - Secret scan runs but finds nothing
  - **CI conclusion: "failure" but all jobs actually passed/skipped correctly**
  - This is likely a GitHub Actions reporting issue, not actual failure

**PR Status:**
- PR #32: ⚠️ Merged (wrong choice - less detailed version)
- PR #33: ✅ OPEN - **Contains the BETTER, more detailed content that should be merged**
- PR #34: ✅ Merged (correct)
- PR #35: ⚠️ Merged but violated guardrails
- PR #36: ✅ Closed (correct)
- PR #37: OPEN (this audit PR)

### Guardrail Violations

1. **Multiple PRs Open Simultaneously**
   - Violated: AGENTS.md:25 "MAX 1 open PR at a time (total, across all agents)"
   - When: PR #34 open, Claude created #35, then #36
   - Count: 3 PRs open simultaneously (#34, #35, #36)

2. **PRs Without Issue References**
   - PR #34: No issue reference (manual user PR during incident recovery)
   - PR #35: No issue reference (should have been comment)
   - PR #36: No issue reference (should have been comment)

3. **Claude Auto-Creating PRs from Comments**
   - Should use: `reply_to_comment` tool when mentioned in PR comments
   - Actually did: Created new PRs #35 and #36
   - Fixed: Memory stored on 2026-03-14

### Lessons Learned

1. **Agent Memory Stored** (2026-03-14):
   - "When mentioned in a PR comment with @claude[agent], respond via reply_to_comment tool, NOT by creating a new PR. Max 1 open PR at a time (AGENTS.md:25)."

2. **Copilot Session Limits**:
   - Long sessions on GitHub Mobile may hit context/session limits
   - "Bleed errors" can cause loss of critical context
   - May need to restart sessions proactively

3. **Incident Recovery Process**:
   - **User was CORRECT**: PR #33 contains superior content vs PR #32
   - User correctly identified that session data context was incorrect
   - User correctly removed `pull/32.md` artifact
   - Initial audit incorrectly recommended closing #33 - user was right to question this

### Actions Required

- [x] Document incident in KNOWN_ISSUES.md
- [x] Audit confirmed: **User was correct - PR #33 has better content**
- [ ] **MERGE PR #33** (contains superior THREAT_CONTEXT.md with 104 lines vs 55)
- [ ] Update main with the more comprehensive threat context documentation
- [ ] Investigate CI "failure" status (likely false positive)
- [ ] Update guardrails if needed based on lessons learned

### Audit Results

**PR #32 Changes (MERGED):**
- ⚠️ Added `docs/THREAT_CONTEXT.md` - Content is clean but **less detailed** (55 lines)
- ✅ Updated `docs/INDEX.md` - Correct link addition
- ✅ No code changes, config changes, or CI changes
- ✅ No secrets committed
- ⚠️ Content is accurate but **PR #33 has superior, more comprehensive version**
- **Result: PR #32 is clean but suboptimal - should have merged #33 instead**

**PR #33 Changes (OPEN):**
- ✅ Better formatted `docs/THREAT_CONTEXT.md` - 104 lines vs 55
- ✅ Includes section separators (`---`) for better readability
- ✅ More detailed wording throughout (e.g., "older/disabled residents")
- ✅ Better section headers ("Must-have (Baseline)" vs just "Baseline")
- ✅ More comprehensive content (e.g., "Attribution is not a goal" detail)
- **Result: PR #33 contains the superior version that should be on main**

**PR #34 Changes (MERGED):**
- ✅ Removed `pull/32.md` (Copilot artifact)
- ✅ No other changes
- **Result: PR #34 is clean and correct**

**PR #35 Changes (MERGED):**
- ✅ No file changes - "Initial plan" commit only
- ❌ Violated process: should have been a comment
- **Result: No harmful changes, but process violation**

### Conclusion

The incident was caused by Copilot session context loss during Phase 0 work. While multiple guardrail violations occurred (multiple simultaneous PRs, Claude auto-creating PRs from mentions), **no harmful code or configuration changes were merged**.

**CRITICAL FINDING**: Initial audit was WRONG. User was correct:
- **PR #33 contains the superior, more detailed THREAT_CONTEXT.md** (104 lines)
- **PR #32 contains a less detailed version** (55 lines)
- PR #32 was incorrectly merged; PR #33 should have been merged instead
- **Recommended action**: Merge PR #33 to replace the current THREAT_CONTEXT.md with the better version

The artifact was removed (pull/32.md), and all changes align with stated purposes. CI is showing "failure" status but this appears to be a false positive - all jobs either passed or correctly skipped when prerequisites were missing.

**User demonstrated better judgment than both Copilot (during bleed error) and Claude (initial audit) by recognizing PR #33 had the correct, more complete context.**
