# Wave 1 — Device + Figma + APK audit captures

Tri-panel evidence for shell chrome and auth screens.

| Platform | App |
|----------|-----|
| Android | `com.example.momentra` |
| iOS | `resolvingpoint.momentra` |

## Figma nodes (file `TzLvwVwlPbeVB8ug1zB3GM`)

| Folder slug | Figma node |
|-------------|------------|
| splash | `353:6500` |
| login | `353:6720` |
| consent_gate | FIGMA_GAP |
| cinematic_onboarding | `353:6518` |
| product_onboarding | `848:11634` |
| app_lock | FIGMA_GAP |
| app_shell_personal | `353:317` |
| topbar_personal | `763:12256` |
| topbar_group | `772:11972` |
| topbar_business_setup | `1522:12255` |
| topbar_business_activated | `692:34971` |
| context_switcher | `763:12897` |
| moment_switcher | `482:19907` |
| bottom_nav | `908:6860` (symbol; `501:6367` invalid in MCP) |
| company_switcher | business chip row (`692:34971` context) |

## Capture

```bash
# Navigate device to target screen, then:
./scripts/qa/capture-wave1-screenshot.sh <screen_slug> both
```

Each folder holds `apk.png`, `ios.png`, and `figma.png` where captured.

**Known gaps (manual capture):**

| Screen | Action |
|--------|--------|
| `consent_gate` (iOS) | Fresh install → Skip product → Skip cinematic → `./scripts/qa/capture-wave1-screenshot.sh consent_gate ios` |
| `login` (iOS) | Continue past consent → capture login |
| `app_lock` (both) | Account → App Security → Enable PIN → background app → relaunch → capture gate |
| `topbar_group` / `topbar_business_*` (iOS) | Tap Group/Business context tabs manually → capture script |
| `splash` (APK) | Launch app; screencap at ~1.5s for branded overlay (not OS loader) |

iOS HID context-tab taps unreliable on iPhone 14 Plus — use manual navigation before running the capture script.

**iOS retry (2026-09-01):** Device reachable; `pymobiledevice3 developer dvt screenshot --native` confirmed. Verified iOS PNGs: `splash`, `product_onboarding`, `cinematic_onboarding`, Personal shell set. Still manual: `consent_gate`, `login`, `topbar_group`, `topbar_business_*`, `app_lock`.

## Audit artifacts

- [`DEVICE_FIGMA_APK_AUDIT_WAVE1.csv`](../../DEVICE_FIGMA_APK_AUDIT_WAVE1.csv)
- [`DEVICE_FIGMA_APK_AUDIT_WAVE1_SUMMARY.md`](../../DEVICE_FIGMA_APK_AUDIT_WAVE1_SUMMARY.md)
