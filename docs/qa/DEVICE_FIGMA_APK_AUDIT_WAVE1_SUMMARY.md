# Device + Figma + APK Audit — Wave 1 Summary

**Date:** 2026-09-01  
**Scope:** Shell chrome + auth (15 capture sets)  
**Devices:** Android `00158357G000049` (`com.example.momentra`) · iOS `00008110-00016CAA2E29401E` (`resolvingpoint.momentra`)  
**Figma:** [momentra](https://www.figma.com/design/TzLvwVwlPbeVB8ug1zB3GM/momentra) (`TzLvwVwlPbeVB8ug1zB3GM`)

Artifacts:
- Tri-panel PNGs: [`screenshots/device-audit/wave1/`](screenshots/device-audit/wave1/)
- Scored matrix: [`DEVICE_FIGMA_APK_AUDIT_WAVE1.csv`](DEVICE_FIGMA_APK_AUDIT_WAVE1.csv)
- Capture helper: [`scripts/qa/capture-wave1-screenshot.sh`](../../scripts/qa/capture-wave1-screenshot.sh)

---

## Executive summary

Wave 1 triangulation is **complete for APK + Figma evidence** on all 15 screens. **iOS device evidence is partial but improved (2026-09-01 retry)**: USB + `pymobiledevice3 dvt screenshot` work reliably again; fresh-install auth captures landed for **Splash**, **Product onboarding**, and **Cinematic onboarding**; Personal shell chrome refreshed. Firebase session still blocks **Login** and **Consent** without sign-out; HID context-tab taps remain unreliable (Group/Business iOS PNGs still show Personal).

**First device-backed dual-authority MATCH:** Bottom Nav (`14_bottom_nav`) — APK vs iOS custom bar + accent FAB aligns; Figma authority is PARTIAL due to **Life vs Live** label conflict.

---

## Score counts (overall status)

| Authority | MATCH | PARTIAL | GAP | UNVERIFIED | INTENTIONAL |
|-----------|-------|---------|-----|------------|-------------|
| **APK vs iOS** (`overall_apk_vs_ios`) | 2 | 6 | 0 | 7 | 0 |
| **Figma vs iOS** (`overall_figma_vs_ios`) | 7 | 2 | 0 | 6 | 0 |
| **APK vs Figma conflict flag** | — | 6 rows PARTIAL | — | — | 2 rows INTENTIONAL (Consent, App Lock FIGMA_GAP); 1 CONFLICT (Bottom Nav label) |

### Screens earning MATCH on APK authority (vs iOS)

| Screen | Notes |
|--------|-------|
| `08_topbar_personal` | Logo, 360, New, Refer, avatar — custom iOS assets post-P0 |
| `14_bottom_nav` | Custom icons + center FAB; only Figma label drift |

---

## Figma node register

| Screen | Node | Status |
|--------|------|--------|
| Splash | `353:6500` | Valid |
| Login | `353:6720` | Valid |
| Cinematic onboarding | `353:6518` | Valid |
| Product onboarding | `848:11634` | Valid |
| App shell (Personal) | `353:317` | Valid |
| TopBar Personal | `763:12256` | Valid |
| TopBar Group | `772:11972` | Valid |
| TopBar Business (no co.) | `1522:12255` | Valid |
| TopBar Business (activated) | `692:34971` | Valid |
| Context Switcher | `763:12897` | Valid |
| Moment Switcher | `482:19907` | Valid |
| Bottom Nav | `908:6860` | Valid (symbol; `501:6367` invalid in MCP) |
| **Consent Gate** | — | **FIGMA_GAP** (minimal APK gate; no dedicated frame) |
| **App Lock** | — | **FIGMA_GAP** (`1511:16014` is Account settings, not gate UI) |

---

## Wiring spot-check (shell controls)

Verified on **Android device**; **iOS** confirmed via code parity with [`AppShellScreen.kt`](../../apk/app/src/main/java/com/example/momentra/ui/shell/AppShellScreen.kt) and [`AppShellView.swift`](../../momentra/momentra/Shell/AppShellView.swift).

| Control | Expected | Android | iOS | Device result |
|---------|----------|---------|-----|---------------|
| Context tabs | Personal / Group / Business / Circle | `selectContext` | `model.selectContext` | **PASS** — Group/Business tabs switch chrome on APK |
| Circle tab | Coming Soon body | `CircleComingSoonContent` | `CircleComingSoonView` | **PASS** (code); not tapped on device |
| Bottom tabs | Pulse / Moments / FAB / Life / Memory | `ShellBottomNavigation` | `ShellBottomNavigationView` | **PASS** — visible on both captures |
| TopBar + | Create flow | `openNewMoment` | `openNewMoment` | **PASS** (code) |
| TopBar 360 | Life360 sheet | `Life360ComingSoon` | `Life360ComingSoonView` | **PASS** (code) |
| QR (Group/Business) | Join scanner | `GroupJoinQrScanner` | `showJoinQrScanner` | **PASS** — QR visible on APK Group/Business topbars |
| Moment switcher | When moments exist | `MomentSwitcher` | `MomentSwitcherView` + `showMomentSwitcher` | **PASS** — visible on APK Personal/Group/Business |
| Company chip | Business + company selected | `CompanySwitcher` | `companyChip` menu | **PASS** — APK company menu captured |

**Wiring gaps (visual, not routing):**
- iOS chrome still uses **SF Symbols** for chevrons (`chevron.up`/`chevron.down`) and moment settings (`gearshape.fill`) where APK uses vector drawables — routing works; icon authority differs.

---

## Conflict register (APK ≠ Figma)

| Screen | Conflict | Recommended iOS target | Priority |
|--------|----------|------------------------|----------|
| Bottom Nav | Figma label **Live**; APK/iOS use **Life** | Keep **Life** (product naming) | Document only |
| Cinematic onboarding | APK purple cinematic palette vs Figma warm gradient | Align APK to Figma or document intentional brand variant | P2 |
| Product onboarding | APK first-stage order + palette | Match Figma carousel structure on iOS | P1 |
| Consent / App Lock | No Figma frames | APK minimal gates; add Figma frames or mark permanent FIGMA_GAP | P1 |

**Consent Gate (#3) update (2026-09-01):** APK capture refreshed — device shows `Privacy & consent` + `Continue` on dark brand canvas ([`consent_gate/apk.png`](screenshots/device-audit/wave1/consent_gate/apk.png)). iOS capture still shows product onboarding carousel (HID navigation unreliable); manual skip-to-consent needed for tri-panel parity.
| Moment Switcher | Figma card pills vs APK implementation | Already PARTIAL in Phase 4 mapping | P1 |

---

## Capture limitations (honest register)

| Issue | Impact |
|-------|--------|
| iOS Firebase session persists after reinstall | Auth screens need in-app sign-out or fresh install without Keychain |
| iOS HID service intermittent | `universal-hid-service` fails to start unless tunnel is healthy; context-tab taps did not switch shell — manual tab switch required |
| iOS `dvt screenshot` | Works via `--native --udid 00008110-00016CAA2E29401E` (confirmed ~117KB–742KB PNGs) |
| Android App Lock gate | `app_lock/apk.png` is PIN **setup sheet** (123456 entered); gate needs Enable PIN + background relaunch |
| Android splash timing | Early screencap shows loader; use product onboarding frame or ~1.5s delay |
| Consent / App Lock Figma | No nodes — `screenshot_figma` empty (FIGMA_GAP) |

### Manual capture checklist

```bash
./scripts/qa/capture-wave1-screenshot.sh consent_gate ios   # after skip-to-consent
./scripts/qa/capture-wave1-screenshot.sh topbar_group ios   # tap Group tab first
./scripts/qa/capture-wave1-screenshot.sh app_lock both      # after PIN enabled + relaunch
```

---

## Top remediation priorities

### P0 — Done / verified
- **Bottom Nav:** iOS custom bar + FAB matches APK on device (**MATCH**).

### P1 — Shell chrome + thin auth
1. **iOS Group/Business topbar device verification** — manual context switch + re-capture (HID coords need calibration).
2. **Product onboarding + Consent iOS layout** — bring iOS views to APK section depth.
3. **App Lock** — enable PIN on both devices; capture gate; add Figma frame or accept FIGMA_GAP.
4. **Moment Switcher / Company Switcher** — replace SF Symbol chevrons with APK vector assets on iOS.

### P2 — Polish
1. Cinematic onboarding color alignment (APK vs Figma).
2. Splash capture script: delay screencap until branded overlay (avoid Android robot frame).
3. iOS auth re-capture after explicit sign-out flow.

---

## Hypothesis check (from plan)

| Hypothesis | Result |
|------------|--------|
| Bottom Nav likely MATCH | **Confirmed** on APK + iOS device |
| TopBar / Context / Moment Switcher PARTIAL (SF Symbols) | **Confirmed** — APK vectors vs iOS SF on chevrons/settings |
| Consent / App Lock / Product onboarding thin iOS | **Confirmed** (source + APK device); iOS UNVERIFIED on device |
| Cinematic APK/Figma color divergence | **Confirmed** PARTIAL |
| Circle → Coming Soon wiring | **Confirmed** in code (not device-tapped) |

---

## Next: Wave 2

Repeat the same template for P1 backlog (~20 populated/empty body screens) per [`IOS_APK_DESIGN_PARITY_MATRIX.csv`](IOS_APK_DESIGN_PARITY_MATRIX.csv) P1 rows. Require iOS sign-out or test account for auth re-verification.
