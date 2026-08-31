# S5 Life360 Audit

**Date:** 2026-08-26  
**Scope:** Global Life360 surface — Coming Soon only (no client API)  
**Figma:** file `TzLvwVwlPbeVB8ug1zB3GM` / [`1075:7637`](https://www.figma.com/design/TzLvwVwlPbeVB8ug1zB3GM/momentra?node-id=1075-7637) — **"Life 360: Coming Soon"**  
**Rule:** Prefer REUSE/REFINE. **S5 UI must not call `GET /v1/life360` or read `projection.life360`.**  
**Guardrails:** GlobalSurface overlay (not a Context); shell selection preserved; Notify CTA LOCAL_ONLY; progress “65%” decorative LOCAL_ONLY.

---

## Executive summary

| Finding | Classification |
|---------|----------------|
| TopBar Life360 entry + mutual exclusion with Profile | **REUSE** (S1) |
| Android `Life360GlobalSurface` stub copy | **REFACTOR** → Figma Coming Soon |
| iOS Life360 sheet placeholder | **REFACTOR** → Figma Coming Soon |
| Live `GET /v1/life360` + stub `{ userId, circles: [] }` | **EXISTS** / **OUT_OF_SCOPE** for S5 UI |
| SQL `projection.life360` (V014) | **EXISTS** / **OUT_OF_SCOPE** for S5 UI |
| Client `hasLife360` / `getLife360` | **NOT_CONNECTED** — must not drive S5 UI |
| Populated Life360 product / AI / live metrics | **DEFERRED** / **SKIP** (S5-E) |
| Notify Me When Ready | **LOCAL_ONLY** ack (no notify API) |
| Development Progress 65% | **LOCAL_ONLY** decorative |
| Circle CRUD / S6 | **OUT_OF_SCOPE** |

---

## Product lock (S5)

```text
Life360 UI in S5 = Figma Coming Soon (1075:7637) ONLY
→ Do NOT call GET /v1/life360 from Android or iOS (S5 surface)
→ Do NOT read projection.life360 for the S5 surface
→ Do NOT background-refresh / SWR Life360
```

**Architecture (future; not wired in S5 clients):**

```text
Personal ─┐
Group ────┼──→ Life360 Projection ──→ Global Life360 UI   ← later stage
Business ─┤
Circle ───┘

S5 ships: TopBar → static Coming Soon overlay only
```

---

## Shell entry (proven)

| Concern | Behavior |
|---------|----------|
| Entry | TopBar radar / Life360 action |
| Presentation | Global overlay / sheet — **not** a context tab |
| Mutual exclusion | Opening Life360 closes Profile and vice versa |
| Isolation | `selectedContext` / company / moment / bottom tab **unchanged** on open/dismiss |
| AuthZ for aggregation | **DEFERRED** until API-connected stage |

---

## Backend inventory (document only — unused by S5 UI)

### Route

| Item | Status |
|------|--------|
| `GET /v1/life360` | Live stub returns `{ userId, circles: [] }` (or equivalent envelope) |
| OpenAPI / generated clients | May list the operation — **S5 UI must not invoke** |

### SQL — `projection.life360` (V014)

| Column | Notes |
|--------|-------|
| `user_id` | PK → `core.user_profile` |
| `personal_payload` / `group_payload` / `business_payload` | JSONB defaults `{}` |
| `attention_payload` / `recent_activity_payload` | JSONB defaults `{}` |
| `source_event_id` / `projection_version` / `updated_at` | Projection metadata |

**S5:** no client read path; no writers required for Coming Soon UI.

### Future projection contract (S5-D — docs only)

When a later stage connects UI:

| Field (conceptual) | Source | Empty honesty |
|--------------------|--------|---------------|
| `userId` | auth actor | required |
| Context payloads | `projection.life360.*_payload` | `{}` = empty-honest, not fake metrics |
| `circles` | circle membership / projection | `[]` until Circle product |
| `status` / readiness | server | Coming Soon vs ready — **not used in S5** |

No OpenAPI/client/backend changes required for S5 UI.

---

## Figma section classification (`1075:7637`)

| Section | Node (approx) | S5 treatment |
|---------|---------------|--------------|
| Status bar | chrome | OS — **NOT_REQUIRED** in overlay body |
| TopBar + Context switcher | shell chrome in Figma frame | **REUSE** existing shell under overlay — do not duplicate in sheet body |
| Hero — Coming Soon badge | content | **LOCAL_ONLY** static |
| Title “Life 360” + subtitle | content | **LOCAL_ONLY** static |
| Life Intelligence illustration | content | **LOCAL_ONLY** static / drawn |
| “Your Life Map is waiting…” card | content | **LOCAL_ONLY** static |
| Feature preview grid (4 cards) | content | **LOCAL_ONLY** static labels — not live metrics |
| Development Progress 65% | content | **LOCAL_ONLY** decorative |
| Notify Me When Ready | CTA | **LOCAL_ONLY** ack / **DEFERRED** notify API |
| Info card footer | content | **LOCAL_ONLY** static |
| Close / dismiss | shell | Always available; no network |

---

## Theme

| Token | Value | Source |
|-------|-------|--------|
| Surface / TopBar | `#0C0F15` | `GlobalSurfaceTheme.life360` / matrix |
| Action circle | `#1E293B` | matrix |
| Online / status | `#10B981` | matrix |
| Coming Soon page bg | `#14121B` | Figma `1075:7637` |
| Card surface | `#161B26` | Figma |
| Gold accent | `#F2CA50` → `#FFAB40` | Figma badge / CTA / progress |
| Text primary | `#E5E0EE` | Figma |
| Text secondary | `#C9C4D8` | Figma |

---

## Out of scope

Connecting `/life360`, reading `projection.life360`, SWR/refresh, populated product, AI, Circle CRUD, S6–S9, V030.
