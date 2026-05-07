# wscan+ Theme Token Strategy

Issue: #301

## Current state

The active monorepo already contains the desktop Electron tactical shell under `desktop/`. The immediate need is not to re-port the archived desktop UI from scratch; it is to formalize the shared visual language so desktop, Android, and future web surfaces stay consistent.

The first token source lives at:

```text
design/tokens/wscan.tokens.json
```

## Strategy

Use one semantic token source and map it into platform-specific outputs over time.

### Desktop Electron

Map token values into CSS variables in `desktop/styles.css`.

Preferred variable shape:

```css
--ws-color-canvas
--ws-color-panel
--ws-color-panel-elevated
--ws-color-accent-scan
--ws-color-threat-critical
--ws-color-rssi-strong
```

The current short aliases such as `--bg`, `--surface`, and `--primary` can remain temporarily as compatibility aliases while the desktop UI is migrated.

### Android View UI

Map token values into `WscanUi` color constants or a future theme object.

Current Android UI is View-based. Avoid broad Compose or AppCompat rewrites in token-only PRs. Theme preference work should be validated separately from visual-token adoption.

### Future Material 3 path

RikkaApps/MaterialThemeBuilder informed the theme-generation direction: use generated color resources where useful, but do not vendor generator code unless the upstream license notice is preserved.

The initial wscan+ need is semantic mapping, not dynamic-color complexity.

## Current vs planned map wording

Current Android scan map behavior is local/offline heatmap rendering through `LocalHeatmapView` and GPS-tagged scan data.

MapLibre is a planned free-only future migration path and must stay in its own dedicated map-provider issue/PR. Token PRs must not add map-provider dependencies.

## PR sequencing

1. Token source + docs.
2. Desktop CSS semantic aliases.
3. Android `WscanUi` token mapping.
4. Future generated platform outputs only after review.

## Guardrails

- Do not vendor upstream UI repositories in the first implementation PR.
- Do not copy icons, SVGs, USS/UXML, Kotlin generator code, or runtime helpers without preserving license notices.
- Do not add fake detector output or claim confirmed threats from demo rows.
- Do not touch Gemini/Vertex integration.
- Keep each PR under the repo's one-work-unit rule.
