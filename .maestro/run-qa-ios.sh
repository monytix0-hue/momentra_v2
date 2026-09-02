#!/usr/bin/env bash
# S9-QA - iOS Maestro suite (Mac / CI only).
# Usage: ./run-qa-ios.sh <smoke|critical|cert|pilot|input|stress|all> [personal|group|business] [shard]
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CLASS="${1:-smoke}"
CONTEXT="${2:-}"
SHARD="${3:-0}"

if [[ "$(uname)" != "Darwin" ]]; then
  echo "IOS_MAESTRO EXECUTION=BLOCKED_ENVIRONMENT (requires macOS + Simulator)"
  echo "Suite is implemented under .maestro/ios/ , .maestro/cert/ios/ , .maestro/input/ios/"
  echo "Record status BLOCKED_ENVIRONMENT and continue Android certification on this host."
  # Still generate flows so Mac CI has artifacts ready
  if [[ "$CLASS" == "pilot" || "$CLASS" == "input" || "$CLASS" == "stress" ]]; then
    (cd "$ROOT/backend/typescript" && QA_FIXTURES_ENABLED=true npm run qa:generate-input-flows) || true
  fi
  exit 2
fi

export JAVA_HOME="${JAVA_HOME:-$HOME/.jdks/jdk-21.0.12.1+1/Contents/Home}"
export PATH="$JAVA_HOME/bin:$HOME/.maestro/bin:$PATH"

# shellcheck disable=SC1091
source "$ROOT/.maestro/load-maestro-env.sh"
RUN_ID="$(date +%Y%m%d%H%M%S)"
load_maestro_env "$ROOT/.maestro/.env.maestro.local" "$RUN_ID"

UDID="${MAESTRO_IOS_UDID:-}"
if [[ -z "$UDID" ]]; then
  UDID="$(xcrun simctl list devices available | grep -E 'iPhone' | head -1 | grep -oE '[A-F0-9-]{36}' || true)"
fi
[[ -n "$UDID" ]] || { echo "No iOS Simulator UDID"; exit 1; }

REPORT="$ROOT/.maestro/reports/qa_ios_${CLASS}_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$REPORT"

echo "==> S9-QA class=$CLASS context=$CONTEXT shard=$SHARD device=$UDID"

# Ensure input flows exist
if [[ "$CLASS" == "pilot" || "$CLASS" == "input" || "$CLASS" == "stress" ]]; then
  if [[ ! -f "$ROOT/.maestro/input/MANIFEST.json" ]]; then
    (cd "$ROOT/backend/typescript" && QA_FIXTURES_ENABLED=true npm run qa:sync-ledger-data && npm run qa:generate-input-flows)
  fi
fi

FLOW=""
case "$CLASS" in
  smoke|critical|isolation|all)
    FLOW="$ROOT/.maestro/ios"
    ;;
  cert)
    FLOW="$ROOT/.maestro/cert/ios"
    ;;
  pilot)
    FLOW="$ROOT/.maestro/input/ios/pilot"
    ;;
  input)
    [[ -n "$CONTEXT" ]] || { echo "Usage: $0 input personal|group|business [shard]"; exit 1; }
    if [[ "$SHARD" != "0" ]]; then
      FLOW=$(printf "%s/.maestro/input/ios/%s/shard_%02d.yaml" "$ROOT" "$CONTEXT" "$SHARD")
    else
      FLOW="$ROOT/.maestro/input/ios/$CONTEXT"
    fi
    ;;
  stress)
    if [[ "$SHARD" != "0" ]]; then
      FLOW=$(printf "%s/.maestro/input/ios/stress/interleaved_%02d.yaml" "$ROOT" "$SHARD")
    else
      FLOW="$ROOT/.maestro/input/ios/stress"
    fi
    ;;
  *)
    echo "Unknown class $CLASS"; exit 1
    ;;
esac

[[ -e "$FLOW" ]] || { echo "Missing flow path $FLOW"; exit 1; }

if [[ -f "$FLOW" ]]; then
  maestro --device "$UDID" test "$FLOW" "${MAESTRO_ENV_ARGS[@]}" --format junit --output "$REPORT/junit.xml" --debug-output "$REPORT"
else
  TAG="$CLASS"
  [[ "$CLASS" == "all" ]] && TAG="*"
  maestro --device "$UDID" test "$FLOW" "${MAESTRO_ENV_ARGS[@]}" --include-tags "$TAG" --format junit --output "$REPORT/junit.xml" --debug-output "$REPORT"
fi

EXIT=$?
WAVE="X"
case "$CLASS" in
  pilot) WAVE="E" ;;
  input)
    case "$CONTEXT" in
      personal) WAVE="F" ;;
      group) WAVE="G" ;;
      business) WAVE="H" ;;
      *) WAVE="F" ;;
    esac
    ;;
  stress) WAVE="I" ;;
esac
MILESTONE=$((SHARD * 50))
(cd "$ROOT/backend/typescript" && QA_FIXTURES_ENABLED=true \
  npm run qa:record-backend-checkpoint -- --platform ios --wave "$WAVE" --milestone "$MILESTONE" --run-id "$RUN_ID" || true)
(cd "$ROOT/backend/typescript" && QA_FIXTURES_ENABLED=true npm run qa:generate-certification-report -- --platform ios --class "$CLASS" --run-id "$RUN_ID" --exit "$EXIT") || true
exit $EXIT
