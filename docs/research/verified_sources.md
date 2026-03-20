# REFERENCES3 — Verified Sources + Evidence-Linked Ideas

Created: 2026-03-19
Scope: New, verified resources (GitHub + official docs) and ideas grounded in those sources only.

## New Verified Resources

1. [WiGLE WiFi Wardriving Android app](https://github.com/wiglenet/wigle-wifi-wardriving)
2. [WiFiAnalyzer (Android)](https://github.com/VREMSoftwareDevelopment/WiFiAnalyzer)
3. [Kismet (GitHub mirror)](https://github.com/kismetwireless/kismet)
4. [Kismet Alerts (official docs)](https://www.kismetwireless.net/docs/readme/alerts/alerts/)
5. [OpenWIPS-ng (GitHub)](https://github.com/aircrack-ng/OpenWIPS-ng)
6. [OpenWIPS-ng (official site)](https://openwips-ng.com/)
7. [CrowdSec CTI docs](https://docs.crowdsec.net/cti/)
8. [CrowdSec CTI API introduction](https://docs.crowdsec.net/u/cti_api/api_introduction)
9. [CrowdSec community CTI key info](https://www.crowdsec.net/blog/community-cti-api-key)
10. [WiGLEUploader formats](https://github.com/forresttindall/WiGLEUploader)

## Evidence-Linked Ideas (No Guessing)

1. Wardriving baseline features: WiGLE’s Android app demonstrates geolocated logging, map views, search, and export workflows. Use as a concrete UX/feature baseline for the companion app.
   Source: [WiGLE app](https://github.com/wiglenet/wigle-wifi-wardriving)

2. Export compatibility targets: WiGLEUploader lists the wardriving formats WiGLE accepts (including Kismet and NetStumbler). Use this list as a compatibility target for export formats.
   Source: [WiGLEUploader](https://github.com/forresttindall/WiGLEUploader)

3. Channel and signal visualization: WiFiAnalyzer provides channel graphs, signal-over-time, band filtering, security filtering, and vendor/OUI lookup. These are proven UI patterns for scanning results.
   Source: [WiFiAnalyzer](https://github.com/VREMSoftwareDevelopment/WiFiAnalyzer)

4. WIDS alert taxonomy: Kismet’s alert system defines stateless and stateful alerts, including trend-based and flood/DoS alerts. This is a direct reference for alert categories and rule structure.
   Source: [Kismet alerts](https://www.kismetwireless.net/docs/readme/alerts/alerts/)

5. Distributed sensor model: OpenWIPS-ng documents a sensor/server/interface architecture suitable for sensor nodes feeding a hub.
   Source: [OpenWIPS-ng](https://openwips-ng.com/)

6. CTI dataset semantics: CrowdSec’s CTI API defines datasets like fire and smoke; use them as distinct cache/TTL classes and confidence inputs.
   Source: [CTI API introduction](https://docs.crowdsec.net/u/cti_api/api_introduction)

7. CTI endpoint confirmation and limits: CrowdSec’s community CTI key post confirms /v2/smoke/{ip} and provides community-tier constraints.
   Source: [Community CTI key info](https://www.crowdsec.net/blog/community-cti-api-key)
