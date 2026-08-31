# Phase 6 — Figma Mapping (Moment Create)

## Source nodes (10/10)

| Node | Scope | Android | iOS | API | Functional | Visual parity |
|------|-------|---------|-----|-----|------------|---------------|
| 353:6809 | Personal — Life Operations | `PersonalLifeOpsSetupContent` | `PersonalLifeOpsSetupView` | `POST /v1/moments` + `personalSetup.LIFE_OPERATIONS` | FAIL | FAIL |
| 353:6905 | Personal — Future Building | `PersonalFutureSetupContent` | `PersonalFutureSetupView` | `personalSetup.FUTURE_BUILDING` | FAIL | FAIL |
| 353:7075 | Personal — Lifestyle | `PersonalLifestyleSetupContent` | `PersonalLifestyleSetupView` | `personalSetup.LIFESTYLE` | FAIL | FAIL |
| 353:7217 | Personal — Relationships | `PersonalRelationshipsSetupContent` | `PersonalRelationshipsSetupView` | `personalSetup.RELATIONSHIPS` | FAIL | FAIL |
| 575:9917 | Group — Shared Experience | `GroupExperienceSetupContent` | `GroupExperienceSetupView` | `POST /v1/moments` GROUP | FAIL | FAIL — long-form 575:9761; prefs LOCAL_ONLY |
| 575:9919 | Group — Shared Purchase | `GroupPurchaseSetupContent` | `GroupPurchaseSetupView` | `POST /v1/moments` GROUP | FAIL | FAIL — long-form; prefs LOCAL_ONLY |
| 575:10567 | Group — Shared Living | `GroupLivingSetupContent` | `GroupLivingSetupView` | `POST /v1/moments` GROUP | FAIL | FAIL — long-form; prefs LOCAL_ONLY |
| 579:12741 | Group — Add People sheet | `GroupAddPeopleSheet` | `GroupAddPeopleSheet` | `POST /v1/group/invites` (mint on open) | FAIL | FAIL until screenshot compare |
| 692:34736 | Business — Team Operations | `BusinessSetupWizardContent` | `BusinessCreateMomentView` wizard | `POST /v1/moments` + `businessSetup` + root `companyId` | PASS | PASS |
| 692:36690 | Business — Business Runway | same | same | `businessSetup.BUSINESS_RUNWAY` | PASS | PASS |
| 692:37188 | Business — Business Operations | same | same | `businessSetup.BUSINESS_OPERATIONS` | PASS | PASS |

## Concrete flows (19/19)

### Personal (4)

| Flow | momentTypeCode | systemCode |
|------|----------------|------------|
| Life Operations | LIFE_RHYTHM | LIFE_OPERATIONS |
| Future Building | FUTURE_GOAL | FUTURE_BUILDING |
| Lifestyle | LIFESTYLE | LIFESTYLE |
| Relationships | RELATIONSHIP_CONNECTION | RELATIONSHIPS |

### Group Experience (4) — node 575:9917

TRIP, WEDDING, HOUSE_PARTY, OFFICE_OUTING

### Group Purchase (4) — node 575:9919

GIFT_POOL, GROUP_PURCHASE, SHARED_ASSET, CUSTOM

### Group Living (4) — node 575:10567

FLATMATES, FAMILY_HOUSEHOLD, CO_LIVING, CUSTOM

### Business (3)

TEAM_OPERATIONS, BUSINESS_RUNWAY, BUSINESS_OPERATIONS — root `companyId` only (not duplicated in `businessSetup`)

## API gaps

| Gap | Handling |
|-----|----------|
| Group `groupId` scope field | **API_GAP** — OpenAPI has no contracted group scope; service uses authenticated organizer + `group_moment_context` |
| Group budget / preference rows in Figma long-form | **LOCAL_ONLY** — interactive on Experience/Purchase/Living wizards; excluded from `POST /v1/moments` until Group preferences API |
| Business Memory chooser (658:9573) | **NOT_IN_SCOPE** — tab shows DEFERRED; no runtime mock memories |

## Phase 5 fallback removal

Scroll PNG runtime activate paths removed for all rows above. PNG assets may remain in repo for reference only.

## Preference key contracts

Validated server-side in `setup-preferences.ts` — see `PHASE_6_MOMENT_CREATE.md` and backend catalogs:

- `backend/typescript/src/modules/personal/setup-service.ts`
- `backend/typescript/src/modules/business/setup-service.ts`

Personal preview / intelligence sentences are **client-derived only**; chip selections persist, generated copy does not.
