# Business runtime gate 2 — multi-store

Date: 2026-10-01. Host: iPhone 17 simulator (`F5F61CAD-1A93-4B08-9DC2-66555B0BF5CE`, iOS 26.5), signed in. The physical iPhone was not reinstalled and was not used for this pass. Scores below are from the simulator screen.

## 1. Executive status

**PASS WITH GAPS.**

`Momentra Multi Store QA` was created as a Growing Business and activated. Setup, the three locations, and the no-moment Moments and Life screens were scored from the display. Money, Daily, and Team moments were not created. Writes, the Life lens, Memory, location switch, Pureborn switch, relaunch, and the cash-sale refresh count were not shown. This is not a full Business PASS. Android was not run.

## 2. Test company

Name on screen: `Momentra Multi Store QA`.

Locations saved in the wizard, in order:

| Name | Area | Primary |
|---|---|---|
| Main Store | Central market | Yes |
| North Store | North area | No |
| South Store | South area | No |

Exactly one Primary badge. No duplicate names. The company id was not shown in the UI. After activation the toolbar chip showed a truncated `M...`. Switch Company listed `Momentra Multi Store QA`. Pureborn was not opened.

## 3. Setup verification

Shown on the simulator.

| Check | Result |
|---|---|
| Audience | Step 2 opened on Small shop / Retail, including sell, bill, and starter template. Growing business was selected. Those Small Shop questions and the starter template left the form. |
| Name | `Momentra Multi Store QA` was visible in the field and on the activate button. |
| Industry | Technology & Software after Growing was selected. |
| Size | Small (2-25) was the filled pill. Solo (1) and Medium (26-100) were not. |
| Logo | Optional. Label read Upload corporate logo. No file was chosen. |
| Entity | Pvt Ltd was the filled pill. LLP, Partnership, and Sole Prop were present and not selected. |
| GSTIN | Test value `22AAAAA0000A1Z5` stayed in the field. No format error appeared. |
| Currency | ₹ INR — Indian Rupee. The row has a chevron. A tap did not open a picker. |
| Financial year | Jan-Dec, Apr-Mar, Custom. Apr-Mar is the default styling. It was left unchanged. |
| Timezone | IST (UTC+5:30). Same static chevron row as currency. A picker did not open. |
| Required name | Continue with an empty name moved to Locations and showed no error. The name was filled before Activate. |
| Back | Back from Locations returned to the company form with Growing, Technology & Software, Small (2-25), Pvt Ltd, and the corporate logo label still in place. |
| Structure | Single Location, Multi-Location, and Multi-Unit were all offered. Multi-Location showed a purple outline and a selected radio. |
| Locations | All three saved. The list was still there after scrolling. Launch has Close rather than Back, so the list was not reopened from Launch. |
| Primary change | The editor has name and address only. The first saved location stays Primary. There is no control to move Primary. |
| Launch copy | “After activation, open Create to add Money, Daily Business, or Team moments.” No Small Shop template steps. Owner row for the signed-in user, scope All Locations. “4 sections configured • 0 people to invite.” Button: Activate Momentra Multi Store QA →. |
| Invite | Activation opened “Invite your team via...” with Messages and WhatsApp. Not now was not in the popover. A tap outside the popover dismissed it. |

## 4. Moment setup verification

Not completed. After the invite popover closed, Create showed Choose a Moment with Team & Work, Money & Cash Flow, Daily Business, and Project Operations partly visible at the bottom. None of those wizards was finished.

The company chip then disappeared. New, the center button, and Start Your Business Journey each opened a fresh Set Up Your Business wizard (onboarding 1/4) instead of moment creation for the company already saved. That second wizard was closed. Switching Personal → Business did not bring the chip back. Profile has no company list.

## 5. Pulse widget matrix

