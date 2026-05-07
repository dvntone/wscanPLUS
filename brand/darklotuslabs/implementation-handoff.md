# darklotusLABS Landing Page Implementation Handoff

## Target

Implement a public landing page redesign for `darklotuslabs.com` that presents wscan+ as an operator-grade wireless intelligence platform under the **darklotusLABS** brand.

## Public routes

- Marketing site: `https://darklotuslabs.com`
- Web hub CTA: `https://app.darklotuslabs.com`

## Required hero content

Headline:

```text
Wireless intelligence, guided by .nyx.
```

Subheadline:

```text
wscan+ is a local-first wireless analysis platform for WiFi, BLE, signal mapping, anomaly detection, and evidence review.
```

Primary CTA:

```text
Launch Web UI
```

Secondary CTAs:

```text
Download Desktop
View GitHub
```

## Required hero visual

Create a product-first hero scene:

```text
Laptop dashboard in the background
Phone app in the foreground
.nyx robot stepping out of the phone screen
Cyan portal/ripple glow
One hand on phone bezel
One foot crossing phone screen edge
Cast shadow to prove intentional depth
```

Do not show only an earpiece or randomly clipped robot body.

## Required sections

1. Hero
2. Feature cards
3. Platform workflow
4. UI showcase
5. Privacy/local-first trust section
6. Open source/GitHub section
7. Final CTA

## Platform workflow

Use this exact conceptual model:

```text
Android Collector → Desktop Hub → Web Evidence Dashboard
```

Do not describe the system as command-and-control software. Use product-safe wording:

- evidence hub
- remote review
- optional sync
- operator portal
- dashboard workflow
- audit-aware actions

## Feature cards

Required cards:

- WiFi Analysis
- BLE Intelligence
- Anomaly Detection
- Spatial Awareness
- Evidence Review
- Desktop Hub
- Web Hub

## Trust section

Heading:

```text
Local-first by default. Operator-controlled by design.
```

Body:

```text
wscan+ is designed around local analysis and user-controlled evidence handling. You decide what leaves your device, what gets exported, and what is synced for remote review.
```

## Visual system

Colors:

```text
#05070B
#0B1220
#111827
#1E293B
#22D3EE
#2DD4BF
#94A3B8
#F8FAFC
```

Recommended fonts:

- Space Grotesk or Sora for headings
- Inter for body
- JetBrains Mono for data labels

Motion:

- subtle RF pulse
- hover elevation
- small scan ripple
- gentle panel transitions

Avoid:

- Matrix rain
- noisy glitch effects
- fake terminal clutter
- aggressive hacker visuals
- mascot-dominated layout

## Build checklist

- [ ] Add hero section with final copy.
- [ ] Add `Launch Web UI` CTA pointing to `https://app.darklotuslabs.com`.
- [ ] Add desktop/GitHub CTAs when URLs are finalized.
- [ ] Add phone + laptop product composition.
- [ ] Add .nyx emergence visual with hand/foot/shadow/portal cues.
- [ ] Add feature card section.
- [ ] Add platform workflow section.
- [ ] Add privacy/local-first trust section.
- [ ] Add UI showcase section.
- [ ] Add final CTA.
- [ ] Verify responsive behavior on mobile, tablet, and desktop.
- [ ] Verify contrast and keyboard accessibility.

## Source handling

Only public-safe artifacts belong in this monorepo. Keep raw design files, private mockups, secrets, and deployment material outside this public repository.
