# Retail GS1 GTIN Lookup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let Retail operators look up an unknown scanned GTIN via a backend advisory endpoint, prefill a product-master draft from registry data, and require merchant confirmation of price/GST/HSN before any sale posts — starting with a Harbor Mart stub, then a real GS1 provider behind an env key.

**Architecture:** Mirror the existing HSN advisory pattern (`GET /business/gst/hsn` → `operating-hsn.ts`). Add `GET /business/products/gtin/:gtin` that returns a normalized product draft from a pluggable provider (`stub` | `gs1` | `none`). Clients call it only on local-master miss (POS / Purchase / New product form). Never auto-create masters or auto-post sales from registry data. Provenance (`LOCAL` | `GS1`) and `lastSyncedAt` live on the master (payload first; columns only if indexing requires).

**Tech Stack:** Backend TypeScript (`v1Router`, `operating-masters`, new `operating-gtin.ts`); Android Compose (`RetailPosSheet`, `RetailCaptureSheets`, `RetailMasterForms`, `BusinessSliceRepository`); iOS SwiftUI (`RetailPosSheet`, `RetailCaptureSheets`, `RetailMasterForms`, `OperatingCatalog` / `APIClient`); existing ML Kit / AV scanners (raw string only).

**Spec:** This plan is the approved design source for GS1/GTIN integration (no separate `docs/superpowers/specs/` file). Grounded in current Retail scan/master flows (local catalogue match only; no GS1 code in repo today).

## Global Constraints

- **Honesty:** We do **not** have GS1 credentials, DataKart subscription, or Verified by GS1 API access today. Do not invent keys, claim live GS1, or call third-party APIs as “GS1” when they are not.
- **Advisory only:** GTIN lookup never creates `operating_master` rows and never posts `operating-event` sales/purchases.
- **Merchant-owned commercial fields:** Operator must confirm/enter sell price, MRP, purchase price, HSN, GST rate before save; registry data is a draft hint.
- **Local match first:** Always resolve against company masters (`barcode` → `code` → name) before any network lookup.
- **iOS barcode parity:** Product form must persist GTIN on `barcode` (Android already does); stop mapping “Barcode / EAN” only to `code`.
- **Harbor fixtures:** Keep `fx-*` / `productionMasterId` stripping; stub GTINs may seed demo masters but must not post fixture IDs as production `masterId`.
- **Open Food Facts:** Optional demo-only fallback (`GTIN_FALLBACK=openfoodfacts`); never label OFF results as GS1; UI source chip must say `Open Food Facts` / `Demo`, not `GS1`.

## Review Focus

1. **Unknown POS scan never charges:** Miss path offers “Look up GTIN” / “New product”; Charge still requires a saved master with merchant prices.
2. **Stub vs live labeling:** When `GTIN_PROVIDER=stub`, response `source` is `STUB` (or `DEMO`); UI never shows “Verified by GS1” for stub data.
3. **Cache does not invent completeness:** Cache hit returns same provenance/`lastSyncedAt`; missing brand/name stays missing — no fabricated catalogue.
4. **iOS create body includes `barcode`:** After form prefill + save, re-scan matches on `barcode` and `code` equally on both platforms.
5. **No auto-post from registry:** Purchase miss may open draft/prefill but must not silently treat registry MRP as posted purchase price without operator confirm.

---

## Access paths (research summary — India-focused)

| Path | Official GS1? | What it gives | Access reality for Momentra |
|------|---------------|---------------|------------------------------|
| **GS1 India DataKart / GTIN Validation** | Yes (GS1 India) | Brand-owner uploaded name, manufacturer, MRP, etc. via DataKart; portal + bulk Excel; “DataKart for Retailers” subscription for retailer access | Requires GS1 India commercial subscription / agreement. **No public free API key in repo.** Contact GS1 India for retailer API terms. |
| **Verified by GS1 (global registries)** | Yes (GS1) | GTIN licence holder, basic product attributes when brand activated data; portal search; enterprise API via **local GS1 Member Organisation** | Member/MO-issued credentials (examples: GS1 Sweden/Norway developer portals). **India: contact GS1 India** — do not assume EU MO credentials work for Momentra. |
| **GEPIR (legacy)** | Yes (historical GS1) | Company prefix / party lookup; largely superseded by Verified by GS1 | Prefer Verified by GS1 / DataKart; do not build on GEPIR as primary. |
| **Licensed aggregators** | Depends (may redistributor GS1-sourced data under licence) | Packaged GTIN APIs | Evaluate only after GS1 India path; treat as Phase B+ alternative if DataKart API lag. |
| **Open Food Facts** | **No** | Crowdsourced food product names/brands | **Demo/dev fallback only.** Never present as GS1. |

