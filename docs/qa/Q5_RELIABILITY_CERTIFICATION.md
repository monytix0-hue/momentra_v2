# Q5 Reliability / Security Certification

Independent of Moment happy paths.

## Coverage

| Case | How |
|------|-----|
| Cold / warm launch | stopApp + launchApp clearState false |
| Process death | stopApp |
| Logout / login | account.sign_out |
| Double submit | tap Submit twice → `qa:verify` canonical count = 1 |
| Rapid Moment / Company switch | cert journeys |
| Hide balances | settings (when id wired) |
| PIN / biometric / auto-lock | `.maestro/android/08_security` + iOS biometric flow |
| Offline / 500 / timeout | platform tooling — document BLOCKED if unavailable |
| Deep-link invite | group invite URL filters in AndroidManifest |
| Revoked session | Firebase token refresh 401 path |

## Journey

`.maestro/cert/android/reliability/q5_reliability.yaml`  
`.maestro/cert/ios/reliability/q5_reliability.yaml`

## Status

Journey **IMPLEMENTED**. Full matrix execution **PENDING**. Double-submit proof requires `qa:verify` after the run.
