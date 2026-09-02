# shellcheck shell=bash
# Source from run-ios.sh / run-qa-ios.sh / before `maestro studio`
# Maestro CLI does not inherit shell env for ${VAR} in flows — pass -e KEY=VALUE.
# Driver process env (e.g. MAESTRO_DRIVER_STARTUP_TIMEOUT) is exported but not passed via -e.

MAESTRO_ENV_ARGS=()

# Process-level Maestro knobs (not flow ${VAR}s).
_maestro_is_driver_env_key() {
  case "$1" in
    MAESTRO_DRIVER_STARTUP_TIMEOUT|MAESTRO_CLI_NO_ANALYTICS|MAESTRO_CLI_ANALYSIS_NOTIFICATION_DISABLED) return 0 ;;
    *) return 1 ;;
  esac
}

load_maestro_env() {
  local env_file="${1:-}"
  local run_id="${2:-$(date +%Y%m%d%H%M%S)}"
  MAESTRO_ENV_ARGS=()

  # Default 5 min — Studio / cold iOS driver often exceeds Maestro's shorter default.
  export MAESTRO_DRIVER_STARTUP_TIMEOUT="${MAESTRO_DRIVER_STARTUP_TIMEOUT:-300000}"

  if [[ -z "$env_file" || ! -f "$env_file" ]]; then
    export MAESTRO_RUN_ID="$run_id"
    MAESTRO_ENV_ARGS=(-e "MAESTRO_RUN_ID=$run_id")
    return 0
  fi

  while IFS= read -r line || [[ -n "$line" ]]; do
    line="${line%%#*}"
    line="$(echo "$line" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')"
    [[ -z "$line" ]] && continue
    [[ "$line" != *"="* ]] && continue

    local key="${line%%=*}"
    local val="${line#*=}"
    key="$(echo "$key" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')"
    val="$(echo "$val" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//' | sed -e 's/^"//' -e 's/"$//' -e "s/^'//" -e "s/'$//")"
    [[ "$key" == "MAESTRO_RUN_ID" ]] && continue

    export "$key=$val"
    if ! _maestro_is_driver_env_key "$key"; then
      MAESTRO_ENV_ARGS+=(-e "${key}=${val}")
    fi
  done < "$env_file"

  export MAESTRO_RUN_ID="$run_id"
  MAESTRO_ENV_ARGS+=(-e "MAESTRO_RUN_ID=$run_id")
}
