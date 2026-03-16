# wscan+ Dependencies (Pinned)

## Dependency Pinning Policy

- **npm**: exact versions only — no `^` or `~` in `package.json`. Use `npm install --save-exact`.
- **Android Gradle**: pin all dependency versions explicitly. No dynamic versions (`+`).
- When adding a dependency, create a dedicated dependency-only PR per docs/AGENTS.md §3.

---

## Desktop Dev Environment (Linux)

### Required

| Tool | Version | Verify |
|------|---------|--------|
| Node.js | **22.x** (see `.nvmrc`) | `node --version` |
| npm | 10.x | `npm --version` |
| Git | 2.40+ | `git --version` |
| adb (android-tools-adb) | 35.0+ | `adb version` |
| JDK | 17 (Temurin recommended) | `java -version` |

### Optional (advanced features)

| Tool | Min Version | Verify | Purpose |
|------|------------|--------|---------|
| iw | any | `iw --version` | Adapter detection (`iw dev`) |
| aircrack-ng | 1.7+ | `airmon-ng --help` | Monitor mode, injection testing |
| tshark | 4.0+ | `tshark --version` | PCAP analysis pipeline |
| nmcli | any | `nmcli --version` | Network interface management |
| Kismet | 2023-07+ | `kismet --version` | Optional scan integration |
| BetterCap | 2.32+ | `bettercap --version` | Optional scan integration |
| Wireshark | 4.0+ | `wireshark --version` | PCAP analysis (GUI) |

---

## Android Dev Environment

| Tool | Version | Notes |
|------|---------|-------|
| Android Studio | current stable | |
| JDK | 17 | Same as desktop |
| Gradle | via wrapper (`./gradlew`) | Do not install globally |
| compileSdk / targetSdk | pinned in Gradle | Do not hardcode in CI |
| Android platform-tools | 35.0+ | Must match desktop adb version |

---

## Android Gradle Dependencies (confirmed pins — March 2026)

### Currently on main

| Dependency | Pinned Version | Notes |
|------------|---------------|-------|
| `androidx.core:core-ktx` | 1.18.0 | Updated PR #62 |
| `androidx.test.ext:junit` | 1.3.0 | Updated PR #62 |
| `androidx.test.espresso:espresso-core` | 3.6.1 | Updated PR #62 |
| `junit:junit` | 4.13.2 | Already latest |

### Planned for Phase 1 (not yet added — Codex verified 2026-03-16, GPT 5.2 extra high reasoning)

| Dependency | Version to Pin | Notes |
|------------|---------------|-------|
| `com.google.gms:google-services` (plugin) | **4.4.4** | Bumped from 4.4.2. Required for Firebase AI. Add to root + app build.gradle.kts. |
| `com.google.firebase:firebase-bom` (platform) | **34.10.0** | Current stable. Do NOT use 35.x (does not exist). No CVEs. |
| `com.google.firebase:firebase-ai` | unversioned via BOM (standalone: `17.10.0`) | Firebase AI Logic SDK. NOT `firebase-vertexai` (superseded) or `generativeai` (deprecated). No CVEs. |
| `com.google.android.gms:play-services-maps` | **20.0.0** | Bumped from 18.1.0. No CVEs. |
| `com.google.android.libraries.mapsplatform.secrets-gradle-plugin` | **2.0.1** | Current stable. No CVEs. |
| `org.jetbrains.kotlinx:kotlinx-coroutines-core` | **1.10.2** | Current stable. Add only when first coroutine code lands. No CVEs. |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | **1.10.2** | Current stable. Add alongside coroutines-core. No CVEs. |

**google-services.json** required at `android/app/google-services.json` — placeholder file needed before Firebase deps compile.

---

## Desktop npm Dependencies (Codex verified — March 2026)

### Currently on main (Codex verified 2026-03-16, GPT 5.2 extra high reasoning)

| Dependency | Pinned Version | Latest Stable | Action |
|------------|---------------|---------------|--------|
| `electron` | 41.0.2 | 41.0.2 | PASS — current stable, no CVEs (CVE-2025-55305 affects older versions only) |
| `eslint` | 9.0.0 | **10.0.3** | ADVISORY — ESLint 10.x is real, no CVE, update in separate dep PR |
| `jest` | 30.3.0 | 30.3.0 | PASS — CVE-2024-21538 resolved (PR #62) |

**Jest 30 config (on main — `desktop/jest.config.mjs`):**
```js
export default { testEnvironment: "node", transform: {} }
```
`extensionsToTreatAsEsm` is NOT required for `.js`/`.mjs` files. Jest 30 already treats `.mjs` and `type: "module"` `.js` as ESM automatically. Only needed for non-standard extensions (`.ts`, `.jsx`). Current config is correct.

### Planned for Phase 3 (not yet added)

| Dependency | Version to Pin | Notes |
|------------|---------------|-------|
| ADB library | TBD | `@u4/adbkit` v5.1.7 is CJS-only — incompatible with ESM-only rule. Evaluate Tango ADB for native ESM at Phase 3 before adding anything. |

---

## Verification Commands (run before starting work)

```bash
# Desktop
node --version && npm --version
git --version
adb version
java -version

# Optional tools
if command -v iw >/dev/null 2>&1; then iw --version 2>/dev/null; else echo "iw not installed"; fi
if command -v airmon-ng >/dev/null 2>&1; then airmon-ng --help 2>/dev/null | head -1; else echo "airmon-ng not installed"; fi
if command -v tshark >/dev/null 2>&1; then tshark --version 2>/dev/null | head -1; else echo "tshark not installed"; fi
if command -v kismet >/dev/null 2>&1; then kismet --version 2>/dev/null | head -1; else echo "kismet not installed"; fi
if command -v bettercap >/dev/null 2>&1; then bettercap --version 2>/dev/null | head -1; else echo "bettercap not installed"; fi

# Android
cd android && ./gradlew -v
adb devices
```
