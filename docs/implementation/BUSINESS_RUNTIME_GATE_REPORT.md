# Business full-device runtime gate

Date: 2026-10-01.

Classification: **not promoted**. B1 Pulse + Create, B2 Moments, B3 Life, and B4 Memory stay **PASS CANDIDATE**.

iOS is `BLOCKED_ENVIRONMENT`. This host has no Xcode. Android was run on a physical phone (Nothing A059, `00158357G000049`) with the current debug APK installed over the existing signed-in session. No product code was changed.

The signed-in company with moments is `pureboen36ADUFS9403L1Z9`. It has two active moments, Money & Cash Flow and Daily Business, and no Team moment. The second company, Pureborn Ops, has no moment. The session can write, so it is not an observer. One user was signed in. A location list was not on screen.

A ₹1 cash sale was saved on Money for customer `Gate Checkv`, which this run created. That is live company data.

## Android

| Check | Result | What the phone showed |
| --- | --- | --- |
| Company switch settles without the previous company | PASS | After the switch, Pureborn Ops replaced the first company. The cash sale, revenue `1.0000`, and `Gate Checkv` were gone. Switching back restored Daily Business Life and the cash sale. The earliest dump, about 400ms after the tap, was already Pureborn Ops. |
| Moment switch does not keep the previous family feed | PASS | Daily Pulse recent activity and Daily Moments no longer showed the Money cash sale. Daily Moments showed Spend, Issues, Updates, and Approvals, and “Nothing recorded yet”. |
| Cold-start Money Pulse cash, without opening Life | PASS | Before any Life visit, Money Pulse said “No figures yet.” and did not invent a cash balance. After relaunch, still without opening Life, Money Pulse showed Revenue `1.0000` and Outflow `0`. Available cash stayed omitted. |
| Another member’s event after refresh | BLOCKED | Only one signed-in user. Live cross-user updates were not required. |
| Observer cannot write | BLOCKED | This session saved a cash sale, so it is not an observer. |
| Khata save refreshes Pulse once; adding a party does not | FAIL | Adding `Gate Checkv` wrote no `pulse_tab_ready` log. The ₹1 cash sale logged one `scoped_refresh_business` and two `pulse_tab_ready` events (`elapsedMs=476` and `elapsedMs=507`). |
| Life is company-wide from Money and Daily | PASS | Both openings showed Money “Early”, Daily Business “Not set up”, Team “Not set up”, “1 recent company event is from this week.”, nothing needing attention, nothing marked as working, and Company totals Revenue `1.0000` / Expenses `0.0000`. Money opened the Money movement tab. Daily opened Activity, which listed the cash sale. |
| Memory is company-wide; lens does not change the other cards | PASS | Money and Daily both showed period “Company memory”, “Nothing saved yet.”, no pattern card, no Then → Now, “No success memories yet.”, and “No risk memories yet.” Worth remembering stayed empty because nothing is saved, so the chip had no rows to filter. |
| Empty, zero, and unavailable stay honest on the active company | PASS | Active Life used “Not set up”. Active Memory used “Nothing saved yet.” Outflow and expenses showed `0` / `0.0000`. Available cash was omitted. No accuracy line and no `items / 3`. |
| Removed placeholder cards stay off the active screens | PASS | Pattern network, playbook, wisdom, knowledge journey, and “Strong & Growing” did not appear on Pulse, Moments, Life, or Memory for the company that has moments. |
| No-moment company empty catalog | FAIL | Pureborn Ops Pulse, Moments, Life, and Memory are the setup catalog. Moments shows sample rows: “You bought supplies”, “A customer paid”, “You saved a receipt”. Life Progress shows a dash. These are not live company events. |
| Small shop | PASS | Money’s primary actions were Khata, Log Revenue, and Shop expense. |
| Growing business | BLOCKED | No growing company was on this phone. |
| Team moment | BLOCKED | No Team moment. Life still showed Team as “Not set up”. |
| Two companies | PASS | Covered by the company-switch check. |
| Several moments in one company | PASS | Money and Daily. |
| Several locations | BLOCKED | No location list was opened. Location-scoped totals were not part of this gate. |
| Relaunch | PASS | Force-stop returned to Business, `pureboen36ADUFS9403L1Z9`, Daily Business Pulse, with Vendors `1` and no Pureborn sample rows. |

## iOS

| Check | Result | Notes |
| --- | --- | --- |
| Full company loop | BLOCKED_ENVIRONMENT | No Xcode or iOS device on this Windows host. |

## Follow-up, not fixed here

- A cash-sale save loaded Pulse twice. Adding a party did not.
- A company with no moment still shows sample Moments rows and a Life progress dash.

`REALTIME_GAP` and location-scoped totals were not treated as blockers and were not changed.
