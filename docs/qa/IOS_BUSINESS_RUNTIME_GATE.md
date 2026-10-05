# iOS Business runtime gate

Date: 2026-10-01. iOS only. No Pulse, Moments, Life, or Memory layout changes.

**Result: BLOCKED. The unified Business experience stays a pass candidate.** Android is not part of this note.

The signed-in phone was opened. The company-switch loop was not walked, so the ten checks stay unscored.

## What the phone showed

Santosh’s iPhone (iOS 26.2), already signed in. Momentra 2.0 (2) was in the foreground. The session was not signed out and the app was not reinstalled.

- Personal Pulse opened first: “Good morning”, day score 52, “Nothing logged yet today.” A recent row read “Pureborn store sale”.
- After a relaunch, the context was Business · Pulse. The toolbar had QR, 360, New, Alerts, Refer, and the profile initials. There was no company chip, so no company was selected.
- That Pulse is the first-run empty screen: “See Where Your Business Money Goes”, Money Spend / Purchases tracked / Expenses all 0, and “Start Tracking”. It did not show another company’s cash or events.

No Money, Daily, or Team moment was on screen. Moments, Life, Memory, Khata, a second company, a second member, and an observer were not opened.

The phone could not be tapped through. iOS 26.2 does not expose the developer touch service used to inject taps. Accessibility focus reached “Open profile” and did not open it. Maestro saw the phone and installed `maestro-driver-iosUITests-Runner`, then stopped with “iOS driver not ready in time.”

## Fixtures

The QA emails in `.maestro/.env.maestro.local` still return `INVALID_LOGIN_CREDENTIALS` on the iOS Firebase project. They were not used on the phone.

The seed script still does not match this gate: no observer on the iOS company, no business moments, no small-shop and growing pair, no second location, no Money plus Daily plus Team set.

## Checks

| Check | Score | Why |
|---|---|---|
| Company switch | NOT RUN | No company chip and no second company on screen |
| Moment switch | NOT RUN | No two families on screen |
| Cold Money Pulse | NOT RUN | No Money moment was opened |
| Other member | NOT RUN | A second member was not signed in |
| Observer | NOT RUN | No observer path was opened |
| Khata once | NOT RUN | Khata was not saved |
| Life company-wide | NOT RUN | Life was not opened |
| Memory lens | NOT RUN | Memory was not opened |
| Empty states | NOT RUN | The no-company Pulse empty screen showed zeros and “Start Tracking”, not another company’s numbers. A company with zero cash, no events, or a failed facet was not opened |
| Placeholders | NOT RUN | Active company Pulse, Life, and Memory were not opened. The empty screen did not show a health ring, fake department workload, or “viewers cannot write” |

No product defect was filed. A fail is a wrong screen. These checks did not reach the screens they score.

## Unblock

On this signed-in account, select or create a company that has Money, Daily, and Team moments, a second company, two locations, a second member, and an observer. Then walk company switch, Pulse, save, Moments, Life, Memory, moment switch, company switch, and relaunch once per row. Score only what is on screen.
