# Quick Add + widget closure (PX-6)

| Family | Quick Add | Writer | Axis refresh |
|--------|-----------|--------|--------------|
| Future | Milestone / Opportunity / Pivot / Progress / Learning | `POST .../future-items` (precision schema) | `refreshFuturePulseAxes` |
| Lifestyle | Experience / Wellbeing / Discovery / Expression / Adjust | `POST .../lifestyle-activities` | `refreshLifestylePulseAxes` |
| Relationships | Connection / Support / Shared / Investment / Interaction | `POST .../relationship-activities` | `refreshRelationshipsBondAxes` |
| Shared | Expense | `POST .../expenses` | finance snapshot (Life Ops path) |

Widget IDs and honesty rules: [`PERSONAL_FAMILY_PRECISION_FREEZE.md`](PERSONAL_FAMILY_PRECISION_FREEZE.md).