**Licensing posture:** Phase A ships with in-repo stub fixtures (Harbor Mart–style GTINs). Phase B enables real calls only when `GTIN_PROVIDER=gs1` **and** env credentials are present; otherwise return structured `503` / `NOT_CONFIGURED` so clients keep working offline with local masters.

---

## Architecture decision

**Chosen approach:** Backend-owned GTIN adapter + thin client “Look up GTIN” CTA on miss paths, modeled on HSN lookup.

**Why:** Credentials stay server-side; both Android and iOS share one contract; cache/rate-limit live once; stub→live swap is env-only; merchant confirmation UX stays in existing `RetailMasterFormSheet`.

**Trade-offs accepted:**
- Extra round-trip vs embedding a client SDK — preferred for key safety.
- DataKart/Verified field coverage varies by brand activation — UI must tolerate sparse responses.
- Payload provenance first avoids blocking on FRDS migration for Phase A.

**Rejected:** Client-direct GS1 calls (keys leak); auto-upsert master on scan (wrong ownership of price/GST); treating OFF as production GS1.

---

## Normalized API contract

`GET /business/products/gtin/:gtin`

**Auth:** Same as other `/business/*` routes (request context / company session). No company write.

**Path param:** Digits-only GTIN/EAN/UPC (8–14). Normalize: strip spaces; reject non-digit; optional leading-zero pad documentation in note (do not invent check-digit fixes that change identity).

**Response envelope:** `projectionEnvelope` like HSN.

```typescript
interface GtinLookupResult {
  gtin: string;
  found: boolean;
  source: 'STUB' | 'GS1' | 'OPEN_FOOD_FACTS' | 'CACHE' | 'NONE';
  /** Present when found */
  draft?: {
    name: string | null;
    brand: string | null;
    description: string | null;
    netContent: string | null;
    imageUrl: string | null;
    /** Hint only — never auto-apply to sell_price without confirm */
    suggestedMrp: string | null;
    licenseeName: string | null;
  };
  note: string; // e.g. "Advisory only. Confirm price, HSN, and GST before saving."
  lastSyncedAt: string | null; // ISO; cache write time when source CACHE
}
```

**Errors:** `400` invalid GTIN shape; `404` found=false with same body shape preferred over bare 404 for UX; `503` when provider configured but upstream down (clients show retry / manual create).

**Cache:** In-process LRU or Redis if already available — key `gtin:{normalized}`; TTL 7d for GS1 hits, 1h for misses; stub bypasses external TTL (static). Do not cache `NONE` forever when provider is `gs1` (short miss TTL).

---

## Data model

Existing: `business.operating_master.barcode` (TEXT), `code`, pricing + GST columns, `payload JSONB`.

**Phase A (no migration):** On create/update after GS1-assisted draft, set:

```json
payload.gtinProvenance = {
  "source": "LOCAL" | "GS1" | "STUB",
  "lastSyncedAt": "ISO-8601" | null,
  "gtin": "890..."
}
```

Operator-entered masters without lookup → `source: "LOCAL"`, `lastSyncedAt: null`.

**Phase B (optional migration):** Promote `gtin_source` / `gtin_last_synced_at` columns only if company-wide GTIN uniqueness or reporting needs indexes. Prefer unique partial index on `(company_id, barcode)` where barcode not null — separate task if product asks for it.

**Never:** Registry row as silent sales line; overwriting merchant sell_price on re-sync without explicit “Refresh from GS1” action (out of Phase C scope unless requested).

---

## Wire points (current code)

