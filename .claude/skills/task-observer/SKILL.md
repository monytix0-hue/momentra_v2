---
name: task-observer
description: Monitor running tasks, background processes, backend workers, and build/test logs across the Momentra workspace.
---

# Task Observer

Use this skill to inspect, monitor, and report on background tasks, active Docker containers, compilation outputs, test runs, and service logs in the Momentra workspace.

## What it does:
1. **Inspects running processes & containers**: Checks Docker Compose status, node/python background workers, and active build processes.
2. **Monitors logs**:tails recent logs from backend services, firebase emulators, or mobile compilation outputs (`apk/compile-out.txt`, etc.).
3. **Summarizes system health**: Reports quick status indicators so you know what's running, what failed, and what needs attention.

## How to run:
When invoked (`/task-observer`), it runs a status sweep across:
- Docker containers (`docker compose ps`)
- Recent compile/build output files (`apk/compile-out.txt`, `firebase-debug.log`, etc.)
- Git working status (`git status -s`)