| Surface | Family | Widget | State | Data shown | Correct? | Action works? | Notes |
|---|---|---|---|---|---|---|---|
| Pulse | none | First-run empty | SHOWN | “See Where Your Business Money Goes”, Money Spend / Purchases tracked / Expenses, Start Tracking | Marketing empty before a company existed | Start Tracking opened setup | No Money Pulse hero. Zeros in that card were not read as a separate number by OCR. |
| Pulse | Money | Hero, Today, Needs attention, Recent, Snapshot | NOT RUN | — | — | — | No Money moment |
| Pulse | Daily | Hero, Today, Needs attention, Recent, Snapshot | NOT RUN | — | — | — | — |
| Pulse | Team | Hero, Today, Needs attention, Recent, Snapshot | NOT RUN | — | — | — | — |

## 6. Moments widget/filter matrix

| Surface | Family | Widget | State | Data shown | Correct? | Action works? | Notes |
|---|---|---|---|---|---|---|---|
| Moments | no moment | Empty marketing | SHOWN | Sample rows: You bought supplies / Today, 10:42 AM; A customer paid / Oct 14, 2024; You saved a receipt / Sep 01, 2024. Chips: Purchases, Expenses, Receipts, Activity. CTA: Add your first moment → | No. Rows are not labeled as examples. | CTA not used | Company chip was visible. See section 14. |
| Moments | Money / Daily / Team | Filters and cards | NOT RUN | — | — | — | No family moment |

## 7. Create action matrix

| Family | Action | Visible | Opens | Save succeeds | Refresh correct | Moments event | Notes |
|---|---|---|---|---|---|---|---|
| — | Choose a Moment | Yes, once, right after activation | Team & Work, Money & Cash Flow, Daily Business, Project Operations | Not saved | — | — | Catalog only. No wizard was completed. |
| Money | Revenue, Expense, Invoice, Khata | NOT RUN | — | — | — | — | |
| Daily | Spend, Vendor, Issue, Approval | NOT RUN | — | — | — | — | |
| Team | Update, Decision, Approval, secondary | NOT RUN | — | — | — | — | |

## 8. Life verification

The active Life blocks and the lens invariant were not opened.

With a company and no moment, Business · Life showed the marketing empty “See your business at a glance”, Team / Money / Work / Progress nodes, and three cards whose value is an unexplained dash: Team, Spending, Earnings. CTA: Start Your Business Journey →. It does not say Not set up.

## 9. Memory verification

Not run.

## 10. Company switching

Partial. Switch Company listed `Momentra Multi Store QA` and Add New Company while the chip was visible. Pureborn was not in that list on this signed-in simulator, so QA → Pureborn → QA was not walked. After that sheet, the chip was gone and could not be opened again from the toolbar.

## 11. Location switching

Not run on the live shell. The wizard stored Main, North, and South, and there is no primary-change control there. Company-wide totals were not on a Pulse or Life facts screen, so they were not scored.

## 12. Relaunch

Not run. The simulator was not killed and reopened after activation.

## 13. Zero / unavailable audit

| Value | Class |
|---|---|
| Life Team / Spending / Earnings dash, no moment | BUG. Unexplained dash on the empty Life screen. |
| Moments sample dates and “Today, 10:42 AM” | BUG. Sample copy presented as activity. |
| Pulse empty metric figures | Not separately read. The labels were on screen. The installed build still draws `0` in that card. |
| Currency and timezone | Display-only setup values, not finance totals. |

## 14. Known-failure retests

### Cash sale duplicate load

Not retested on iOS. No ₹1 cash sale was saved on the simulator.

Android beta-fix retest, after this pass, on `pureboen36ADUFS9403L1Z9`: one cash sale of ₹1 for Gate Checkv logged one `scoped_refresh_business` (`prefetch=false`, `force=false`) and one `pulse_tab_ready` (`elapsedMs=1030`). Company revenue moved from `1.0000` to `2.0000`. The new row is `INR 1.0000 · 1 Oct, 3:37 PM`. The SSE event used `scopeType=USER` and did not start a second Pulse load.

### No-moment sample rows

Retested on `Momentra Multi Store QA` before any family moment existed.

- Business · Moments showed the three sample rows above, with no “example” label, plus Add your first moment →.
- Business · Life showed dashes for Team, Spending, and Earnings, not Not set up.

