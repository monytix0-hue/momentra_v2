# Personal Family Precision Freeze — Future / Lifestyle / Relationships (PX-1..PX-3)

**Status:** Provisional freeze derived from shipped Figma nodes + V003 canonical tables (no external 407 Excel pack in-repo for these families).  
**Honesty:** Widgets are MAPPED to precision reads, STATIC copy, or honest empty — never fabricate scores.

## Freeze boundary

| Family | Prefix | Canonical tables (V003+) | Profile (V046) |
|--------|--------|--------------------------|----------------|
| Future Building | `PER-FU-*` | `future_opportunity`, `future_pivot`, `future_learning_activity`, `future_progress_observation` | `personal.future_building_profile` |
| Lifestyle | `PER-LS-*` | `lifestyle_activity` | `personal.lifestyle_profile` |
| Relationships | `PER-RE-*` | `relationship_connection`, `relationship_activity` | `personal.relationships_profile` |

Life Ops (`PER-LO-*`) remains owned by V042–V045 / `life-ops-precision.ts`.

## Figma screen anchors

| Family | Setup | Pulse | Moments | Memory | Quick Add hub |
|--------|-------|-------|---------|--------|---------------|
| Future | `353:6905` | shared Pulse family branch | Future Moments | Future Memory | Milestone `353:11724`, Opportunity `353:11768`, Pivot `353:11812` |
| Lifestyle | `353:7075` | `505:12365` | `505:12574` | `505:12665` | Experience / Wellbeing / Discovery / Expression / Adjust |
| Relationships | `353:7217` | `505:11793` | `505:12002` | `505:12093` | `1006:8274` |

## Widget inventory (precision contracts)

### Future Building (`PER-FU`)

| Widget ID | Surface | Ownership | Contract |
|-----------|---------|-----------|----------|
| PER-FU-S01-RUNTIME | Pulse / Moments header | `GET .../future-runtime-summary` | entriesToday, openOpportunityCount, openLearningCount, lastItemAt |
| PER-FU-S02-AXES | Pulse Vision/Growth/Momentum/Discipline | `GET .../future-axis-snapshot` | Deterministic from table counts (null if zero activity) |
| PER-FU-S03-INVENTORY | Moments list | `GET .../future-inventory` | Typed rows from four Future tables |
| PER-FU-S04-JOURNEY | Memory timeline | `GET .../future-journey` | Chronological Future events |
| PER-FU-S05-PROFILE | Setup / Adjust | `PATCH .../future-profile` | Profile codes from setup |
| PER-FU-W-ITEM | Quick Add | `POST .../future-items` (enriched) | opportunityType, pivotReason, providerName, progressType |

### Lifestyle (`PER-LS`)

| Widget ID | Surface | Ownership | Contract |
|-----------|---------|-----------|----------|
| PER-LS-S01-RUNTIME | Pulse header | `GET .../lifestyle-runtime-summary` | activity counts by context |
| PER-LS-S02-VITALITY | Joy/Fulfillment/Vitality/Exploration | `GET .../lifestyle-vitality-snapshot` | Deterministic from context counts + avg wellbeingRating |
| PER-LS-S03-INVENTORY | Moments | `GET .../lifestyle-inventory` | `lifestyle_activity` rows |
| PER-LS-S04-JOURNEY | Memory | `GET .../lifestyle-journey` | Chronological activities |
| PER-LS-S05-PROFILE | Setup | `PATCH .../lifestyle-profile` | Profile codes |
| PER-LS-W-ACTIVITY | Quick Add | `POST .../lifestyle-activities` (enriched) | locationText, startAt/endAt |

### Relationships (`PER-RE`)

| Widget ID | Surface | Ownership | Contract |
|-----------|---------|-----------|----------|
| PER-RE-S01-RUNTIME | Pulse header | `GET .../relationships-runtime-summary` | connectionCount, activityCount |
| PER-RE-S02-BOND | Trust/Care/Support/Presence | `GET .../relationships-bond-snapshot` | Deterministic from activity_type counts — **no fixed-72 heuristics** |
| PER-RE-S03-CONNECTIONS | Moments | `GET .../relationships-connections` | Active connections |
| PER-RE-S04-JOURNEY | Memory | `GET .../relationships-journey` | Activity timeline |
| PER-RE-S05-PROFILE | Setup | `PATCH .../relationships-profile` | Profile codes |
| PER-RE-W-ACTIVITY | Quick Add | `POST .../relationship-activities` (enriched) | investmentValue, unitCode, relationshipType |

## Axis formulas (deterministic)

Scores are `null` when the underlying count is 0 (honest empty). Otherwise `clamp(40 + count * 12, 0, 100)` blended with rating averages where present.

| Family | Axis | Source count |
|--------|------|--------------|
| Future | vision | milestones + progress observations |
| Future | growth | learning + opportunities |
| Future | momentum | pivots + progress |
| Future | discipline | open learning IN_PROGRESS + PURSUING opportunities |
| Lifestyle | joy | EXPERIENCE count |
| Lifestyle | fulfillment | WELLBEING count (+ avg rating) |
| Lifestyle | vitality | blended active contexts |
| Lifestyle | exploration | DISCOVERY + CREATION |
| Relationships | trust | INTERACTION count |
| Relationships | care | SUPPORT count |
| Relationships | support | SUPPORT + RELATIONSHIP_INVESTMENT |
| Relationships | presence | SHARED_EXPERIENCE |
| Relationships | bondIndex | avg of non-null axes |

## Modules / migrations

| Pack | Migration | Module |
|------|-----------|--------|
| PX-1..3 profiles | `V046__personal_family_precision_profiles.sql` | `future-precision.ts`, `lifestyle-precision.ts`, `relationships-precision.ts` |
| GX-1 settlements | `V047__settlement_record_capability_map.sql` | existing `group-expense.createSettlement` |

Shared shell (`/personal/pulse`, setups, activity) stays generic; family GETs are domain-owned.
