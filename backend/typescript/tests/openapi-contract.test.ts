import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import { describe, it } from 'node:test';
import SwaggerParser from '@apidevtools/swagger-parser';
import YAML from 'yaml';

const root = join(__dirname, '..');
const specPath = join(root, 'openapi', 'momentra-v1.yaml');
const commonPath = join(root, 'openapi', 'schemas', 'common.yaml');

describe('OpenAPI contract (Phase 2)', () => {
  it('parses and resolves all $refs', async () => {
    const api = await SwaggerParser.validate(specPath);
    assert.ok(api.paths);
  });

  it('has unique operationIds', async () => {
    const api = await SwaggerParser.dereference(specPath);
    const ids = new Map<string, string>();
    for (const [path, item] of Object.entries(api.paths ?? {})) {
      for (const method of ['get', 'post', 'patch', 'delete', 'put'] as const) {
        const op = (item as Record<string, { operationId?: string }>)[method];
        if (!op?.operationId) continue;
        const prev = ids.get(op.operationId);
        assert.ok(!prev, `Duplicate operationId ${op.operationId}: ${prev} and ${path}`);
        ids.set(op.operationId, path);
      }
    }
    assert.ok(ids.size >= 50);
  });

  it('requires bearer auth on all /v1 product endpoints', async () => {
    const api = await SwaggerParser.dereference(specPath);
    for (const [path, item] of Object.entries(api.paths ?? {})) {
      for (const method of ['get', 'post', 'patch', 'delete'] as const) {
        const op = (item as Record<string, { security?: unknown[] }>)[method];
        if (!op) continue;
        assert.ok(Array.isArray(op.security) && op.security.length > 0, `${method.toUpperCase()} ${path} missing security`);
      }
    }
  });

  it('documents error responses on command endpoints', async () => {
    const api = await SwaggerParser.dereference(specPath);
    for (const [path, item] of Object.entries(api.paths ?? {})) {
      for (const method of ['post', 'patch', 'delete'] as const) {
        const op = (item as Record<string, { responses?: Record<string, unknown> }>)[method];
        if (!op?.responses) continue;
        assert.ok(op.responses['400'], `${method.toUpperCase()} ${path} missing 400`);
        assert.ok(op.responses['401'], `${method.toUpperCase()} ${path} missing 401`);
        assert.ok(op.responses['500'], `${method.toUpperCase()} ${path} missing 500`);
      }
    }
  });

  it('marks idempotent-sensitive commands with x-momentra-idempotency', async () => {
    const raw = YAML.parse(readFileSync(specPath, 'utf8'));
    const sensitive = ['createMoment', 'createExpense', 'createMovement', 'createMediaUpload'];
    for (const opId of sensitive) {
      let found = false;
      for (const item of Object.values(raw.paths ?? {}) as Record<string, Record<string, unknown>>[]) {
        for (const op of Object.values(item)) {
          if ((op as { operationId?: string }).operationId === opId) {
            assert.equal((op as { 'x-momentra-idempotency'?: string })['x-momentra-idempotency'], 'required');
            found = true;
          }
        }
      }
      assert.ok(found, `Missing operation ${opId}`);
    }
  });

  it('marks OCC mutations with x-momentra-occ', async () => {
    const raw = YAML.parse(readFileSync(specPath, 'utf8'));
    for (const item of Object.values(raw.paths ?? {}) as Record<string, Record<string, unknown>>[]) {
      const patch = item.patch as { operationId?: string; 'x-momentra-occ'?: string } | undefined;
      if (patch?.operationId === 'updateMoment') {
        assert.equal(patch['x-momentra-occ'], 'required');
      }
    }
  });

  it('uses decimal-safe money representation', () => {
    const common = YAML.parse(readFileSync(commonPath, 'utf8'));
    assert.equal(common.Money.properties.amount.type, 'string');
    assert.notEqual(common.Money.properties.amount.type, 'number');
    assert.equal(common.ExpenseCreateRequest.properties.amount.type, 'string');
  });

  it('paginated routes declare cursor pagination extension', async () => {
    const raw = YAML.parse(readFileSync(specPath, 'utf8'));
    const paginated = ['listPersonalMoments', 'getPersonalActivity', 'listGroupMoments'];
    for (const opId of paginated) {
      let ok = false;
      for (const item of Object.values(raw.paths ?? {}) as Record<string, Record<string, unknown>>[]) {
        for (const op of Object.values(item)) {
          if ((op as { operationId?: string }).operationId === opId) {
            assert.equal((op as { 'x-momentra-pagination'?: string })['x-momentra-pagination'], 'cursor');
            ok = true;
          }
        }
      }
      assert.ok(ok, opId);
    }
  });

  it('health spec validates separately', async () => {
    await SwaggerParser.validate(join(root, 'openapi', 'health.yaml'));
  });
});
