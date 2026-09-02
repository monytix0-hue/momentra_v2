# iOS native layout audit screenshots

Before/after captures for layout-only remediation (icons and artwork unchanged).

## Structure

```
ios-native-layout/
  A/   # Shell chrome
  B/   # Auth / gates
  C/   # P1 body screens
  D/   # Remaining backlog
  {wave}/{screen_slug}/before.png
  {wave}/{screen_slug}/after.png
```

## Capture

Navigate device to target screen, then:

```bash
./scripts/qa/capture-native-layout-screenshot.sh C personal_quickadd_hub after ios
```

Wave A before shots may reuse [`device-audit/wave1/`](../device-audit/wave1/) captures.

## Artifacts

- [`IOS_NATIVE_LAYOUT_AUDIT.csv`](../../IOS_NATIVE_LAYOUT_AUDIT.csv)
- [`IOS_NATIVE_LAYOUT_AUDIT_SUMMARY.md`](../../IOS_NATIVE_LAYOUT_AUDIT_SUMMARY.md)
