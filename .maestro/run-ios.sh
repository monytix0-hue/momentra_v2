#!/usr/bin/env bash
# Run Momentra Maestro flows on iOS Simulator or a connected physical iPhone.
#
# Usage:
#   ./.maestro/run-ios.sh [flow.yaml]
#   MAESTRO_TARGET=device ./.maestro/run-ios.sh
#   MAESTRO_TARGET=simulator ./.maestro/run-ios.sh .maestro/flows/02_email_login.yaml
#
# Defaults: prefer a connected physical iPhone when one is present; else Simulator.
#
# Physical device needs:
#   - Apple team (APPLE_TEAM_ID, default TY9S2C44WR)
#   - brew install libimobiledevice  (iproxy USB-forwards Maestro's driver port)
#   - Developer Mode on, device unlocked / trusted
#   - Mac Keychain may prompt for your macOS password when Xcode signs the XCTest driver
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
export JAVA_HOME="${JAVA_HOME:-$HOME/.jdks/jdk-21.0.12.1+1/Contents/Home}"
export PATH="/usr/local/bin:/opt/homebrew/bin:$JAVA_HOME/bin:$HOME/.maestro/bin:$PATH"
export MAESTRO_CLI_NO_ANALYTICS="${MAESTRO_CLI_NO_ANALYTICS:-true}"
export MAESTRO_CLI_ANALYSIS_NOTIFICATION_DISABLED=true
export MAESTRO_DRIVER_STARTUP_TIMEOUT="${MAESTRO_DRIVER_STARTUP_TIMEOUT:-180000}"

DERIVED_ROOT="${DERIVED_ROOT:-$HOME/Library/Developer/Xcode/DerivedData/momentra-afcfwpbbsgmhvbgkxqovavvbmkno}"
APPLE_TEAM_ID="${APPLE_TEAM_ID:-TY9S2C44WR}"

IPROXY_PID=""
IPROXY_WATCH_PID=""

cleanup() {
  if [[ -n "${IPROXY_WATCH_PID}" ]] && kill -0 "$IPROXY_WATCH_PID" 2>/dev/null; then
    kill "$IPROXY_WATCH_PID" 2>/dev/null || true
  fi
  if [[ -n "${IPROXY_PID}" ]]; then
    kill "$IPROXY_PID" 2>/dev/null || true
    wait "$IPROXY_PID" 2>/dev/null || true
  fi
  pkill -f "iproxy .* -u ${UDID:-deadbeef}" 2>/dev/null || true
  [[ -n "${DEVICE_FLOW_TMP:-}" && -f "${DEVICE_FLOW_TMP:-}" ]] && rm -f "$DEVICE_FLOW_TMP"
}
trap cleanup EXIT

# shellcheck disable=SC1091
source "$ROOT/.maestro/load-maestro-env.sh"
load_maestro_env "$ROOT/.maestro/.env.maestro.local" "$(date +%Y%m%d%H%M%S)"

echo "==> Java: $(java -version 2>&1 | head -1)"
echo "==> Maestro: $(maestro --version 2>&1 | tail -1)"

detect_physical_udid() {
  python3 - <<'PY'
import re, subprocess
out = subprocess.check_output(["xcrun", "xctrace", "list", "devices"], text=True, stderr=subprocess.STDOUT)
in_offline = False
for line in out.splitlines():
    if line.strip().startswith("== Devices Offline"):
        in_offline = True
        continue
    if line.strip().startswith("== Simulators"):
        break
    if in_offline or "Simulator" in line:
        continue
    m = re.search(r"\(([0-9A-Fa-f-]{25,})\)\s*$", line)
    if m and "MacBook" not in line and "Watch" not in line:
        print(m.group(1))
        raise SystemExit
PY
}

detect_simulator_udid() {
  python3 - <<'PY'
import json, subprocess
d = json.loads(subprocess.check_output(["xcrun", "simctl", "list", "devices", "available", "-j"]))
for prefer in ("iPhone 17", "iPhone 17 Pro"):
    for devices in d.get("devices", {}).values():
        for dev in devices:
            if dev.get("isAvailable") and dev.get("name") == prefer:
                print(dev["udid"]); raise SystemExit
for devices in d.get("devices", {}).values():
    for dev in devices:
        if dev.get("isAvailable") and "iPhone" in (dev.get("name") or ""):
            print(dev["udid"]); raise SystemExit
PY
}

