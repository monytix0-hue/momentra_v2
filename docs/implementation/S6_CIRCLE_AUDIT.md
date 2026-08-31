# S6 Circle Audit

**Date:** 2026-08-26  
**Scope:** Circle context — Coming Soon only (no CRUD / no client API for UI)  
**Figma:** file `TzLvwVwlPbeVB8ug1zB3GM` / [`1075:7556`](https://www.figma.com/design/TzLvwVwlPbeVB8ug1zB3GM/momentra?node-id=1075-7556) — **"Circle: Coming Soon"**  
**Rule:** Prefer REUSE/REFINE. **S6 UI must not call `GET /v1/life360`, invent Circle CRUD, or read `projection.life360`.**  
**Guardrails:** Circle is a **ContextSwitcher context** (≠ Life360 global overlay); Notify CTA LOCAL_ONLY; progress “45%” decorative LOCAL_ONLY.

---

## Executive summary

| Finding | Classification |
|---------|----------------|
| CIRCLE in bootstrap `supportedContexts` | **REUSE** |
| ContextSwitcher tab for Circle | **REUSE** |
| Android/iOS hard-`Deferred` placeholder copy | **REFACTOR** → Figma Coming Soon |
| No dedicated Circle empty/active screens | **MISSING** → Coming Soon |
| CIRCLE theme = Personal purple `#7C5CFC` | **REFACTOR** → Figma pink `#E86BA3` / `#FC6A8B` |
| Live `GET /v1/life360` stub `{ userId, circles: [] }` | **EXISTS** / **NOT_CONNECTED** for S6 UI |
| SQL `projection.life360` (V014) | **EXISTS** / **OUT_OF_SCOPE** for S6 UI |
| Circle CRUD routes / `circle.*` schema | **OUT_OF_SCOPE** / **CONTRACT_DEFERRED** |
| Populated Circle product | **SKIP** (S6-E) |
| Notify Me When Ready | **LOCAL_ONLY** |
| Development Progress 45% | **LOCAL_ONLY** decorative |
| Life360 TopBar overlay (S5) | **OUT_OF_SCOPE** — leave unchanged |
| S7 Global/Settings | **OUT_OF_SCOPE** |

---

## Product lock (S6)

```text
Circle UI in S6 = Figma Coming Soon (1075:7556) ONLY
→ Circle is a ContextSwitcher context (≠ Life360 global overlay)
→ Do NOT call GET /v1/life360 from Circle UI
→ Do NOT invent Circle CRUD or new SQL
→ Do NOT ship populated Circle product
```

**vs Life360 (S5):**

```text
Life360 = TopBar global overlay (gold Coming Soon)
Circle  = Context tab body (pink Coming Soon)
```

---

## Shell entry (proven)

| Concern | Behavior |
|---------|----------|
| Entry | ContextSwitcher → CIRCLE |
| Presentation | Shell body under TopBar + ContextSwitcher |
| Moments | Always empty; moment switcher **hidden** |
| Company | N/A |
| Isolation | No Personal/Group/Business moment or finance surfaces |
| Loading/Error/Offline | Shell-level when bootstrap not Ready; Retry = shell reload |
| AuthZ aggregation | **DEFERRED** until API-connected stage |

---

## Backend inventory (document only — unused by S6 UI)

### Route

| Item | Status |
|------|--------|
| `GET /v1/life360` | Live stub `{ userId, circles: [] }` — OpenAPI tag Circle |
| Circle CRUD | **CONTRACT_DEFERRED** |
| OpenAPI / generated `CircleApi` | May exist — **S6 UI must not invoke** |

### SQL

| Item | Status |
|------|--------|
| `circle.*` schema | **None** |
| `projection.life360` (V014) | Table exists; unused by stub API and S6 UI |

### Bootstrap capability (S6-D)

| Item | Status |
|------|--------|
| `supportedContexts` includes `CIRCLE` | Always |
| Circle-specific capability seed | **None** — do not invent |
| Future contract | Life360 projection read + Circle CRUD deferred |

---

## Figma section classification (`1075:7556`)

| Section | S6 treatment |
|---------|--------------|
| Status bar / TopBar / ContextSwitcher | **REUSE** shell chrome (Circle selected accent pink) |
| Hero network illustration | **LOCAL_ONLY** static / drawn |
| Coming Soon badge | **LOCAL_ONLY** |
| Title “Circle” + subtitle | **LOCAL_ONLY** |
| What Circle Will Reveal (2×2) | **LOCAL_ONLY** labels — not live metrics |
| Development card | **LOCAL_ONLY** |
| Progress 45% | **LOCAL_ONLY** decorative |
| Notify Me When Ready | **LOCAL_ONLY** ack |
| Info footer card | **LOCAL_ONLY** |

---

## Theme (target)

| Token | Value | Source |
|-------|-------|--------|
| Context accent / selected | `#E86BA3` / `#FC6A8B` | Figma |
| Accent end | `#FF6B8A` | Figma gradient |
| Secondary lavender | `#B794F6` | Feature card |
| Page bg | `#14121B` → `#1C1B1B` | Figma |
| Card | `#161B26` / `#1C1B1B` | Figma |
| Text primary | `#E5E2E1` | Figma |
| Text secondary | `rgba(208,197,175,0.8)` | Figma |

---

## Out of scope

Circle CRUD, reading `projection.life360` for UI, SWR, populated Circle, AI, Life360 overlay changes, S7–S9, V030.
