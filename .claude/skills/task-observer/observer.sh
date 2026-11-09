#!/usr/bin/env bash
echo "=== MOMENTRA TASK OBSERVER ==="
echo ""
echo "[1] Git Status:"
git status -s
echo ""
echo "[2] Docker Compose Services:"
if command -v docker &> /dev/null && docker compose version &> /dev/null; then
  docker compose ps 2>/dev/null || echo "Docker compose not running / no compose file active"
else
  echo "Docker / Docker compose not available"
fi
echo ""
echo "[3] Recent Build / Log Files:"
for f in apk/compile-out.txt firebase-debug.log backend/backend.log; do
  if [ -f "$f" ]; then
    echo "--- $f (last 10 lines) ---"
    tail -n 10 "$f"
  fi
done
echo ""
echo "=== OBSERVER SWEEP COMPLETE ==="