is_hardware_udid() {
  local id="$1"
  [[ ${#id} -ne 36 ]] || [[ "$id" == 0000* ]]
}

ensure_iphoneos_driver() {
  local marker="$HOME/.maestro/maestro-iphoneos-driver-build/driver-iphoneos/Build/Products/Debug-iphoneos/maestro-driver-iosUITests-Runner.app"
  if [[ -d "$marker" ]]; then
    echo "==> iOS device driver already built"
    return 0
  fi
  echo "==> Building Maestro iOS device driver (team $APPLE_TEAM_ID)..."
  local jar="$HOME/.maestro/lib/maestro-cli-2.9.0.jar"
  local patch_lib="$HOME/.maestro/ios-driver-patch/MaestroDriverLib"
  if [[ -f "$jar" && -f "$patch_lib/Info.plist" ]]; then
    if ! jar tf "$jar" 2>/dev/null | grep -q 'driver/ios/MaestroDriverLib/Info.plist'; then
      echo "==> Patching Maestro CLI jar with MaestroDriverLib (2.9.0 packaging gap)"
      (
        cd "$HOME/.maestro/ios-driver-patch"
        mkdir -p driver/ios
        rm -rf driver/ios/MaestroDriverLib
        cp -R MaestroDriverLib driver/ios/
        jar uf "$jar" driver/ios/MaestroDriverLib
      )
    fi
  fi
  maestro driver-setup --apple-team-id="$APPLE_TEAM_ID"
}

# Maestro picks a random localhost port per run (e.g. 53094). The XCTest HTTP server
# listens on device loopback at the same port — iproxy must bridge Mac:PORT → device:PORT.
watch_maestro_driver_port() {
  local udid="$1"
  local report_dir="$2"
  local active=""
  while true; do
    local port=""
    local f
    for f in "$report_dir"/.maestro/tests/*/maestro.log "$HOME/.maestro/tests"/*/maestro.log; do
      [[ -f "$f" ]] || continue
      port="$(grep -Eo 'using port [0-9]+|TEST_RUNNER_PORT=[0-9]+' "$f" 2>/dev/null | grep -Eo '[0-9]+$' | tail -1 || true)"
      [[ -n "$port" ]] && break
    done
    if [[ -n "$port" && "$port" != "$active" ]]; then
      if [[ -n "$IPROXY_PID" ]]; then
        kill "$IPROXY_PID" 2>/dev/null || true
        wait "$IPROXY_PID" 2>/dev/null || true
      fi
      pkill -f "iproxy ${active} ${active} -u ${udid}" 2>/dev/null || true
      echo "==> USB port-forward localhost:${port} → device ${udid} (Maestro driver)"
      iproxy "$port" "$port" -u "$udid" >/tmp/maestro-iproxy-${port}.log 2>&1 &
      IPROXY_PID=$!
      disown "$IPROXY_PID" 2>/dev/null || true
      active="$port"
    fi
    sleep 0.5
  done
}

install_on_device() {
  local app="$1"
  local udid="$2"
  echo "==> Installing $app on device $udid"
  if xcrun devicectl device install app --device "$udid" "$app"; then
    return 0
  fi
  (cd "$ROOT/momentra" && xcodebuild \
    -scheme momentra \
    -destination "id=$udid" \
    -configuration Debug \
    -derivedDataPath "$DERIVED_ROOT" \
    install | tail -30)
}

TARGET="${MAESTRO_TARGET:-auto}"
PHYSICAL_UDID="$(detect_physical_udid || true)"

if [[ "$TARGET" == "auto" ]]; then
  if [[ -n "${MAESTRO_UDID:-}" ]]; then
    UDID="$MAESTRO_UDID"
    if is_hardware_udid "$UDID"; then TARGET="device"; else TARGET="simulator"; fi
  elif [[ -n "$PHYSICAL_UDID" ]]; then
    TARGET="device"
    UDID="$PHYSICAL_UDID"
  else
    TARGET="simulator"
    UDID="$(detect_simulator_udid)"
  fi
elif [[ "$TARGET" == "device" ]]; then
  UDID="${MAESTRO_UDID:-$PHYSICAL_UDID}"
  [[ -n "$UDID" ]] || { echo "No physical iPhone connected (xcrun xctrace list devices)."; exit 1; }
elif [[ "$TARGET" == "simulator" ]]; then
  UDID="${MAESTRO_UDID:-$(detect_simulator_udid)}"
else
  echo "Unknown MAESTRO_TARGET=$TARGET (use auto|device|simulator)"
  exit 1
fi

echo "==> Target: $TARGET"
echo "==> UDID: $UDID"

FLOW="${1:-$ROOT/.maestro/flows/01_onboarding_to_login.yaml}"
REPORT_DIR="$ROOT/.maestro/reports/$(date +%Y%m%d_%H%M%S)"
mkdir -p "$REPORT_DIR"

DEVICE_FLOW_TMP=""
if [[ "$TARGET" == "device" && -f "$FLOW" ]] && grep -q '00_launch_clear.yaml' "$FLOW"; then
  DEVICE_FLOW_TMP="$(mktemp /tmp/maestro_device_flow.XXXXXX.yaml)"
  sed 's/00_launch_clear\.yaml/00_launch_device.yaml/g' "$FLOW" >"$DEVICE_FLOW_TMP"
  echo "==> Device run: using 00_launch_device.yaml (clearKeychain unsupported on physical iOS)"
  FLOW="$DEVICE_FLOW_TMP"
fi

if [[ "$TARGET" == "device" ]]; then
  if ! command -v iproxy >/dev/null 2>&1; then
    echo "iproxy not found. Install with: brew install libimobiledevice"
    exit 1
  fi
  ensure_iphoneos_driver
  DERIVED="${DERIVED:-$DERIVED_ROOT/Build/Products/Debug-iphoneos/momentra.app}"
  if [[ ! -d "$DERIVED" ]]; then
    echo "==> Building for iphoneos..."
    (cd "$ROOT/momentra" && xcodebuild \
      -scheme momentra \
      -destination "generic/platform=iOS" \
      -configuration Debug \
      -derivedDataPath "$DERIVED_ROOT" \
      DEVELOPMENT_TEAM="$APPLE_TEAM_ID" \
      build | tail -40)
  fi
  install_on_device "$DERIVED" "$UDID"
  watch_maestro_driver_port "$UDID" "$REPORT_DIR" &
  IPROXY_WATCH_PID=$!
  disown "$IPROXY_WATCH_PID" 2>/dev/null || true
else
  DERIVED="${DERIVED:-$DERIVED_ROOT/Build/Products/Debug-iphonesimulator/momentra.app}"
  xcrun simctl boot "$UDID" 2>/dev/null || true
  open -a Simulator --args -CurrentDeviceUDID "$UDID" || true
  if [[ ! -d "$DERIVED" ]]; then
    echo "==> Building for iphonesimulator..."
    (cd "$ROOT/momentra" && xcodebuild \
      -scheme momentra \
      -destination "platform=iOS Simulator,id=$UDID" \
      -configuration Debug \
      -derivedDataPath "$DERIVED_ROOT" \
      build | tail -20)
  fi
  echo "==> Installing $DERIVED"
  xcrun simctl install "$UDID" "$DERIVED"
fi

echo "==> Running Maestro on $UDID → $FLOW"
if [[ "$TARGET" == "device" ]]; then
  echo "==> Tip: if the terminal shows 'Password:', enter your Mac login password (Keychain/code signing — not the app login)."
fi
set +e
maestro --device "$UDID" test "$FLOW" \
  "${MAESTRO_ENV_ARGS[@]}" \
  --format junit \
  --output "$REPORT_DIR/junit.xml" \
  --debug-output "$REPORT_DIR"
STATUS=$?
set -e

echo "==> Reports: $REPORT_DIR (exit $STATUS)"
exit $STATUS
