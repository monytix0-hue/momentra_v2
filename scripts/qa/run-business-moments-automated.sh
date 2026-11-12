#!/usr/bin/env bash
# Business-moments automated harness (unit + backend — not full device E2E).
#
# Runs:
#   1) Backend operating-*.test.ts (honesty / projection / analytics / POS)
#   2) operating-moment-capabilities.test.ts when DATABASE_URL is reachable
#   3) Android JVM unit tests under ui.shell.business (hub fidelity + sheet wiring)
#
# Not covered here (still manual / Maestro PENDING):
#   - Visual Figma parity, toast copy on device, Logcat-only faults
#   - Full sheet fill → POST → projection round-trips on a phone
#   - Maestro certification ledger waves
#
# Usage (repo root):
#   ./scripts/qa/run-business-moments-automated.sh
#   DATABASE_URL=postgres://… ./scripts/qa/run-business-moments-automated.sh
#
# Docs: docs/qa/BUSINESS_MOMENTS_AUTOMATED.md
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
BACKEND_TS="$ROOT/backend/typescript"
APK="$ROOT/apk"
LOG_DIR="${TMPDIR:-/tmp}/momentra-bm-qa"
mkdir -p "$LOG_DIR"

pass=()
fail=()
skip=()

section() { printf '\n━━ %s ━━\n' "$1"; }
record_pass() { pass+=("$1"); printf '  PASS  %s\n' "$1"; }
record_fail() { fail+=("$1"); printf '  FAIL  %s\n' "$1"; }
record_skip() { skip+=("$1"); printf '  SKIP  %s\n' "$1"; }

# --- env: DATABASE_URL from backend/.env if unset ---------------------------------
load_database_url() {
  if [[ -n "${DATABASE_URL:-}" ]]; then
    return 0
  fi
  local env_file="$ROOT/backend/.env"
  if [[ -f "$env_file" ]]; then
    # shellcheck disable=SC1090
    DATABASE_URL="$(grep -E '^DATABASE_URL=' "$env_file" | head -1 | cut -d= -f2- | sed -e 's/^"//' -e 's/"$//' -e "s/^'//" -e "s/'$//")"
    export DATABASE_URL
  fi
}

db_reachable() {
  [[ -n "${DATABASE_URL:-}" ]] || return 1
  if command -v psql >/dev/null 2>&1; then
    PGPASSWORD="" psql "$DATABASE_URL" -c 'SELECT 1' >/dev/null 2>&1 && return 0
  fi
  # Fallback: node pg via the backend package (already a dependency).
  (cd "$BACKEND_TS" && node --input-type=module -e "
    import pg from 'pg';
    const pool = new pg.Pool({ connectionString: process.env.DATABASE_URL, connectionTimeoutMillis: 5000 });
    try {
      await pool.query('SELECT 1');
      await pool.end();
      process.exit(0);
    } catch (e) {
      try { await pool.end(); } catch {}
      process.exit(1);
    }
  ") >/dev/null 2>&1
}

# --- JAVA_HOME discovery for Android unit tests -----------------------------------
discover_java_home() {
  if [[ -n "${JAVA_HOME:-}" && -x "${JAVA_HOME}/bin/java" ]]; then
    return 0
  fi
  local candidate
  for candidate in \
    "$HOME/.gradle/jdks"/eclipse_adoptium-21* \
    "$HOME/.gradle/jdks"/OpenJDK21* \
    /usr/lib/jvm/java-21-* \
    /usr/lib/jvm/temurin-21-*
  do
    if [[ -d "$candidate" && -x "$candidate/bin/java" ]]; then
      export JAVA_HOME="$candidate"
      return 0
    fi
  done
  return 1
}

# --- 1) Backend operating tests (no DB required for most) -------------------------
run_backend_operating() {
  section "Backend operating-*.test.ts"
  local log="$LOG_DIR/backend-operating.log"
  local files=()
  local f
  for f in "$BACKEND_TS"/tests/operating-*.test.ts; do
    [[ -f "$f" ]] || continue
    case "$(basename "$f")" in
      operating-moment-capabilities.test.ts) continue ;;
      *) files+=("$f") ;;
    esac
  done
  if [[ ${#files[@]} -eq 0 ]]; then
    record_fail "backend/operating (no files)"
    return
  fi
  if (cd "$BACKEND_TS" && npx --yes tsx --test "${files[@]}" >"$log" 2>&1); then
    record_pass "backend/operating (${#files[@]} files)"
  else
    record_fail "backend/operating"
    tail -n 40 "$log" || true
  fi
}

run_backend_capabilities() {
  section "Backend operating-moment-capabilities (needs DATABASE_URL)"
  load_database_url
  if [[ -z "${DATABASE_URL:-}" ]]; then
    record_skip "backend/capabilities (DATABASE_URL unset — load backend/.env or export it)"
    return
  fi
  if ! db_reachable; then
    record_skip "backend/capabilities (DATABASE_URL set but not reachable)"
    return
  fi
  local log="$LOG_DIR/backend-capabilities.log"
  if (cd "$BACKEND_TS" && npx --yes tsx --test tests/operating-moment-capabilities.test.ts >"$log" 2>&1); then
    record_pass "backend/capabilities"
  else
    record_fail "backend/capabilities"
    tail -n 40 "$log" || true
  fi
}

# --- 2) Android business unit tests -----------------------------------------------
run_android_business() {
  section "Android ui.shell.business unit tests"
  if ! discover_java_home; then
    record_skip "android/business (JAVA_HOME not found — expected ~/.gradle/jdks/eclipse_adoptium-21*)"
    return
  fi
  printf '  JAVA_HOME=%s\n' "$JAVA_HOME"
  local log="$LOG_DIR/android-business.log"
  # Include fidelity smokes + existing TradingSheetState / copy / semantics tests.
  if (cd "$APK" && ./gradlew :app:testDebugUnitTest \
      --tests 'com.example.momentra.ui.shell.business.*' \
      -q >"$log" 2>&1); then
    record_pass "android/business (ui.shell.business.*)"
  else
    record_fail "android/business"
    # Surface failures without dumping the entire Gradle log.
    grep -E 'FAILED|failed|AssertionError|e: |FAILURE' "$log" | tail -n 60 || tail -n 40 "$log" || true
  fi
}

# --- matrix -----------------------------------------------------------------------
print_matrix() {
  section "Summary matrix"
  printf '\n%-36s %s\n' "SUITE" "RESULT"
  printf '%-36s %s\n' "------------------------------------" "------"
  local item
  for item in "${pass[@]:-}"; do
    [[ -n "$item" ]] && printf '%-36s %s\n' "$item" "PASS"
  done
  for item in "${fail[@]:-}"; do
    [[ -n "$item" ]] && printf '%-36s %s\n' "$item" "FAIL"
  done
  for item in "${skip[@]:-}"; do
    [[ -n "$item" ]] && printf '%-36s %s\n' "$item" "SKIP"
  done
  printf '\nLogs: %s\n' "$LOG_DIR"
  printf 'Manual still required: device walks (Figma/toast/Logcat), Maestro cert (PENDING).\n'
  printf 'See docs/qa/BUSINESS_MOMENTS_AUTOMATED.md\n'
}

main() {
  printf 'Business moments automated harness\n'
  printf 'Repo: %s\n' "$ROOT"
  run_backend_operating
  run_backend_capabilities
  run_android_business
  print_matrix
  if [[ ${#fail[@]} -gt 0 ]]; then
    exit 1
  fi
  exit 0
}

main "$@"
