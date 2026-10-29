#!/usr/bin/env bash
# Wave 1 device screenshot capture — APK (adb) + iOS (pymobiledevice3).
# Usage: ./scripts/qa/capture-wave1-screenshot.sh <screen_slug> [apk|ios|both]
# Example: ./scripts/qa/capture-wave1-screenshot.sh bottom_nav both
#
# Prerequisite: navigate both devices to the target screen manually before running.
# Does NOT automate navigation (UI audit, not Maestro).

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
ANDROID_SERIAL="${ANDROID_SERIAL:-00158357G000049}"
IOS_UDID="${IOS_UDID:-00008110-00016CAA2E29401E}"
SLUG="${1:?screen slug required (e.g. login, bottom_nav)}"
PLATFORM="${2:-both}"

OUT_DIR="$ROOT/docs/qa/screenshots/device-audit/wave1/$SLUG"
mkdir -p "$OUT_DIR"

capture_apk() {
  adb -s "$ANDROID_SERIAL" exec-out screencap -p > "$OUT_DIR/apk.png"
  echo "APK → $OUT_DIR/apk.png"
}

capture_ios() {
  python3 -m pymobiledevice3 developer dvt screenshot --native --udid "$IOS_UDID" "$OUT_DIR/ios.png"
  echo "iOS → $OUT_DIR/ios.png"
}

case "$PLATFORM" in
  apk) capture_apk ;;
  ios) capture_ios ;;
  both)
    capture_apk
    capture_ios
    ;;
  *)
    echo "Platform must be apk, ios, or both" >&2
    exit 1
    ;;
esac
