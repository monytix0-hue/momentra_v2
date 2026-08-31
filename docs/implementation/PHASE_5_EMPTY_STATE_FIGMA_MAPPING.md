# Phase 5 — Empty State Figma Mapping

File: [momentra](https://www.figma.com/design/TzLvwVwlPbeVB8ug1zB3GM/momentra)

**Status policy (2026-08 reset):** Do **not** mark MATCHED/PASS without a side-by-side Figma-node screenshot vs device screenshot. Interactive placeholders and scroll-PNG pastes are **FAIL**.

Statuses: `MATCHED` | `PARTIAL` | `DEFERRED` | `API_GAP` | `ASSET_GAP` | `FAIL`

| Frame/node | Context | Screen | State | Android | iOS | API source | Status |
|---|---|---|---|---|---|---|---|
| `353:317` Personal Pulse empty | Personal | Pulse | First / Between | `PersonalPulseEmptyContent` | `PersonalPulseEmptyView` | `GET /v1/personal/moments` | FAIL — live composed Figma body; awaiting screenshot compare |
| `353:391` Personal Moments empty | Personal | Moments | First / Between | `PersonalMomentsEmptyContent` | `PersonalMomentsEmptyView` | list + history | FAIL — live composed Figma body; awaiting screenshot compare |
| `353:452` Personal Create empty | Personal | Create | First / Between | `PersonalCreateEmptyContent` | `PersonalCreateEmptyView` | none (UI only) | FAIL — chooser on empty Create tab; Quick Add Hub only when active Moment |
| `353:6809` Life Operations Setup | Personal | Create → Setup | — | `PersonalLifeOpsSetupContent` | `PersonalLifeOpsSetupView` | `POST /v1/moments` + expanded prefs | FAIL — live Figma long-form; tabs scroll; habit2 Add; awaiting screenshot compare |
| `353:6905` Future Building Setup | Personal | Create → Setup | — | `PersonalFutureSetupContent` | `PersonalFutureSetupView` | same | FAIL — live Figma long-form; awaiting screenshot compare |
| `353:7075` Lifestyle Setup | Personal | Create → Setup | — | `PersonalLifestyleSetupContent` | `PersonalLifestyleSetupView` | same | FAIL — live Figma long-form; awaiting screenshot compare |
| `353:7217` Relationships Setup | Personal | Create → Setup | — | `PersonalRelationshipsSetupContent` | `PersonalRelationshipsSetupView` | same | FAIL — live Figma long-form; awaiting screenshot compare |
| `353:5780` Personal Life empty | Personal | Life | First / Between | `PersonalLifeEmptyContent` | `PersonalLifeEmptyView` | none (education) | FAIL — live composed Figma body; awaiting screenshot compare |
| `1047:7689` Personal Life populated | Personal | Life | Ready (all moments) | `PersonalLifeActiveContent` | `PersonalLifeActiveView` | `GET /v1/personal/life` | FAIL — live composed cross-moment Life Health; awaiting screenshot compare |
| `505:11793` Relationships Pulse | Personal | Pulse | Ready (RELATIONSHIPS) | `PersonalRelationshipsPulseActiveContent` | `PersonalRelationshipsPulseActiveView` | `GET /v1/personal/pulse` + activity | FAIL — Bond Index Pulse; awaiting screenshot compare |
| `505:12365` Lifestyle Pulse | Personal | Pulse | Ready (LIFESTYLE) | `PersonalLifestylePulseActiveContent` | `PersonalLifestylePulseActiveView` | `GET /v1/personal/pulse` + widgetPayload axes | FAIL — Vitality Index Pulse; awaiting screenshot compare |
| `505:12574` Lifestyle Moments | Personal | Moments | Ready (LIFESTYLE) | `PersonalLifestyleMomentsActiveContent` | `PersonalLifestyleMomentsActiveView` | pulse + activity | FAIL — Experience journey; awaiting screenshot compare |
| `505:12665` Lifestyle Memory | Personal | Memory | Ready (LIFESTYLE) | `PersonalLifestyleMemoryActiveContent` | `PersonalLifestyleMemoryActiveView` | pulse + activity (not GET /memory) | FAIL — Vitality memory; awaiting screenshot compare |
| `505:12002` Relationships Moments | Personal | Moments | Ready (RELATIONSHIPS) | `PersonalRelationshipsMomentsActiveContent` | `PersonalRelationshipsMomentsActiveView` | pulse + activity | FAIL — Bond journey; awaiting screenshot compare |
| `505:12093` Relationships Memory | Personal | Memory | Ready (RELATIONSHIPS) | `PersonalRelationshipsMemoryActiveContent` | `PersonalRelationshipsMemoryActiveView` | pulse + activity (not GET /memory) | FAIL — Bond memory; awaiting screenshot compare |
| `1036:7697` Relationships Recent Activity | Personal | Pulse → View All | Ready | `PersonalRelationshipsActivityFlow` | `PersonalRelationshipsActivityFlow` | activity list (+ demo seed) | FAIL — filters + edit/delete; awaiting screenshot compare |
| `1006:8434` Life Ops Activity Timeline | Personal | Pulse → View All (Life Ops / shared) | Ready | `PersonalRecentActivityFlow` | `PersonalRecentActivityFlow` | `GET /v1/personal/activity` | FAIL — search, filters, stats, rich rows; awaiting screenshot compare |
| `417:8759` Edit Transaction | Personal | Timeline expense edit | Ready | `PersonalEditTransactionSheet` | `PersonalEditTransactionSheet` | `PATCH .../expenses/:id` | FAIL — full form; awaiting screenshot compare |
| `417:8863` Upload Attachment | Personal | Edit Transaction attachments | Ready | `PersonalUploadAttachmentSheet` | `PersonalUploadAttachmentSheet` | — | API_GAP — local preview only |
| `453:9376` Master Expense | Personal | Expense create (Life Ops / Lifestyle / Relationships) | Ready | `PersonalMasterExpenseSheet` | `PersonalMasterExpenseSheet` | `POST .../expenses` | FAIL — premium form; awaiting screenshot compare |
| `1036:7727` Relationships Edit Activity | Personal | Pulse → Edit | Ready | `RelationshipsEditActivityBody` | `RelationshipsEditActivitySheet` | local save/delete | FAIL — Edit Activity sheet; awaiting screenshot compare |
| `1006:8274` Relationships Quick Add hub | Personal | Quick Add | Ready (RELATIONSHIPS) | `PersonalQuickAddHub` | `PersonalQuickAddHubView` | opens RelationshipsQuickAddKind sheets | FAIL — Action Center themed for Relationships; awaiting screenshot compare |
| `353:5875` Personal Memory empty | Personal | Memory | First / Between | `PersonalMemoryEmptyContent` | `PersonalMemoryEmptyView` | list + history | FAIL — live composed Figma body; awaiting screenshot compare |
| `431:14372` Personal empty section | Personal | all | First / Between | `ContextEmptyExperience` | `ContextEmptyExperienceView` | `GET /v1/personal/moments` | FAIL until child screens MATCHED |

**2026-08 rework:** Pulse/Moments/Life/Memory rebuilt as live composed bodies (no ◆ scaffold / scroll-PNG paste). Empty Create tab shows Figma chooser; empty CTAs navigate to Create. Scroll PNG leftovers removed. Status stays FAIL until device↔Figma screenshot compare.
| `575:8552` Group empty screens | Group | Moments/Life/Memory/Pulse | First | Group empty copy | Group empty copy | `GET /v1/group/moments` | PARTIAL |
| `575:8894` Group Create chooser | Group | Create | First / Ready | `GroupCreateMomentContent` / `GroupCreateFlow` | `GroupCreateMomentView` | none (UI) | FAIL — wired; Community/Goal Coming Soon; awaiting screenshot compare |
| `575:9917` Group Experience setup | Group | Create → Experience | — | `GroupExperienceSetupContent` | `GroupExperienceSetupView` | `POST /v1/moments` GROUP | FAIL — Activate posts; hero/01–04 parity pass started; awaiting screenshot compare |
| `575:9919` Group Purchase setup | Group | Create → Purchase | — | `GroupPurchaseSetupContent` | `GroupPurchaseSetupView` | `POST /v1/moments` GROUP | FAIL — Activate posts; CUSTOM→COMMUNITY_PURCHASE; awaiting screenshot compare |
| `575:10567` Group Living setup | Group | Create → Living | — | `GroupLivingSetupContent` | `GroupLivingSetupView` | `POST /v1/moments` GROUP | FAIL — Activate posts; CUSTOM→COMMUNITY_LIVING; awaiting screenshot compare |
| Phase 4 no-company Business | Business | setup | No company | setup empty | setup empty | `GET /v1/companies` | MATCHED |
| Between + history (all contexts) | All | empty + history | Between | history rows | history rows | list limit 20 / show 5 | MATCHED |

## Personal Figma fidelity notes

- **Source of truth:** Figma nodes above via `get_design_context` — **not** full-frame/scroll PNG paste, **not** generic ◆ placeholders.
- Shell chrome (TopBar / Context / BottomNav) stays native; recreate **body only**.
- Create (`353:452`): live 2×2 life-system cards with committed thumbs + Quick Start + tip.
- Life Ops / Future / Lifestyle / Relationships setups: long-form sheets (01–04) with catalog preference keys + Activate → `createPersonalMoment`. Live status counts, category tab scroll (Life Ops), Add → `habit2`. No setup scroll-PNG paste.
- See [`figma design.md`](../../figma%20design.md) FORBIDDEN SHORTCUTS + REQUIRED WORKFLOW.

**Personal visual MATCHED count:** **0** until screenshot compare passes.
