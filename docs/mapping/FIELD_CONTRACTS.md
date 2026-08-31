# Field-level mapping contracts

Every writable Figma form must resolve each field before live APK wiring.

| Screen | Figma field | API field | Type | Required | Validation | Canonical target |
|---|---|---|---|---|---|---|
| Moment Create | Title | `title` | string | yes | 1–120 chars | `core.moment.title` |
| Moment Create | Description | `description` | string | no | 2000 max | `core.moment.description` |
| Moment Create | Start | `startAt` | ISO-8601 | no | domain rules | `core.moment.start_at` |
| Moment Create | End | `endAt` | ISO-8601 | no | >= startAt | `core.moment.end_at` |
| Moment Create | Custom type label | `customTypeLabel` | string | no | 120 max | `core.moment.custom_type_label` |
| Moment Create | Type | `momentTypeCode` | string | yes | V018 code | `core.moment_type.code` |
| Trip setup | Destination | `destination` | string | trip only | 200 max | `collaboration.shared_experience_context` |
| Expense / Master Expense | Amount | `amount` | decimal string | yes | > 0 | `finance.expense.amount` |
| Expense | Currency | `currencyCode` | string | yes | ISO 4217 | `finance.expense.currency_code` |
| Expense | Description | `description` | string | no | 500 max | `finance.expense.description` |
| Goal Quick Add | Title | `title` | string | yes | 1–200 | `work.goal.title` |
| Milestone | Title | `title` | string | yes | 1–200 | `work.milestone.title` |
| Milestone | Goal | `goalId` | uuid | yes | must exist | `work.milestone.goal_id` |
| Life observation | Signal | `observationType` | enum | yes | RECOVERY/MOOD/RHYTHM/WELLBEING | `personal.life_operation_observation.observation_type` |
| Life observation | Value | `numericValue` / `textValue` | number/string | one required | | observation columns |
| Company | Legal name | `legalName` | string | yes | | `business.company.legal_name` |
| Company | Display name | `displayName` | string | yes | | `business.company.display_name` |
| Location | Name | `name` | string | yes | | `business.company_location.name` |
| Location | Address | `addressText` | string | no | | `business.company_location.address_text` |
| Location | Timezone | `timezone` | string | no | IANA | `business.company_location.timezone` |
| Poll | Question | `question` | string | yes | | `shared.poll.question` |
| Poll option | Text | `optionText` | string | yes | | `shared.poll_option.option_text` |
| Device register | Token | `pushToken` | string | yes | | `platform.user_device.push_token` |
| Media upload | Content type | `contentType` | string | yes | allow-list | upload intent |

**Schema gap rule:** if no canonical column exists, mark `SCHEMA_GAP` and add forward migration — never ad-hoc JSON.
