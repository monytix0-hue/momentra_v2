# Phase 5 — Empty State Matrix

Supported screens only. ✓ = implemented with real API-backed state (or explicit setup/error states).

| Context | Screen | Never Had Moment | No Active + History | Loading | Error | Notes |
|---|---|---|---|---|---|---|
| Personal | Pulse | ✓ | ✓ | ✓ | ✓ | No fake Pulse metrics |
| Personal | Moments | ✓ | ✓ | ✓ | ✓ | Primary empty experience |
| Personal | Create | ✓ | ✓ | ✓ | ✓ | Shell entry; no wizard |
| Personal | Life | ✓ | ✓ | ✓ | ✓ | First uses educational Life empty |
| Personal | Memory | ✓ | ✓ | ✓ | ✓ | History when present |
| Group | Pulse | ✓ | ✓ | ✓ | ✓ | Shared empty via context experience |
| Group | Moments | ✓ | ✓ | ✓ | ✓ | |
| Group | Create | ✓ | ✓ | ✓ | ✓ | Routes to Create tab |
| Group | Life | ✓ | ✓ | ✓ | ✓ | Same Group empty shell |
| Group | Memory | ✓ | ✓ | ✓ | ✓ | |
| Business | Pulse | ✓* | ✓ | ✓ | ✓ | *Requires company |
| Business | Moments | ✓* | ✓ | ✓ | ✓ | *Requires company |
| Business | Create | ✓* | ✓ | ✓ | ✓ | |
| Business | Life | ✓* | ✓ | ✓ | ✓ | |
| Business | Memory | ✓* | ✓ | ✓ | ✓ | |
| Business | No company | ✓ (setup) | n/a | ✓ | ✓ | Phase 4 setup — not Moment empty |
| Circle | Shell | Deferred | Deferred | ✓ | ✓ | No Circle Moment empty clone |

## Moment Switcher

| Scenario | Shown |
|---|---|
| Personal any | No |
| Group no active | No |
| Group active | Yes |
| Business no company | No |
| Business company, no active | No |
| Business company, active | Yes |
| Loading / error / 403 | No |

## Bottom navigation

Always available during empty/inactive Moment states. User is not forced into Create.
