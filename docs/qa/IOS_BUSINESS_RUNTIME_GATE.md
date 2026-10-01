# iOS Business runtime gate

Date: 2026-10-01. iOS only. No Pulse, Moments, Life, or Memory layout changes.

**Result: BLOCKED. The unified Business experience stays a pass candidate.** Android is not part of this note.

The loop was not walked. None of the ten checks were scored from a screen.

## Fixtures

`.maestro/.env.maestro.local` has values for `QA_IOS_BUSINESS_OWNER`, `QA_IOS_BUSINESS_MEMBER`, `QA_IOS_BUSINESS_OUTSIDER`, `QA_BUSINESS_OWNER`, `QA_BUSINESS_MEMBER`, and `QA_MULTI_CONTEXT`.

Signing those accounts in with the iOS Firebase project (`momentra-v2`) returned `INVALID_LOGIN_CREDENTIALS`. The same failure happened with the Android client key. No company, moment, location, or role inventory could be read.

The seed contract in `backend/typescript/scripts/qa/seed-maestro-fixtures.ts` also does not match this gate, even after a fresh seed:

- iOS Company A is created with a `MEMBER`, not an `OBSERVER`.
- iOS Company A and Company B are created with no business moments.
- There is no small-shop company and no growing-business company.
- There is no second location.
- There is no Money, Daily, and Team set on one company.

Simulators are installed (iPhone 17 family, iOS 26.5). None were booted. No signed-in session existed to drive.

## Loop

Not run. Company switch, Pulse, Create/save, Moments, Life, Memory, moment switch, company switch, and relaunch were not shown.

## Checks

| Check | Score | Why |
|---|---|---|
| Company switch | NOT RUN | No signed-in company B |
| Moment switch | NOT RUN | No two families on screen |
| Cold Money Pulse | NOT RUN | Money Pulse was not opened |
| Other member | NOT RUN | Second member could not sign in |
| Observer | NOT RUN | No observer membership |
| Khata once | NOT RUN | No Khata save |
| Life company-wide | NOT RUN | Life was not opened |
| Memory lens | NOT RUN | Memory was not opened |
| Empty states | NOT RUN | No empty or failed facet was shown |
| Placeholders | NOT RUN | Production paths were not opened |

No product defect was filed. A fail is a wrong screen. These checks never reached a screen.

## Unblock

Refresh the iOS QA passwords (or re-seed against a non-production database with `QA_FIXTURES_ENABLED=true`), then add the missing rows before the loop: observer membership, small shop and growing audiences, Money plus Daily plus Team on one company, a second company, and two locations. Boot an iOS simulator, sign in, and walk the loop once per row. Score only what is on screen.
