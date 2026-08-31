#!/bin/sh
set -eu
if [ "$#" -ne 1 ]; then
  echo "Usage: $0 <postgres-connection-uri>" >&2
  exit 2
fi
URI=$1
BASE=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
"$BASE/scripts/verify_checksums.sh"
while IFS= read -r file; do
  [ -n "$file" ] || continue
  echo "==> $file"
  psql "$URI" -v ON_ERROR_STOP=1 -f "$BASE/migrations/$file"
done < "$BASE/manifest/MIGRATION_ORDER.txt"
