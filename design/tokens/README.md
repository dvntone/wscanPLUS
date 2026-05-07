# wscan+ Design Tokens

This directory contains the seed semantic token source for the wscan+ tactical/operator UI language.

## Purpose

The token file is not a decorative palette dump. It defines stable semantic names that can be mapped into:

- desktop Electron CSS variables
- Android View/Kotlin color constants
- future Android Material 3 resources
- web/PWA CSS variables
- documentation and visual QA checklists

## Current scope

`wscan.tokens.json` is a source-of-truth seed for issue #301. The first implementation goal is consistency and traceability, not a full token build pipeline.

Runtime wiring should happen in small follow-up PRs so desktop, Android, and any web UI can adopt the tokens without broad rewrites.

## Naming rules

Use semantic names tied to product meaning:

- `color.canvas`
- `color.panel`
- `color.accentScan`
- `color.threatCritical`
- `color.rssiStrong`

Avoid names tied only to appearance:

- `blue500`
- `redBright`
- `cardColor2`

## Guardrails

- Do not vendor third-party design-system code into this directory without preserving upstream license notices.
- Do not use tokens to imply detector certainty. Threat colors are severity/status UI tokens only.
- Do not introduce paid map, font, icon, or theme dependencies as part of token adoption.
- Keep Gemini/Vertex Android integration untouched.