| Surface | Miss behavior today | GS1 hook |
|---------|---------------------|----------|
| Android POS `applyPosScan` | `RetailPosSheet.kt` ~349–350 sets `query` | After miss + looksLikeGtin → CTA “Look up GTIN” |
| iOS POS `applyPosScan` | `RetailPosSheet.swift` ~905–906 sets `skuQuery` | Same CTA in empty `skuMatches` region |
| Android Purchase `applyPurchaseScan` | Always adds line | Optional “Create product from GTIN” when unmatched + GTIN-shaped |
| iOS Purchase `applyPurchaseScan` | Same | Same |
| Android New product | `RetailMasterForms.kt` barcode field ~329–330 | “Look up” / scan-to-fill beside barcode |
| iOS New product | `RetailMasterForms.swift` binds EAN to `draft.code` ~375–376 | Fix `barcode` on draft + create body; then look up |
| Pattern to copy | `GET /business/gst/hsn`, `HsnLookupHints` | `GtinLookupHints` / `lookupGtin` |

---

## File map

| File | Role |
|------|------|
| Create `backend/typescript/src/modules/business/operating-gtin.ts` | Normalize GTIN, providers (`stubGtinLookup`, `gs1GtinLookup`, optional OFF), cache, `lookupGtin` |
| Create `backend/typescript/src/modules/business/operating-gtin-stub-fixtures.ts` | Harbor Mart / common India demo GTINs → draft |
| Modify `backend/typescript/src/api/v1/router.ts` | Register `GET /business/products/gtin/:gtin` |
| Create `backend/typescript/tests/.../operating-gtin.test.ts` | Stub hits/misses, invalid GTIN, provider none |
| Modify Android `ApiService.kt` / `Dto.kt` / `BusinessSliceRepository.kt` | `lookupGtin` |
| Create/Modify Android shared `GtinLookupHints` (near `HsnLookupField.kt`) | Debounced/manual lookup UI |
| Modify `RetailPosSheet.kt`, `RetailCaptureSheets.kt`, `RetailMasterForms.kt` | Miss CTA + prefill |
| Modify iOS `APIClient.swift` / repository | `lookupGtin` |
| Modify iOS `OperatingCatalog.swift` | Add `barcode` to draft + `toCreateBody()` |
| Modify iOS Retail POS/Capture/Master forms | Miss CTA + prefill + barcode field binding |
| Env sample / deploy docs (existing env pattern only) | `GTIN_PROVIDER`, `GS1_API_BASE_URL`, `GS1_API_TOKEN` (or MO-specific names once known), `GTIN_FALLBACK` |

---

## Phases + effort

| Phase | Deliverable | Effort (order-of-magnitude) |
|-------|-------------|------------------------------|
| **A** | Stub provider + backend route + unit tests; Harbor Mart fixture GTINs; no client UX required to demo via curl/Postman | **~1–2 days** |
| **B** | Real GS1 adapter behind env; cache; `NOT_CONFIGURED` when unset; no OFF in prod default | **~3–5 days** after credentials + sample payloads from GS1 India / MO (calendar time dominated by licensing) |
| **C** | Android + iOS miss CTAs, form prefill, iOS barcode fix, source chip, confirm-before-save copy | **~3–4 days** |

Total engineering once licensed: ~1.5–2 weeks. Licensing/procurement is the critical path for Phase B, not code.

---

### Task 1: Backend GTIN module + stub provider

**Files:**
- Create: `backend/typescript/src/modules/business/operating-gtin.ts`
- Create: `backend/typescript/src/modules/business/operating-gtin-stub-fixtures.ts`
- Create: `backend/typescript/tests/operating-gtin.test.ts` (or existing test layout sibling)
- Modify: `backend/typescript/src/api/v1/router.ts` (near HSN route ~1478)

**Interfaces:**
- Consumes: `projectionEnvelope`, request context pattern from HSN route
- Produces: `lookupGtin(gtin: string, opts?: { provider?: string }): Promise<GtinLookupResult>`; `normalizeGtin(raw: string): string | null`; env `GTIN_PROVIDER` default `stub` in non-production, `none` in production unless set

- [ ] **Step 1: Write failing tests** for `normalizeGtin` (reject letters; accept 8/12/13/14 digit), stub hit for a fixture GTIN returns `found:true` + `source:'STUB'`, unknown returns `found:false`, and note always present.

