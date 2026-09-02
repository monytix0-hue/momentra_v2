#!/usr/bin/env bash
# Launch Maestro Studio against a physical iPhone with USB port-forwarding.
#
# Why this exists:
#   Plain `maestro studio` often fails with "iOS driver not ready in time" because:
#   1) JAVA_HOME is unset (system java stub), and
#   2) the XCTest driver listens on the *device* loopback; Mac needs iproxy.
#
# Usage:
#   ./.maestro/studio-ios.sh
#   MAESTRO_UDID=00008110-... ./.maestro/studio-ios.sh
#
# Keep this terminal open while Studio is running (iproxy watcher lives here).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
export JAVA_HOME="${JAVA_HOME:-$HOME/.jdks/jdk-21.0.12.1+1/Contents/Home}"
export PATH="/usr/local/bin:/opt/homebrew/bin:$JAVA_HOME/bin:$HOME/.maestro/bin:$PATH"
export MAESTRO_CLI_NO_ANALYTICS="${MAESTRO_CLI_NO_ANALYTICS:-true}"
export MAESTRO_CLI_ANALYSIS_NOTIFICATION_DISABLED=true
export MAESTRO_DRIVER_STARTUP_TIMEOUT="${MAESTRO_DRIVER_STARTUP_TIMEOUT:-300000}"

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
}
trap cleanup EXIT

if [[ ! -x "$JAVA_HOME/bin/java" ]]; then
  echo "JAVA_HOME missing or invalid: $JAVA_HOME"
  echo "Install JDK 21 or set JAVA_HOME, then retry."
  exit 1
fi
if ! command -v maestro >/dev/null 2>&1; then
  echo "maestro not on PATH"
  exit 1
fi
if ! command -v iproxy >/dev/null 2>&1; then
  echo "iproxy not found. Install with: brew install libimobiledevice"
  exit 1
fi

# shellcheck disable=SC1091
source "$ROOT/.maestro/load-maestro-env.sh"
load_maestro_env "$ROOT/.maestro/.env.maestro.local" "$(date +%Y%m%d%H%M%S)"
export MAESTRO_DRIVER_STARTUP_TIMEOUT="${MAESTRO_DRIVER_STARTUP_TIMEOUT:-300000}"

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

UDID="${MAESTRO_UDID:-$(detect_physical_udid || true)}"
[[ -n "$UDID" ]] || {
  echo "No physical iPhone found (xcrun xctrace list devices)."
  echo "Unlock the phone, trust this Mac, enable Developer Mode, reconnect USB."
  exit 1
}

echo "==> Java: $(java -version 2>&1 | head -1)"
echo "==> Maestro: $(maestro --version 2>&1 | tail -1)"
echo "==> iPhone UDID: $UDID"
echo "==> Driver timeout: ${MAESTRO_DRIVER_STARTUP_TIMEOUT}ms"

marker="$HOME/.maestro/maestro-iphoneos-driver-build/driver-iphoneos/Build/Products/Debug-iphoneos/maestro-driver-iosUITests-Runner.app"
if [[ ! -d "$marker" ]]; then
  echo "==> Building Maestro iOS device driver (team $APPLE_TEAM_ID)..."
  maestro driver-setup --apple-team-id="$APPLE_TEAM_ID"
else
  echo "==> iOS device driver already built"
fi

# Parse port from Studio/CLI logs. Driver listens on device loopback at that port.
extract_driver_port() {
  local port=""
  local f
  shopt -s nullglob
  for f in \
    "$HOME/.maestro/tests"/*/maestro.log \
    "$HOME/.maestro/tests"/*/xctest_runner_*.log \
    /tmp/maestro*.log
  do
    [[ -f "$f" ]] || continue
    port="$(
      grep -Eo 'starting server 127\.0\.0\.1:[0-9]+|using port [0-9]+|TEST_RUNNER_PORT=[0-9]+|Failed to connect to /127\.0\.0\.1:[0-9]+' "$f" 2>/dev/null \
        | grep -Eo '[0-9]+$' \
        | tail -1 || true
    )"
    [[ -n "$port" ]] && { echo "$port"; return 0; }
  done
  return 1
}

watch_maestro_driver_port() {
  local udid="$1"
  local active=""
  while true; do
    local port=""
    port="$(extract_driver_port || true)"
    if [[ -n "$port" && "$port" != "$active" ]]; then
      if [[ -n "$IPROXY_PID" ]]; then
        kill "$IPROXY_PID" 2>/dev/null || true
        wait "$IPROXY_PID" 2>/dev/null || true
      fi
      pkill -f "iproxy ${active} ${active} -u ${udid}" 2>/dev/null || true
      echo "==> USB port-forward localhost:${port} → device ${udid}"
      iproxy "$port" "$port" -u "$udid" >/tmp/maestro-iproxy-${port}.log 2>&1 &
      IPROXY_PID=$!
      disown "$IPROXY_PID" 2>/dev/null || true
      active="$port"
    fi
    sleep 0.4
  done
}

echo "==> Starting USB port-forward watcher (keep this terminal open)"
watch_maestro_driver_port "$UDID" &
IPROXY_WATCH_PID=$!
disown "$IPROXY_WATCH_PID" 2>/dev/null || true

echo "==> Tip: unlock iPhone; if Mac asks for Keychain password, use your Mac login password."
echo "==> Launching Maestro Studio…"
exec maestro studio
