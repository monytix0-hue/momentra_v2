#!/usr/bin/env bash
# S9-QA-B — iOS Maestro suite (Mac / CI only).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CLASS="${1:-smoke}"

if [[ "$(uname)" != "Darwin" ]]; then
  echo "IOS_MAESTRO EXECUTION=BLOCKED_ENVIRONMENT (requires macOS + Simulator)"
  echo "Suite is implemented under .maestro/ios/ and .maestro/cert/ios/ — run on Mac/CI before RC."
  exit 2
fi

export JAVA_HOME="${JAVA_HOME:-$HOME/.jdks/jdk-21.0.12.1+1/Contents/Home}"
export PATH="$JAVA_HOME/bin:$HOME/.maestro/bin:$PATH"

# shellcheck disable=SC1091
source "$ROOT/.maestro/load-maestro-env.sh"
load_maestro_env "$ROOT/.maestro/.env.maestro.local" "$(date +%Y%m%d%H%M%S)"

UDID="${MAESTRO_IOS_UDID:-}"
if [[ -z "$UDID" ]]; then
  UDID="$(xcrun simctl list devices available | grep -E 'iPhone' | head -1 | grep -oE '[A-F0-9-]{36}' || true)"
fi
[[ -n "$UDID" ]] || { echo "No iOS Simulator UDID"; exit 1; }

REPORT="$ROOT/.maestro/reports/qa_ios_${CLASS}_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$REPORT"

echo "==> S9-QA-B class=$CLASS device=$UDID"
FLOW_ROOT="$ROOT/.maestro/ios"
if [[ "$CLASS" == "cert" ]]; then
  FLOW_ROOT="$ROOT/.maestro/cert/ios"
  if [[ ! -f "$ROOT/.maestro/cert/catalog.json" ]]; then
    echo "Missing catalog.json — run npm run qa:build-catalog first"
    exit 1
  fi
fi
if [[ "$CLASS" == "all" ]]; then
  maestro --device "$UDID" test "$ROOT/.maestro/ios" "${MAESTRO_ENV_ARGS[@]}" --format junit --output "$REPORT/junit.xml" --debug-output "$REPORT"
else
  maestro --device "$UDID" test "$FLOW_ROOT" "${MAESTRO_ENV_ARGS[@]}" --include-tags "$CLASS" --format junit --output "$REPORT/junit.xml" --debug-output "$REPORT"
fi
