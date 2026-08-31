# Business Life API Gaps — Audit & Target Contract

**Figma:** Company Life `695:9782`  
**Route:** `GET /v1/business/moments/:momentId/life`  
**Implementation:** [`backend/typescript/src/modules/projection/service.ts`](../../backend/typescript/src/modules/projection/service.ts)

---

## Live route (before parity)

Moment-scoped auth; company-scoped data from `projection.business_life`, `projection.business_pulse`, `business.issue`, `projection.recent_activity`, `business.business_system_setup`.

| Field | Status (pre-parity) |
|-------|---------------------|
| `kpis` | LIVE |
| `modules.*.score` | `null` (API_GAP) |
| `trends` | `DEFERRED` |
| `sections.vendorOperations` | `DEFERRED` |
| `signals` | Issues only (proxy) |
| `activity` | User-scoped |
| Share / Report CTAs | LOCAL_ONLY / disabled |

---

## Gap table (target)

| Gap | Before | After |
|-----|--------|-------|
| Module scores | `null` | Derived 0–100 from capacity, runway health, SLA/budget |
| Health trends | `DEFERRED` | `trends.series[]` from `projection.business_pulse_history` |
| Vendor ops | `DEFERRED` | `vendorOperationsPayload` + `modules.vendorOperations` |
| Signals | Issues only | Typed: `issue`, `capacity`, `runway`, `budget`, `sla`, `invoice` |
| Activity | Per-user | Company-wide BUSINESS moments |
| Share | Stub | `POST …/share-link` with token + expiry |
| Weekly report | Unwired | `GET …/weekly-report?period=7d\|30d` |

---

## Target `GET …/life` payload

```json
{
  "momentId": "uuid",
  "facet": "life",
  "payload": {
    "dataQuality": "OK",
    "sections": {
      "teamOperations": "REAL_DATA",
      "runway": "REAL_DATA",
      "businessOperations": "REAL_DATA",
      "vendorOperations": "REAL_DATA",
      "healthTrends": "OK"
    },
    "kpis": {
      "activeModuleCount": 2,
      "activeMomentCount": 3,
      "runwayMonths": "8.5",
      "financialHealthScore": "72",
      "attentionCount": 1
    },
    "modules": {
      "teamOperations": { "active": true, "statusLabel": "…", "score": "85" },
      "runway": { "active": true, "statusLabel": "…", "runwayMonths": "8.5", "score": "72", "revenueMomPct": 8, "expenseMomPct": -3 },
      "businessOperations": { "active": true, "statusLabel": "…", "score": "91" },
      "vendorOperations": { "active": true, "statusLabel": "3 active vendors", "score": "88" }
    },
    "teamOperationsPayload": { "capacityPct": 85 },
    "runwayPayload": { },
    "businessOperationsPayload": { },
    "vendorOperationsPayload": { "activeVendorCount": 3, "slaCompliancePct": 91 },
    "signals": [
      { "signalId": "…", "signalType": "issue", "title": "…", "family": "OPERATIONS", "statusLabel": "Action", "severity": "HIGH" }
    ],
    "activity": [ ],
    "journey": [ ],
    "trends": {
      "status": "OK",
      "series": [
        { "month": "2026-03", "financialHealthScore": 72, "teamScore": 85, "runwayScore": 72, "opsScore": 91 }
      ]
    }
  }
}
```

Scores are **never invented** — `null` / empty when underlying facts are absent.

---

## Satellite routes

| Route | When to call |
|-------|----------------|
| `POST /v1/business/moments/:momentId/share-link` | Share with Team CTA |
| `GET /v1/business/moments/:momentId/weekly-report?period=7d` | View Detailed Report |
| `GET …/capacity`, `…/mom-deltas` | Also inlined on Life GET; available for family screens |

---

## Closure pack overlap

Wave 3 P1 from [business_api_gaps_audit plan](../../.cursor/plans/business_api_gaps_audit_cf538a4e.plan.md): capacity, MoM, share, weekly report — folded into Life GET and hardened routes.

Wave 5 P2 (AI intelligence) remains **honest-empty** on Life.

---

## Family Life frames

`708:9524`, `700:10521`, `700:11150` reuse company Life — no separate endpoints.