Both match the earlier failure. Android’s empty copy already uses an empty sample list and the words Not set up. The iOS empty views still hardcoded the samples, the dash, and a Pulse `0`.

Two existing test call sites were also updated so their argument order matches the current initializers (`IdentityCacheTests`, `GroupActivityTreeTests`, `BusinessMomentsPresentationTests`). That did not get the test target to a green run.

Source now matches that Android copy:

- `BusinessNoMomentEmptyCopy` — empty sample list, absent text `Not set up`
- Moments empty hides the timeline when the list is empty
- Life empty and Pulse empty use `Not set up` instead of `-` and `0`

`BusinessNoMomentEmptyCopyTests` asserts that copy. The `momentraTests` target still fails to compile in `ShellModelTests` (`contextContent` and `moments` setters are inaccessible), so this new test was not executed. The simulator is still running the previous Debug build, so the fix was not shown on screen.

## 15. Bugs fixed

Narrow empty-state copy only, aligned with the Android `BusinessNoMomentEmptyCopy` already in the repo. No Pulse, Moments, Life, or Memory layout of an active moment was changed. The running simulator was not rebuilt, so the screen still shows the old copy.

## 16. Bugs remaining

- The installed simulator build still shows sample Moments rows and Life dashes until it is rebuilt and reopened.
- After Switch Company, the toolbar chip disappeared. New and the center button then start a new company wizard instead of moment creation. Moment setup, writes, and the later checks stopped there.
- Continue on step 2 does not require a company name. The empty-name error exists on Activate, which was not reached with an empty name.
- Currency and timezone rows do not open a picker.
- Invite popover showed Messages and WhatsApp only. Not now was not visible.
- Cash-sale double refresh was not measured.
- Primary cannot be moved in the location editor.

## 17. Android status

**NOT RUN.** This host has no Android device attached. Android empty copy already avoids the sample rows and the dash. That was not rechecked on a device.

## 18. iOS status

**PARTIAL** on the iPhone 17 simulator. The signed-in session was driven from Personal Pulse through Growing setup, activation, the post-activation Create catalog, and the no-moment Moments and Life screens.

The physical iPhone was not used.

## 19. Remaining product gaps

- Realtime: another member’s activity may require refresh. Not exercised.
- Location-scoped finance is not implemented. No finance total was on screen to mis-label.
- Money, Daily, and Team moments, their writes, Life lenses, Memory, location switch, and relaunch are still unchecked.
- The selected-company chip can disappear, and Create then offers a new company instead of the one just activated.

## 20. Final recommendation

**NO.**

Setup for one Growing multi-store company worked on the simulator, and the no-moment empty screens still present sample activity and dashes. The moment, write, Life, and Memory checks were not reached. Beta readiness needs those screens on a build that includes the empty-copy fix, plus the same checklist on Android.

## 21. iOS shell unblock gate

Not run. This host cannot build or drive the iOS simulator. The code change keeps a selected company when bootstrap omits it, and `ShellModelTests` no longer assigns private shell state. Empty Pulse, Moments, and Life already use `BusinessNoMomentEmptyCopy` (`Not set up`, no sample timeline). Rebuild a Debug app on the Mac before scoring the screen. The previous simulator session was an older build.

Classification after this code change, before the simulator gate: **PASS WITH GAPS / NOT BETA READY**.

Realtime, location-scoped finance, and B5 stay out of this pass.

Short unblock only, on the fresh build:

| Step | Expected |
|---|---|
| Create or restore the company | Company stays selected. Chip stays visible. |
| No active moment: Pulse, Moments, Life, Memory | No sample timeline as live data. Inactive Life reads Not set up, not a dash or 0. |
| Center + | Choose a Moment. Not Create Company. |
| Activate Money | Lands on Money. Chip remains. |
| Force-stop and relaunch | Company and Money restore. |

If that passes, continue Money, Daily, Team, writes, Pulse, Moments, Life, Memory, locations, company switch, and relaunch. Do not start that full matrix until the short gate passes.
