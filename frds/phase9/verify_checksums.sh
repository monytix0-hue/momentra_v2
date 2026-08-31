#!/bin/sh
set -eu
BASE=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$BASE/migrations"
sha256sum -c "$BASE/manifest/SHA256SUMS.txt"
