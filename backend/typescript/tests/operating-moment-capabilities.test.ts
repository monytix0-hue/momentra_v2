/**
 * V094 seeded operating Business types with MOMENT_CREATE only.
 * Start-this-business (Android + iOS) renames the draft via MOMENT_UPDATE before
 * activate, so those types must carry the lifecycle + shared write maps.
 */
import { describe, it, before, after } from 'node:test';
import assert from 'node:assert/strict';
import { closePool, getPool } from '../src/platform/database/pool';

const OPERATING_TYPES = [
  'CLIENT_CAMPAIGN',
  'OWN_BUSINESS_CAMPAIGN',
  'TRADING_WHOLESALE',
  'DISTRIBUTION_DEALERSHIP',
  'RETAIL_STORE',
  'RESTAURANT_FOOD',
  'CONSUMER_SERVICE',
  'STANDARD_PRODUCTION',
  'MADE_TO_ORDER',
] as const;

/** Caps required so setup Start and post-activate writes are not governance-denied. */
const REQUIRED_CAPS = [
  'MOMENT_CREATE',
  'MOMENT_VIEW',
  'MOMENT_UPDATE',
  'TASK_CREATE',
  'EXPENSE_CREATE',
  'BUDGET_MANAGE',
  'REVENUE_RECORD',
  'INVOICE_CREATE',
  'UPDATE_CREATE',
  'MEMORY_CREATE',
] as const;

describe('operating moment capability maps', () => {
  before(async () => {
    await getPool().query('SELECT 1');
  });

  after(async () => {
    await closePool();
  });

  it('maps Start + shared write caps onto every operating Business subtype', async () => {
    const rows = await getPool().query<{ moment_type: string; capability: string }>(
      `SELECT mt.code AS moment_type, c.code AS capability
       FROM core.moment_type mt
       JOIN core.moment_type_capability mtc
         ON mtc.moment_type_id = mt.moment_type_id AND mtc.status = 'ACTIVE'
       JOIN core.capability c
         ON c.capability_id = mtc.capability_id AND c.status = 'ACTIVE'
       WHERE mt.domain_code = 'BUSINESS'
         AND mt.status = 'ACTIVE'
         AND mt.code = ANY($1::text[])
         AND c.code = ANY($2::text[])`,
      [OPERATING_TYPES, REQUIRED_CAPS]
    );

    const byType = new Map<string, Set<string>>();
    for (const type of OPERATING_TYPES) byType.set(type, new Set());
    for (const row of rows.rows) {
      byType.get(row.moment_type)?.add(row.capability);
    }

    for (const type of OPERATING_TYPES) {
      const mapped = byType.get(type) ?? new Set();
      for (const cap of REQUIRED_CAPS) {
        assert.ok(
          mapped.has(cap),
          `${type} missing capability map: ${cap}`
        );
      }
    }
  });
});
