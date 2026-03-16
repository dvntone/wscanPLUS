# Known Issues

## 2026-03-16: CVE-2024-21538 — jest 29.7.0 (desktop, dev-only)

**Severity:** High (CVSS 7.5) — dev tooling only, not shipped in production app
**Affected:** `jest:29.7.0` → transitive dep `cross-spawn@7.0.3` (via execa)
**Vulnerability:** ReDoS (Regular Expression Denial of Service) — CPU exhaustion via crafted input to `cross-spawn`
**Fix:** `jest:30.3.0` (ships `cross-spawn@7.0.5+`)
**Status:** ✅ RESOLVED — PR #62 merged 2026-03-16 (commit e95a880)
**Resolution:** `cross-spawn` resolved to `7.0.6` in lockfile. `npm ci` clean, 0 vulnerabilities.
**Note:** `glob@7.2.3` deprecation warning remains (transitive via test-exclude→Jest). npm audit shows 0 CVEs — dev-only, not shipped. Track for future dep update.

---

## 2026-03-16: Firebase AI BOM version correction (35.5.0 → 34.10.0)

**Issue:** Initial Codex verification (2026-03-16) locked `firebase-bom:35.5.0` and `firebase-ai:16.0.0`. Both were incorrect — Firebase BOM 35.x does not exist.
**Correct versions (re-verified Codex 2026-03-16):**
- BOM: `com.google.firebase:firebase-bom:34.10.0` (released 2026-02-26)
- Artifact: `com.google.firebase:firebase-ai` (no explicit version when using BOM; standalone = `17.10.0`)
- `firebase-ai:16.x` is superseded; stable line is 17.x
**Breaking changes 16.x → 17.x:** minSdk bumped to 23 (project minSdk 24 — compatible). `generateContent()`/`countTokens()` require ≥1 argument. Grounding metadata fields non-optional. No migration burden since no Firebase AI code exists yet.
**Status:** Corrected in SESSION_STATE. No code impact until Firebase scaffold PR.

---

## 2026-03-16: AGP 9.1.0 now stable — pending upgrade

**Issue:** AGP 9.0.0 was locked with note "9.1.0 is alpha-only". AGP 9.1.0 was promoted to stable 2026-03-03.
**Action:** Bump AGP to 9.1.0 in next dep PR (after PR #62 merges).
**Migration from 9.0.0:** Gradle wrapper already at 9.3.1 (meets 9.1.0 minimum — no wrapper change needed). SDK Build Tools 36.0.0 (already set). R8 repackaging enabled by default in 9.1.0 — add `-dontrepackage` to ProGuard rules only if it causes issues (unlikely at scaffold stage).
**Built-in Kotlin:** Still applies in AGP 9.1.0 — no `org.jetbrains.kotlin.android` plugin needed.
**Status:** ✅ RESOLVED — PR #62 merged 2026-03-16. AGP bumped to 9.1.0 in this PR.

---

## 2026-03-16: Scanner chain — Nexmon and Shizuku removed

**Decision date:** 2026-03-16
**Removed from scanner chain:** Nexmon, Shizuku
**New effective chain:** USB > Standard (Root = developer opt-in stub, never silent fallback)

**Nexmon removal rationale:**
- Broadcom chipsets only (~20% of Android market)
- Primary test devices (OnePlus 10T = Qualcomm Snapdragon, Pixel 10 Pro XL = Google Tensor) are incompatible
- Requires per-device bootloader unlock + kernel flashing — not distributable as an app feature
- Only viable for dedicated research hardware (Kali NetHunter, etc.)

**Shizuku removal rationale:**
- Previously dropped from scope (REFERENCES.md)
- Research confirmed: adds zero WiFi scanning capability over standard WifiManager
- Scan throttling (4/2 min foreground) still applies; no monitor mode; no raw frames
- Sessions don't survive reboot without root; Android 16 beta already broke it

---

## 2026-03-16: adbkit — all variants CJS-only, deferred to Phase 3

**Issue:** `openstf/adbkit` is unmaintained (~7 years). `@u4/adbkit` v5.1.7 (maintained fork) is confirmed CJS-only — no `"exports"` field, no `"type": "module"`. Incompatible with wscanplus ESM-only rule without a `createRequire` shim.
**Decision:** `createRequire` workaround rejected — violates ESM-only principle. ADB desktop library deferred to Phase 3.
**Phase 3 action:** Evaluate Tango ADB for native ESM compatibility before adding any ADB library.
**Impact:** No ADB library added to desktop in Phase 1 or 2. Android-side `ServerSocket(9000)` architecture unaffected.

---

## 2026-03-16: Firebase AI SDK name — use firebase-ai, not firebase-vertexai

**Issue:** `com.google.firebase:firebase-vertexai` is superseded. `com.google.ai.client.generativeai` is deprecated.
**Resolution (Codex verified Mar 2026):** Use **`com.google.firebase:firebase-ai:16.0.0`** (Firebase AI Logic SDK) via BOM `firebase-bom:35.5.0`. Requires `com.google.gms:google-services:4.4.2` plugin and `google-services.json` at `android/app/google-services.json`.
**WIF note (gemini_findings.md):** WIF is for CI/CD → GCP server-side auth only. Not for Android app runtime. Filed for Phase 4+.

---

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

## 2026-03-15: AGP 9.0.0 Built-in Kotlin (PR #57, #59)

### Summary

AGP 9.0.0 ships with **built-in Kotlin**. The `org.jetbrains.kotlin.android` plugin is intentionally removed — AGP applies it internally. This caused 3 CI failures during scaffold setup before the official migration guide was consulted.

### Official behaviour (confirmed: [developer.android.com/build/migrate-to-built-in-kotlin](https://developer.android.com/build/migrate-to-built-in-kotlin))

- `org.jetbrains.kotlin.android` must NOT be applied — AGP registers the `kotlin` extension; applying it again throws `Cannot add extension with name 'kotlin'`
- `kotlinOptions { }` is removed — replaced by `kotlin { compilerOptions { } }` top-level block
- `jvmTarget` defaults automatically to `compileOptions.targetCompatibility` — explicit set is optional but recommended for clarity
- `kotlin-stdlib` is managed by AGP built-in Kotlin — no explicit declaration needed
- For modules with no Kotlin sources: optionally add `android { enableKotlin = false }` to improve build performance

### Correct module pattern

```kotlin
// No Kotlin plugin declaration needed

android {
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // jvmTarget for Kotlin defaults to targetCompatibility — no separate kotlin block needed
    }

    // If the module has no Kotlin sources, disable built-in Kotlin for build performance:
    // enableKotlin = false
}
```

### Confirmed Stable Versions (as of 2026-03-15)

| Tool | Version | Notes |
|------|---------|-------|
| AGP | 9.1.0 | Stable since 2026-03-03. Gradle 9.3.1 already meets minimum. |
| Gradle | 9.3.1 | Wrapper SHA-256 pinned |
| compileSdk / targetSdk | 36 | — |
| minSdk | 24 | — |
| JDK | 17 | — |

### Lesson

Always consult the official AGP migration guide when hitting plugin extension conflicts. The fix was in the docs — 3 CI cycles could have been avoided.

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
