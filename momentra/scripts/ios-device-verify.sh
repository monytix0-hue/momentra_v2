#!/bin/bash
# Run on Mac at 192.168.68.107 with iPhone connected.
# Usage: ./scripts/ios-device-verify.sh [/path/to/momentra_v2]

set -euo pipefail
ROOT="${1:-$HOME/momentra_v2}"
APP="$ROOT/momentra"
SCHEME="momentra"
BUNDLE_ID="com.example.momentra"

cd "$APP"

DEVICE_ID="$(xcrun xctrace list devices 2>/dev/null | awk -F'[()]' '/iPhone/{print $(NF-1); exit}')"
if [[ -z "${DEVICE_ID:-}" ]]; then
  DEVICE_ID="$(xcrun devicectl list devices 2>/dev/null | awk '/iPhone/{print $NF; exit}')"
fi

echo "Using device: ${DEVICE_ID:-<none>}"
if [[ -z "${DEVICE_ID:-}" ]]; then
  echo "No iPhone detected. Plug in / trust the Mac and retry."
  exit 1
fi

DERIVED="$APP/build/DerivedData-agent"
xcodebuild \
  -project momentra.xcodeproj \
  -scheme "$SCHEME" \
  -configuration Debug \
  -destination "id=$DEVICE_ID" \
  -derivedDataPath "$DERIVED" \
  build

APP_PATH="$(find "$DERIVED/Build/Products" -name '*.app' | head -1)"
echo "Built: $APP_PATH"

xcrun devicectl device install app --device "$DEVICE_ID" "$APP_PATH"
xcrun devicectl device process launch --device "$DEVICE_ID" "$BUNDLE_ID" || true
echo "Install + launch attempted for $BUNDLE_ID"
