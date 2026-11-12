# iOS-on-Mac — Restaurant QA (separate pass)

Android phone pass is done on Linux; this host has **no Xcode**. Run on Mac only.

## Commands

```bash
export MOMENTRA_API_BASE_URL='https://api.momentra.tech/'
cd momentra   # Xcode project root
xcodebuild -scheme momentra -destination 'platform=iOS Simulator,name=iPhone 16' build
# Or: open momentra.xcodeproj → scheme momentra → Simulator / device
```

Bundle: `resolvingpoint.momentra`  
API: same as Android (`https://api.momentra.tech/`)  
Moment name: **Spice Route QA iOS** (new; do not mix with Android moment)

## Walk

Follow `docs/superpowers/plans/2026-11-12-restaurant-device-qa-walkthrough.md` Tasks A–F.

| Amounts | ₹ |
|---------|---|
| Dishes | 480 / 420 / 340 / 90 / 180 |
| Purchase | **4,650** · Fresh Farm QA |
| KOT Table 12 | **1,180** kitchen · **no revenue** |
| Sale A | **1,080** · or Bill from KOT **1,180** |

KOT: Status → **Send KOT ›** and/or Pulse → **KOT**.

```text
[iOS] PASS/FAIL — dishes=5 purchase=4650 kot=no-revenue sale=<amount> notes=
```

## Carry-over from Android

- Masters create overlay needed Box parent (Android fix local/uncommitted)
- Vendor Fresh Farm QA may not persist
- Dish sellPrice may not round-trip → POS “Price needed”
- Do not invent GST when UI says Unavailable
