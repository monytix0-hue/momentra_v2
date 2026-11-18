#!/usr/bin/env bash
set -euo pipefail
fail=0
for p in "$@"; do
  if [[ -e "$p" ]]; then echo "STILL_PRESENT: $p"; fail=1; fi
done
exit $fail