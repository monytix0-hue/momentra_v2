# Phase 4 — Navigation Map

```text
App
│
├── Splash (covers Firebase restore + /v1/me when session exists)
│
├── Signed Out
│   ├── Onboarding (local prefs — ONBOARDING_PERSISTENCE_GAP)
│   └── Authentication (Email / Phone / Google → Firebase → GET /v1/me)
│
└── Signed In  (AuthPhase.Authenticated + ShellIdentity)
    │
    ├── Shell chrome (always)
    │   ├── MomentraTopBar
    │   │     └── Business only: Company selector
    │   ├── Context Switcher → Personal | Group | Business | Circle
    │   ├── Moment Switcher (Group/Business when non-empty; never on setup/Personal/Circle)
    │   └── Bottom Navigation → Pulse | Moments | Create | Life | Memory
    │
    ├── Personal
    │   ├── Pulse / Moments / Create / Life / Memory
    │   └── Real empty shells (no domain load in Phase 4)
    │
    ├── Group
    │   ├── Empty: no Group Moments
    │   ├── Ready: shell chrome only (features deferred)
    │   └── Forbidden: access removed (session kept)
    │
    ├── Business
    │   ├── Company selector (authorized companies only)
    │   ├── Empty: no companies (setup — no Moment Switcher)
    │   └── With company: shell + optional Moment Switcher chrome (empty list OK)
    │
    └── Circle
        ├── Deferred: no Life360 / unsupported CRUD
        └── Ready: Life360 read available (read-only contract)
```

## Non-goals (Phase 4)

- Nested feature stacks beyond shell destinations
- MOMENT.CREATE and other product commands
- Fake demo companies / Groups / Moments
