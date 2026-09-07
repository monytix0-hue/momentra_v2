# android / 03_group

S9-QA flows for this product area. Prefer `id:` selectors from `shared/accessibility-ids.md`.

## Finance split coverage

| Flow | Strategy / check |
|------|------------------|
| `critical_expense_equal.yaml` | EQUAL create + member visibility |
| `critical_expense_percentage.yaml` | PERCENTAGE create + pulse |
| `critical_expense_exact.yaml` | EXACT/Custom create + pulse |
| `critical_moments_partitioned.yaml` | Moments shows Total spent + Your share (no Per-person split) |
