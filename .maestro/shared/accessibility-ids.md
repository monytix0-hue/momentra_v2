# S9-QA Accessibility IDs

Canonical IDs for Maestro + TalkBack/VoiceOver.  
Android: `MaestroIds` + `testTagsAsResourceId`.  
iOS: matching `accessibilityIdentifier`.

## Auth / onboarding

| ID | Surface |
|----|---------|
| `login.screen` | Auth root |
| `login.email` | Email field |
| `login.password` | Password field |
| `login.submit` | Email sign-in |
| `login.google` | Google |
| `login.apple` | Apple (iOS) |
| `login.forgot` | Forgot password |
| `login.error` | Error text |
| `onboarding.skip` | Skip onboarding |
| `consent.gate` | Consent screen |
| `consent.continue` | Continue |

## Shell navigation

| ID | Surface |
|----|---------|
| `topbar.root` | Top bar |
| `topbar.life360` | Life360 |
| `topbar.new_moment` | Create moment |
| `topbar.profile` | Profile / account |
| `context.switcher` | Context row |
| `context.personal` | Personal |
| `context.group` | Group |
| `context.business` | Business |
| `context.circle` | Circle |
| `moment.switcher` | Moment picker |
| `moment.create` | Create moment CTA |
| `company.switcher` | Business company chip |
| `bottom.nav` | Bottom nav |
| `bottom.pulse` | Pulse |
| `bottom.moments` | Moments |
| `bottom.quickadd` | Quick Add |
| `bottom.life` | Life |
| `bottom.memory` | Memory |
| `account.sign_out` | Sign out |
| `life360.coming_soon` | Life360 Coming Soon |
| `circle.coming_soon` | Circle Coming Soon |

## Personal finance

| ID | Surface |
|----|---------|
| `personal.expense.amount` | Amount |
| `personal.expense.category` | Category chips |
| `personal.expense.account` | Paid-from |
| `personal.expense.date` | When |
| `personal.expense.more` | Details / form body |
| `personal.expense.note` | Title / note (use `MAESTRO-${MAESTRO_RUN_ID}`) |
| `personal.expense.submit` | Save |

## Personal Life Ops quick-add

| ID | Surface |
|----|---------|
| `personal.lifeops.recovery.note` | Recovery notes |
| `personal.lifeops.recovery.submit` | Save Recovery |
| `personal.lifeops.mood.note` | Mood reflection note |
| `personal.lifeops.mood.submit` | Save Reflection |
| `personal.lifeops.attention.note` | Attention notes |
| `personal.lifeops.attention.submit` | Save Focus |
| `personal.lifeops.adjust.note` | Adjustment reason |
| `personal.lifeops.adjust.submit` | Update Rhythm |

## Personal money (transfer / savings)

| ID | Surface |
|----|---------|
| `personal.money.transfer.amount` | Transfer amount |
| `personal.money.transfer.submit` | Transfer Now |
| `personal.money.savings.amount` | Savings amount |
| `personal.money.savings.submit` | Save Now |
| `personal.money.add_account` | Add account CTA / form |
| `qa.tile.transfer` | Hub Transfer tile |
| `qa.tile.savings` | Hub Savings tile |

## Group

| ID | Surface |
|----|---------|
| `group.expense.amount` | Amount |
| `group.expense.payer` | Paid by |
| `group.expense.split` | Split strategy |
| `group.expense.note` | Description |
| `group.expense.submit` | Save |
| `group.invite.create` | Mint invite CTA |
| `group.invite.code` | Invite code / path |
| `group.invite.redeem` | Redeem CTA |

## Business

| ID | Surface |
|----|---------|
| `business.expense.amount` | Amount |
| `business.expense.category` | Category |
| `business.expense.note` | Description |
| `business.expense.submit` | Save |
| `business.revenue.amount` | Revenue amount *(wire when sheet exists)* |
| `business.revenue.submit` | Revenue save *(wire when sheet exists)* |
| `business.invoice.customer` | Invoice customer *(wire when sheet exists)* |
| `business.invoice.line.add` | Add line *(wire when sheet exists)* |
| `business.invoice.submit` | Invoice save *(wire when sheet exists)* |
| `business.approval.approve` | Approve *(wire when sheet exists)* |
| `business.approval.reject` | Reject *(wire when sheet exists)* |

## Master Certification Quick Add tiles (Q0+)

| ID | Surface |
|----|---------|
| `qa.tile.expense` | Expense |
| `qa.tile.recovery` | Recovery |
| `qa.tile.mood` | Mood |
| `qa.tile.attention` | Attention |
| `qa.tile.adjust` | Adjust |
| `qa.tile.milestone` | Milestone |
| `qa.tile.opportunity` | Opportunity |
| `qa.tile.pivot` | Pivot |
| `qa.tile.progress` | Progress |
| `qa.tile.learning` | Learning |
| `qa.tile.experience` | Experience |
| `qa.tile.wellbeing` | Wellbeing |
| `qa.tile.discovery` | Discovery |
| `qa.tile.expression` | Expression |
| `qa.tile.connection` | Connection |
| `qa.tile.support` | Support |
| `qa.tile.shared_exp` | Shared Exp |
| `qa.tile.investment` | Investment |
| `qa.tile.contribute` | Group contribution |
| `qa.tile.settle` | Settlement (deferred) |
| `qa.tile.people` | People / new moment |
| `qa.tile.revenue` | Revenue |
| `qa.tile.invoice` | Invoice |
| `personal.setup.life_operations` | Life Ops setup |
| `personal.setup.future_building` | Future setup |
| `personal.setup.lifestyle` | Lifestyle setup |
| `personal.setup.relationships` | Relationships setup |
| `personal.setup.submit` | Personal setup submit |
| `group.setup.submit` | Group setup submit |
| `business.company.create` | Create company |
| `business.company.submit` | Company submit |
| `business.setup.team_operations` | Team Ops setup |
| `business.setup.business_runway` | Runway setup |
| `business.setup.business_operations` | Ops setup |
| `business.setup.submit` | Business setup submit |

Prefer `id:` in YAML over visible text. Text fallbacks allowed only as optional `when:` guards for system dialogs.

**Alias note:** keep `topbar.*` (not `top.*`) for stability with existing smoke flows.

**QA correlation (debug):** Android broadcast `com.example.momentra.QA_SET_CORRELATION` with extras `correlation_id` + `run_id`. iOS: `QaCorrelationHolder`. Backend non-prod accepts `qa-[a-z0-9-]{8,64}` correlation tokens.
