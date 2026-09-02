# iOS vs APK parity screenshots (optional)

Paired captures for **manual UI audit confirmation**. APK is the visual reference.

## Audit method

The primary audit is **source-level UI comparison** (not E2E tests):

- [`IOS_APK_DESIGN_PARITY_MATRIX.csv`](../IOS_APK_DESIGN_PARITY_MATRIX.csv) — 78 screens scored on layout, colors, typography, spacing, icons, empty states, hero, animation
- [`IOS_APK_DESIGN_PARITY_SUMMARY.md`](../IOS_APK_DESIGN_PARITY_SUMMARY.md) — executive summary + P1 backlog
- [`IOS_APK_THEME_TOKEN_DIFF.md`](../IOS_APK_THEME_TOKEN_DIFF.md) — shell + family theme hex pairing
- Regenerator: `npx tsx scripts/qa/build-ios-apk-parity-matrix.ts`

Screenshots in this folder are **optional evidence** when you want to visually confirm a matrix row. Do **not** use Maestro flows for this audit.

## Correct apps

| Platform | Repo | Package / bundle |
|----------|------|------------------|
| Android (reference) | `apk/` | `com.example.momentra` |
| iOS | `momentra/` | `resolvingpoint.momentra` |

Do not compare against other Momentra installs (e.g. `MagnatePoint.Momentra`).

## Manual capture (if needed)

1. Open the same account/context/tab on both devices.
2. Save PNGs using matrix `screenshot_apk` / `screenshot_ios` column paths.
3. Update matrix `gap_notes` if visual diff differs from source scores.
