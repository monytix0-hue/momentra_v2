# shellcheck shell=bash
# Source from run-ios.sh / run-qa-ios.sh
# Maestro CLI does not inherit shell env for ${VAR} in flows — pass -e KEY=VALUE.

MAESTRO_ENV_ARGS=()

load_maestro_env() {
  local env_file="${1:-}"
  local run_id="${2:-$(date +%Y%m%d%H%M%S)}"
  MAESTRO_ENV_ARGS=()

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
    MAESTRO_ENV_ARGS+=(-e "${key}=${val}")
  done < "$env_file"

  export MAESTRO_RUN_ID="$run_id"
  MAESTRO_ENV_ARGS+=(-e "MAESTRO_RUN_ID=$run_id")
}
