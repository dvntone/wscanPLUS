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

| Dependency | Pinned Version | Latest Stable | Action |
|------------|---------------|---------------|--------|
| `androidx.core:core-ktx` | 1.13.1 | **1.18.0** | UPDATE in dep PR |
| `androidx.test.ext:junit` | 1.1.5 | **1.3.0** | UPDATE in dep PR |
| `androidx.test.espresso:espresso-core` | 3.5.1 | **3.6.1** | UPDATE in dep PR |
| `junit:junit` | 4.13.2 | 4.13.2 | PASS — already latest |

### Planned for Phase 1 (not yet added — confirmed versions)

| Dependency | Version to Pin | Notes |
|------------|---------------|-------|
| `com.google.gms:google-services` (plugin) | 4.4.2 | Required for Firebase AI. Add to root + app build.gradle.kts |
| `com.google.firebase:firebase-bom` (platform) | 35.5.0 | Firebase BOM — manages Firebase lib versions |
| `com.google.firebase:firebase-ai` | 16.0.0 | Firebase AI Logic SDK (Gemini). NOT `firebase-vertexai` (superseded) or `generativeai` (deprecated) |
| `com.google.android.gms:play-services-maps` | 18.1.0 | Google Maps SDK |
| `com.google.android.libraries.mapsplatform.secrets-gradle-plugin` | 2.0.1 | Secrets plugin for API key injection from local.properties |
| `org.jetbrains.kotlinx:kotlinx-coroutines-core` | 1.10.2 | Add only when first coroutine code lands |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | 1.10.2 | Add alongside coroutines-core |

**google-services.json** required at `android/app/google-services.json` — placeholder file needed before Firebase deps compile.

---

## Desktop npm Dependencies (Codex verified — March 2026)

### Currently on main

| Dependency | Pinned Version | Latest Stable | Action |
|------------|---------------|---------------|--------|
| `electron` | 41.0.2 | 41.0.2 | PASS |
| `eslint` | 9.0.0 | 10.0.3 | ADVISORY — no CVE, update when convenient |
| `jest` | 29.7.0 | **30.3.0** | **UPDATE REQUIRED — CVE-2024-21538** |

**Jest 30 migration notes (Codex confirmed):**
- `--experimental-vm-modules` flag no longer needed — remove from test script
- Updated script: `"test": "jest --runInBand --passWithNoTests"`
- Add `jest.config.mjs`: `export default { testEnvironment: "node", transform: {}, extensionsToTreatAsEsm: [".js", ".mjs"] }`
- CVE-2024-21538 (cross-spawn ReDoS) fixed in Jest 30.x via updated cross-spawn/execa

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
