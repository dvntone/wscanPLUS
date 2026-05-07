# Third-Party Attribution Notes

This file records design-system and theme-generation influences for wscan+.

It is not a vendor manifest. If upstream code, assets, icons, SVGs, stylesheets, generators, or runtime helpers are copied or adapted later, the relevant upstream license files must be preserved alongside the copied material.

## Sinan Ata / Leap of Legends — Unity UI Toolkit design-system influence

The wscan+ design-system architecture was informed by the open-source Unity UI Toolkit design-system work by Sinan Ata / Leap of Legends.

Useful adopted concepts:

- tokenized visual language
- reusable component discipline
- showcase/test-harness style UI validation
- mobile/desktop override thinking
- production UI consistency over one-off screen styling

Original source:

```text
https://github.com/sinanata/unity-ui-document-design-system
```

Compliance note:

If any code, USS, UXML, SVG icons, runtime helpers, or assets from that repository are copied or adapted, preserve the upstream MIT license notice.

## RikkaApps MaterialThemeBuilder — Android theme-generation influence

The wscan+ Android theme-token strategy was informed by RikkaApps/MaterialThemeBuilder.

Useful adopted concepts:

- generated theme resources
- light/dark token derivation
- semantic color mapping before platform output
- keeping generated theme artifacts reproducible

Original source:

```text
https://github.com/RikkaApps/MaterialThemeBuilder
```

Compliance note:

If generator code, Gradle logic, generated-resource templates, or other implementation code from that repository are copied or adapted, preserve the upstream license notice.

## Current implementation stance

The current #301 token seed does not vendor either upstream repository. It only documents design influence and creates a wscan+ owned token source.
