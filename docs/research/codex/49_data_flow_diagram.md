# Device ? Desktop Data Flow (Conceptual)

```mermaid
flowchart LR
  A["Android Device"] --> B["Local Scan Capture"]
  B --> C["Observation Store"]
  C --> D["Anomaly Candidate Scoring"]
  D --> E["Alert Event (Evidence Mode)"]
  C --> F["Export/Sync Queue"]
  F --> G["Desktop Hub (Debian/Kali)"]
  G --> H["Timeline + Heuristic Review"]

  subgraph Android
    A
    B
    C
    D
    E
    F
  end

  subgraph Desktop
    G
    H
  end
```

## Notes
- Observation Store is **local**, retaining timestamps, RSSI, SSID/BSSID, and coarse geo.
- Alert Event is **gated** by frequency rules to avoid false positives.
- Export/Sync Queue can be offline?safe; Desktop Hub reconciles when connected.
- Evidence Mode allows full identifiers when anomaly is detected.