- [ ] **Step 2: Run tests — expect FAIL** (module missing).

- [ ] **Step 3: Implement `normalizeGtin` + stub fixtures + `lookupGtin`** with provider switch: `stub` | `none` | `gs1` (gs1 throws/returns NOT_CONFIGURED until Task 2). Include at least 5 Harbor-relevant stub SKUs (e.g. basmati/oil/soap-style names matching demo catalogue spirit — **synthetic GTINs clearly fake**, e.g. documented in fixture file header as non-real).

- [ ] **Step 4: Wire `GET /business/products/gtin/:gtin`** returning `projectionEnvelope({ ...lookup })`.

- [ ] **Step 5: Run tests — expect PASS.**

- [ ] **Step 6: Commit** `feat(retail): stub GTIN lookup API for GS1 integration`.

---

### Task 2: Phase B provider adapter (credentials-gated)

**Files:**
- Modify: `operating-gtin.ts`
- Modify: env documentation only where the repo already documents business env vars (do not invent a new markdown doc unless one exists for env)

**Interfaces:**
- Consumes: `GS1_API_BASE_URL`, `GS1_API_TOKEN` (or rename once GS1 India issues real names — keep adapter interface stable)
- Produces: `gs1GtinLookup(gtin): Promise<GtinLookupResult>` mapping MO JSON → `draft`; on missing env → `source:'NONE'`, `found:false`, note `GS1 provider not configured`

- [ ] **Step 1: Write failing test** with mocked `fetch`: configured env maps a minimal Verified-by-GS1-shaped payload (brandName/productDescription/licenseeName) into `draft`; missing token does not call network.

- [ ] **Step 2: Implement adapter + LRU cache** (module-level Map with TTL). Map only fields we trust; leave `suggestedMrp` null unless provider field is explicit and documented.

- [ ] **Step 3: Optional `GTIN_FALLBACK=openfoodfacts`** — only when `GTIN_PROVIDER=stub` or explicit fallback flag; `source:'OPEN_FOOD_FACTS'`; never when `GTIN_PROVIDER=gs1` succeeds.

- [ ] **Step 4: Run tests — PASS.**

- [ ] **Step 5: Commit** `feat(retail): env-gated GS1 GTIN provider adapter`.

**Blocker:** Do not mark Phase B “live” in product copy until a real Momentra subscription/token exists. Stub remains default for Harbor demos.

---

### Task 3: Android client — lookup + miss UX

**Files:**
- Modify: `apk/.../data/api/ApiService.kt`, `Dto.kt`, `BusinessSliceRepository.kt`
- Create or extend: shared `GtinLookupHints` near `HsnLookupField.kt`
- Modify: `RetailPosSheet.kt` (`applyPosScan` miss), `RetailCaptureSheets.kt` (purchase miss), `RetailMasterForms.kt` (barcode field)

**Interfaces:**
- Consumes: `GET /business/products/gtin/{gtin}` → `GtinLookupResponseDto`
- Produces: Prefill callbacks `(barcode, name, brand?, suggestedMrp?)` → open `RetailMasterFormSheet` Product kind

- [ ] **Step 1: Add DTO + `lookupGtin` API + repository method** (mirror `searchHsn`).

- [ ] **Step 2: POS miss:** If trimmed query matches GTIN shape and no catalogue hit, show secondary CTA “Look up GTIN” (and keep typed query). On success open New product with fields filled; on not found toast + offer blank New product with barcode set.

- [ ] **Step 3: Purchase miss:** Same lookup affordance before/alongside anonymous line — prefer opening master draft when operator chooses “Add to catalogue”.

- [ ] **Step 4: Master form:** Button/scan-to-fill on Barcode field calling lookup; show source chip (`Stub` / `GS1` / `Open Food Facts`); never auto-set sell price from MRP without operator edit (prefill MRP field only as editable hint).

- [ ] **Step 5: Manual QA on emulator** with stub backend: scan/type fixture GTIN → draft → save → re-scan adds cart line.

- [ ] **Step 6: Commit** `feat(android-retail): GTIN lookup on unknown scan and product form`.

---

### Task 4: iOS client — barcode parity + lookup + miss UX

