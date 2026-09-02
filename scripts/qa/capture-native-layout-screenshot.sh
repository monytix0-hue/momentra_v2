#!/usr/bin/env bash
# Native layout audit screenshot capture.
# Usage: ./scripts/qa/capture-native-layout-screenshot.sh <wave> <screen_slug> [before|after] [apk|ios|both]
# Example: ./scripts/qa/capture-native-layout-screenshot.sh C personal_quickadd_hub after ios

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
WAVE="${1:?wave required (A|B|C|D)}"
SLUG="${2:?screen slug required (e.g. personal_pulse)}"
WHEN="${3:-after}"
PLATFORM="${4:-ios}"

OUT_DIR="$ROOT/docs/qa/screenshots/ios-native-layout/$WAVE/$SLUG"
mkdir -p "$OUT_DIR"

case "$PLATFORM" in
  ios)
    python3 -m pymobiledevice3 developer dvt screenshot --native --udid "${IOS_UDID:-00008110-00016CAA2E29401E}" "$OUT_DIR/${WHEN}.png"
    echo "iOS → $OUT_DIR/${WHEN}.png"
    ;;
  apk)
    adb -s "${ANDROID_SERIAL:-00158357G000049}" exec-out screencap -p > "$OUT_DIR/${WHEN}.png"
    echo "APK → $OUT_DIR/${WHEN}.png"
    ;;
  both)
    adb -s "${ANDROID_SERIAL:-00158357G000049}" exec-out screencap -p > "$OUT_DIR/apk_${WHEN}.png"
    python3 -m pymobiledevice3 developer dvt screenshot --native --udid "${IOS_UDID:-00008110-00016CAA2E29401E}" "$OUT_DIR/ios_${WHEN}.png"
    echo "Both → $OUT_DIR/"
    ;;
  *)
    echo "Platform must be apk, ios, or both" >&2
    exit 1
    ;;
esac
