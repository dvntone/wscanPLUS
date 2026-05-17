# 64 — Cloudflare Workers AI as Gemini Replacement

**Date:** 2026-05-17  
**Status:** Approved direction — implementation pending  
**Supersedes:** Gemini/firebase-ai threat narrative layer (Phase 4)

---

## Decision

Replace `GeminiThreatAnalyzer` (firebase-ai) with a Cloudflare Workers AI endpoint.

**Drivers:**
- Gemini/Firebase AI Pro subscription: unreliable availability, unexpectedly high cost at current usage
- OpenRouter: evaluated, also too costly at scale
- Groq: free tier viable for personal use but rate limits hit under continuous scanning; not production-safe
- Cloudflare Workers AI: near-zero cost at wscanplus scan volumes, already planned as web UI backend platform

---

## Architecture

**Current:**
```
WatchdogService → GeminiThreatAnalyzer (firebase-ai SDK) → GeminiNarrativeEntity (DB)
```

**Target:**
```
WatchdogService → AiThreatAnalyzer (OkHttp → CF Worker HTTPS) → structured JSON → ThreatSignal boost + NarrativeEntity
```

The CF Worker acts as a thin proxy:
- Receives POST with threat signals (JSON)
- Calls `@cf/meta/llama-3.1-8b-instruct-fast` (or Mistral Small 3.1 24B for better quality)
- Returns structured JSON: `{ severity: "HIGH"|"MEDIUM"|"LOW", confidence: float, reasons: string[] }`
- Android side: pure OkHttp call, no Firebase SDK dependency

**Web UI alignment:** The same CF Worker deployment that serves the web UI control center backend hosts the AI analysis endpoint. One platform, one deployment, no Google dependency.

---

## Cost Analysis

wscanplus scan cadence: ~1 analysis per 5-min cooldown window (consent-gated, on-demand)  
Prompt size estimate: ~500 tokens in, ~200 tokens out

| Model | Input $/M | Output $/M | Cost per analysis | Cost at 100/day |
|-------|-----------|------------|-------------------|-----------------|
| Llama 3.1 8B fast | ~$0.044 | ~$0.044 | ~$0.000033 | ~$0.0033 |
| Mistral Small 3.1 24B | ~$0.11 | ~$0.11 | ~$0.000083 | ~$0.0083 |

At personal/field use scale: effectively **$0/month**.

CF free tier includes 10,000 Neurons/day — sufficient for low-volume usage without any payment.

---

## Migration Scope

**Android changes:**
- Remove `firebase-ai` and `firebase-bom` dependencies from `android/app/build.gradle.kts`
- Remove `GeminiThreatAnalyzer.kt`, `GeminiAnalysisResult.kt`
- Add `AiThreatAnalyzer.kt` — OkHttp POST to CF Worker endpoint
- Add CF Worker URL to `android/app/.env` / `local.properties` (gitignored)
- Update `WatchdogService` to use new analyzer

**New infrastructure:**
- CF Worker in `desktop/` or new `worker/` directory
- Accepts POST `{ signals: ThreatSignal[] }`, returns `{ severity, confidence, reasons }`
- Deployed under same CF account as web UI backend

**Known constraint:** Do not modify Gemini/Vertex AI integration without maintainer approval per AGENTS.md. This is a full replacement, not a modification — open dedicated issue for @dvntone approval before implementation.

---

## Groq as Fallback Option

If CF Workers AI has availability issues:
- Groq free tier: 100K tokens/day, 1K req/day — viable for personal use
- Llama 3.1 8B: $0.05/$0.08 per M tokens — near-zero at wscanplus scale
- OpenAI-compatible API — same integration pattern as CF Worker

---

## References

- [Cloudflare Workers AI pricing](https://workers.cloudflare.com/pricing/)
- [Workers AI model catalog](https://developers.cloudflare.com/workers-ai/llms-full.txt)
- [Groq pricing](https://groq.com/pricing)
- [Groq rate limits](https://console.groq.com/docs/rate-limits)
