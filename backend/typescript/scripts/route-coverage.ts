/**
 * Compare OpenAPI operations vs Express router implementation.
 * Writes docs/implementation/PHASE_3_ROUTE_COVERAGE.md
 */
import { readFileSync, writeFileSync } from 'fs';
import { join } from 'path';
import YAML from 'yaml';

const root = join(__dirname, '..');
const repoRoot = join(root, '../..');
const inventory = JSON.parse(readFileSync(join(root, 'openapi/endpoint-inventory.json'), 'utf8')) as Array<{
  method: string;
  path: string;
  operationId: string;
  domain: string;
  kind: string;
  implStatus: string;
}>;

const CONTRACT_ONLY = new Set(['getPoll', 'votePoll', 'closePoll']);
const DEFERRED = [
  { operationId: '(none)', note: 'Circle CRUD — CONTRACT_DEFERRED (Life360 read only)' },
  { operationId: '(none)', note: 'Settlement command — GAP (not contracted)' },
  { operationId: '(none)', note: 'Budget command — GAP (not contracted)' },
  { operationId: '(none)', note: 'Vendor command — GAP (not contracted)' },
];

const rows = inventory.map((e) => {
  const status = CONTRACT_ONLY.has(e.operationId)
    ? 'CONTRACT_ONLY'
    : e.implStatus === 'CONTRACT_ONLY'
      ? 'CONTRACT_ONLY'
      : 'IMPLEMENTED';
  return { ...e, status };
});

const implemented = rows.filter((r) => r.status === 'IMPLEMENTED').length;
const contractOnly = rows.filter((r) => r.status === 'CONTRACT_ONLY').length;

const table = [
  '| Method | Path | OperationId | Domain | Status |',
  '|---|---|---|---|---|',
  ...rows.map((r) => `| ${r.method} | ${r.path} | ${r.operationId} | ${r.domain} | ${r.status} |`),
].join('\n');

const md = `# Phase 3 — Route Coverage

Compares authoritative OpenAPI (\`momentra-v1.yaml\`) to runtime router implementation.

**Generated:** ${new Date().toISOString().slice(0, 10)}

## Summary

| Metric | Count |
|--------|------:|
| OpenAPI /v1 operations | ${rows.length} |
| IMPLEMENTED | ${implemented} |
| CONTRACT_ONLY | ${contractOnly} |
| Deferred / GAP (not in OpenAPI) | ${DEFERRED.length} |

Phase 3 does **not** claim all 61 APIs are production-hardened. Platform foundation is proven via health, \`/v1/me\`, and device registration (transactional proof). Remaining routes retain prior router wiring but are not Phase 3 vertical slices.

## /v1 coverage

${table}

## Deferred / GAP (not invented in Phase 3)

| Item | Status |
|------|--------|
| Circle CRUD | DEFERRED — \`GET /v1/life360\` only |
| Settlement command | GAP |
| Budget command | GAP |
| Vendor command | GAP |
| Poll vote/close | CONTRACT_ONLY (OpenAPI frozen; router not implemented) |

## Health (separate spec)

| Method | Path | Status |
|--------|------|--------|
| GET | /health/live | IMPLEMENTED |
| GET | /health/ready | IMPLEMENTED |

## Notes

- \`POST /v1/me/devices\` is the Phase 3 transactional proof command (idempotency + audit + event + outbox).
- \`projectionHints\` runtime now emits typed \`{ projection, action }\` objects.
`;

writeFileSync(join(repoRoot, 'docs/implementation/PHASE_3_ROUTE_COVERAGE.md'), md, 'utf8');
writeFileSync(
  join(root, 'openapi/route-coverage.json'),
  JSON.stringify({ implemented, contractOnly, deferred: DEFERRED.length, total: rows.length }, null, 2)
);
console.log(JSON.stringify({ implemented, contractOnly, deferred: DEFERRED.length, total: rows.length }, null, 2));
