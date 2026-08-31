# S3 Group Parity Matrix

**Date:** 2026-08-26  
**Figma:** `575:7980`  
**Statuses:** PASS | PARTIAL | FAIL | API_GAP | EMPTY_SUPPORTED | FIGMA_GAP | DEFERRED | BLOCKED_ENVIRONMENT | NOT_REQUIRED

| Surface | Backend | Android | iOS | Notes |
|---------|---------|---------|-----|-------|
| Group empty | — | PASS | PASS | S1 shell; no fake balances |
| Setup ×12 / 3 families | PASS create | PASS | PASS | Prefs LOCAL_ONLY / API_GAP |
| Invite mint | PASS | PASS | PASS | |
| Invite redeem | PASS | PASS | PASS | Shell/QR wired S3 |
| Moment switch / isolation | PASS | PASS | PASS | Tests U2 ↛ Moment B |
| Group Pulse | PASS (projection/EMPTY) | PASS | PASS | Read model; no live calc |
| Group Moments | PASS activity | PASS | PASS | |
| Group Life | EMPTY/API_GAP sections | PASS honest | PASS | |
| Group Memory | EMPTY | PASS | PASS | |
| Group Activity | PASS | PASS | PASS | Moment-scoped |
| Quick Add hub | — | PASS | PASS | Registry V019 |
| Group Expense | PASS | PASS | PASS | `POST …/group-expenses` |
| Equal split | PASS | PASS | PASS | Server-authoritative |
| Percentage/Exact/Shares | PASS server | PARTIAL UI | PARTIAL UI | Server supports; clients default EQUAL |
| Expense shares + obligations | PASS | via finance facet | via finance facet | |
| Contributions | PASS mount | PASS | PASS | |
| Settlements | **API_GAP** 501 | disabled CTA | disabled CTA | Capability unmapped |
| Participant list | PASS | PASS | PASS | `participant_id` identity |
| Scoped refresh | hints | `groupTabRefreshToken` | same | No full-context reload |
| Multi-currency FX | NOT_REQUIRED | — | — | Explicit: no FX in V001–V029 |
| iOS Xcode runtime | — | — | BLOCKED_ENVIRONMENT | Source-equivalent |

## Mandatory PASS bar

| Item | Result |
|------|--------|
| Group creation | PASS |
| Membership | PASS |
| Invite mint + redeem | PASS |
| Isolation | PASS |
| Group expense + Equal + shares + obligations | PASS |
| Activity after expense | PASS |
| Pulse finance effect or honest EMPTY | PASS |
| Idempotency / auth / audit / event / outbox / hints | PASS |
| Settlements advanced | API_GAP (allowed) |
