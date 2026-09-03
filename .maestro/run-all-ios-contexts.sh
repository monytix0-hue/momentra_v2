#!/usr/bin/env bash
# Run all iOS Maestro personal / group / business suites on Simulator.
# Continues on failure; writes summary to .maestro/reports/ios_contexts_<ts>/.
set -uo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
export JAVA_HOME="${JAVA_HOME:-$HOME/.jdks/jdk-21.0.12.1+1/Contents/Home}"
export PATH="/usr/local/bin:/opt/homebrew/bin:$JAVA_HOME/bin:$HOME/.maestro/bin:$PATH"
export MAESTRO_DRIVER_STARTUP_TIMEOUT="${MAESTRO_DRIVER_STARTUP_TIMEOUT:-600000}"
export MAESTRO_TARGET=simulator

TS="$(date +%Y%m%d_%H%M%S)"
REPORT="$ROOT/.maestro/reports/ios_contexts_${TS}"
SUMMARY="$REPORT/summary.tsv"
mkdir -p "$REPORT"

# shellcheck disable=SC1091
source "$ROOT/.maestro/load-maestro-env.sh"
RUN_ID="$(date +%Y%m%d%H%M%S)"
load_maestro_env "$ROOT/.maestro/.env.maestro.local" "$RUN_ID"

UDID="${MAESTRO_IOS_UDID:-}"
if [[ -z "$UDID" ]]; then
  UDID="$(python3 - <<'PY'
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
)"
fi
[[ -n "$UDID" ]] || { echo "No iOS Simulator"; exit 1; }

echo "==> UDID=$UDID"
echo "==> Report=$REPORT"

# Build + install once
DERIVED="${DERIVED:-$HOME/Library/Developer/Xcode/DerivedData/momentra-afcfwpbbsgmhvbgkxqovavvbmkno/Build/Products/Debug-iphonesimulator/momentra.app}"
if [[ ! -d "$DERIVED" ]]; then
  echo "==> Building simulator app..."
  (cd "$ROOT/momentra" && xcodebuild -scheme momentra \
    -destination "platform=iOS Simulator,id=$UDID" -configuration Debug \
    -derivedDataPath "${DERIVED_ROOT:-$HOME/Library/Developer/Xcode/DerivedData/momentra-afcfwpbbsgmhvbgkxqovavvbmkno}" \
    build >/dev/null)
fi
echo "==> Installing $DERIVED on $UDID"
xcrun simctl shutdown "$UDID" 2>/dev/null || true
sleep 2
xcrun simctl boot "$UDID" 2>/dev/null || true
xcrun simctl bootstatus "$UDID" -b
xcrun simctl uninstall "$UDID" resolvingpoint.momentra 2>/dev/null || true
xcrun simctl install "$UDID" "$DERIVED"
sleep 5
echo "==> Simulator ready"

printf "phase\tflow\tstatus\tseconds\tnote\n" >"$SUMMARY"

run_one() {
  local phase="$1"
  local flow="$2"
  local name
  name="$(basename "$flow" .yaml)"
  local out="$REPORT/${phase}__${name}"
  mkdir -p "$out"
  local start end elapsed status note
  start=$(date +%s)
  echo ""
  echo "========================================"
  echo "==> [$phase] $flow"
  echo "========================================"
  if maestro --device "$UDID" test "$flow" "${MAESTRO_ENV_ARGS[@]}" \
      --format junit --output "$out/junit.xml" --debug-output "$out" 2>&1 | tee "$out/run.log"; then
    status=PASS
    note=""
  else
    status=FAIL
    note="$(rg -o 'Assertion.*|CommandFailed:.*|Element not.*|Unknown error|Invalid File Path.*' "$out/run.log" 2>/dev/null | head -1 || echo "see run.log")"
  fi
  end=$(date +%s)
  elapsed=$((end - start))
  printf "%s\t%s\t%s\t%s\t%s\n" "$phase" "$name" "$status" "$elapsed" "$note" >>"$SUMMARY"
  echo "==> $status (${elapsed}s) $note"
}

# --- Phase 0: pilot (gate) ---
for p in pilot_personal pilot_group pilot_business; do
  run_one pilot "$ROOT/.maestro/input/ios/pilot/${p}.yaml"
done

# --- Phase 1: ios/ critical smoke per context ---
run_one critical "$ROOT/.maestro/ios/02_personal/critical_expense.yaml"
run_one critical "$ROOT/.maestro/ios/02_personal/smoke_account_picker.yaml"
run_one critical "$ROOT/.maestro/ios/02_personal/qh_transaction_lifecycle.yaml"
run_one critical "$ROOT/.maestro/ios/03_group/critical_expense_equal.yaml"
run_one critical "$ROOT/.maestro/ios/04_business/critical_finance.yaml"

# --- Phase 2: input shards ---
for ctx in personal group business; do
  for shard in "$ROOT/.maestro/input/ios/$ctx"/shard_*.yaml; do
    [[ -f "$shard" ]] || continue
    run_one "input_$ctx" "$shard"
  done
done

# --- Summary ---
PASS=$(awk -F'\t' 'NR>1 && $3=="PASS"{c++} END{print c+0}' "$SUMMARY")
FAIL=$(awk -F'\t' 'NR>1 && $3=="FAIL"{c++} END{print c+0}' "$SUMMARY")
TOTAL=$((PASS + FAIL))
{
  echo ""
  echo "========== iOS CONTEXTS RUN COMPLETE =========="
  echo "Total: $TOTAL  PASS: $PASS  FAIL: $FAIL"
  echo "Report: $REPORT"
  echo ""
  echo "Failures:"
  awk -F'\t' 'NR>1 && $3=="FAIL"{printf "  - [%s] %s: %s\n",$1,$2,$5}' "$SUMMARY"
} | tee "$REPORT/final_summary.txt"

exit $(( FAIL > 0 ? 1 : 0 ))
