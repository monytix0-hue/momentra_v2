/**
 * Behaviour under test: OpenAPI documents P4 operating/widgets routes + error contracts.
 * Module: openapi/momentra-v1.yaml (from scripts/build-openapi.ts).
 * Existing tests: none for widgets OpenAPI; P3 covers widget analytics service only.
 * User: "Regenerate and validate OpenAPI against the implemented Business V4 routes."
 * + "Include `/widgets` and `/widgets/:widgetId/sources`."
 * + "Preserve 401, 403, 409 and validation-error behavior."
 */
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import { describe, it } from 'node:test';

const yaml = readFileSync(join(__dirname, '..', 'openapi', 'momentra-v1.yaml'), 'utf8');

describe('P4 OpenAPI Business V4 freeze', () => {
  const requiredPaths = [
    '/business/moments/{momentId}/operating',
    '/business/moments/{momentId}/ledger',
    '/business/moments/{momentId}/widgets',
    '/business/moments/{momentId}/widgets/{widgetId}/sources',
    '/business/moments/{momentId}/operating-events',
  ];

  for (const p of requiredPaths) {
    it(`documents ${p}`, () => {
      assert.match(yaml, new RegExp(`^  ${p.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}:`, 'm'));
    });
  }

  it('widgets sources preserve 401/403/400', () => {
    const idx = yaml.indexOf('/business/moments/{momentId}/widgets/{widgetId}/sources:');
    assert.ok(idx > 0);
    const slice = yaml.slice(idx, idx + 1200);
    assert.match(slice, /'401'/);
    assert.match(slice, /'403'/);
    assert.match(slice, /'400'/);
  });

  it('operating-events command documents idempotency + 409', () => {
    const idx = yaml.indexOf('operationId: recordOperatingEvent');
    assert.ok(idx > 0);
    const slice = yaml.slice(idx, idx + 1800);
    assert.match(slice, /x-momentra-idempotency: required/);
    assert.match(slice, /'409'/);
    assert.match(slice, /'401'/);
    assert.match(slice, /'403'/);
  });

  it('widgets summary mentions coverage states', () => {
    assert.match(yaml, /COMPLETE\|PARTIAL\|UNAVAILABLE\|ZERO/);
  });
});