**Files:**
- Modify: `OperatingCatalog.swift` (`OperatingMasterDraft`, `toCreateBody` / create body) — **add `barcode`**
- Modify: `RetailMasterForms.swift` — bind “Barcode / EAN” to `draft.barcode` (keep `code` as internal SKU if separate)
- Modify: `APIClient.swift` / `BusinessOperatingRepository.swift` — `lookupGtin`
- Modify: `RetailPosSheet.swift`, `RetailCaptureSheets.swift`

**Interfaces:**
- Consumes: same backend contract as Android
- Produces: parity with Android miss → prefill → `createMaster`

- [ ] **Step 1: Failing/compiling fix** — add `barcode` through draft → create/update payload → list DTO already has barcode.

- [ ] **Step 2: Wire `lookupGtin`** mirroring `searchHsn`.

- [ ] **Step 3: POS / Purchase / form UX** matching Android Task 3 copy and behavior.

- [ ] **Step 4: Verify** create master round-trip: saved `barcode` visible in catalogue and matched by `applyPosScan`.

- [ ] **Step 5: Commit** `feat(ios-retail): barcode field + GTIN lookup on unknown scan`.

---

### Task 5: Provenance on save + demo honesty

**Files:**
- Modify: master create bodies (Android `OperatingMasterBody` / iOS create body) to pass `payload.gtinProvenance` when draft came from lookup
- Modify: Harbor stub fixture list aligned with Android `harborMartFixtureCatalog` names where useful
- Modify: any retail missing-API honesty doc **only if** `docs/implementation/RETAIL_MISSING_APIS.md` should mention GTIN as advisory (one short bullet — skip new docs otherwise)

**Interfaces:**
- Consumes: lookup `source` + `lastSyncedAt` at confirm time
- Produces: master payload provenance; UI subtitle “From GS1 draft — confirm price & GST”

- [ ] **Step 1: On successful create from lookup**, set provenance in payload; LOCAL otherwise.

- [ ] **Step 2: Ensure Charge / Purchase paths unchanged** — still require masterId / confirmed lines; no registry short-circuit.

- [ ] **Step 3: Commit** `feat(retail): record GTIN provenance on master save`.

---

## Data flow

```
Scan / type code
    → local masters: barcode | code | name
        → HIT: add cart / line (unchanged)
        → MISS + looksLikeGtin:
            → GET /business/products/gtin/:gtin
                → cache?
                → provider stub | gs1 | fallback OFF
            → UI: draft sheet (name/brand/MRP hint/barcode)
            → operator sets sell price, HSN, GST
            → POST/PATCH operating-masters (provenance in payload)
            → reload masters → re-scan adds line
            → Charge / Post purchase (existing events only)
```

---

## Risks / licensing

1. **No credentials yet** — Phase B blocked on GS1 India / MO commercial process; code must degrade cleanly.
2. **Sparse registry data** — Many GTINs validate licence holder only; product description may be empty → operator types name.
3. **MRP ≠ sell price** — Indian MRP from DataKart is a ceiling/hint; Momentra sell price stays merchant-owned.
4. **Terms of use** — Caching, redistribution, and displaying GS1 data in a multi-tenant SaaS may need explicit licence language; keep data company-scoped and advisory.
5. **OFF mislabeling** — Strict source enum + UI chip; default OFF off in production.
6. **Synthetic stub GTINs** — Document as fake; do not collide with real company prefixes if avoidable (use clearly reserved test patterns and a fixture header comment).

---

## Ask-ready next step

**Approve Phase A stub + UI?**  
Recommended: implement **Task 1 (stub API)** immediately, then **Tasks 3–4 Phase C UX** against stub so Harbor Mart demos show “Look up GTIN” end-to-end. Start GS1 India / DataKart for Retailers conversation in parallel for Phase B — no fake keys.

---

## Execution handoff

Plan complete and saved to `docs/superpowers/plans/2026-11-12-retail-gs1-gtin-lookup.md`.

**Recommended execution:** Native for Phase A (small, interface-defining), then Subagent-driven for Android/iOS UX tasks so platform reviews stay independent.

Does this plan capture what you want, and should we proceed with **Phase A stub + UI** on approval?
