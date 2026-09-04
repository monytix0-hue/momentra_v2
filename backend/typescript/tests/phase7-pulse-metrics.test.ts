import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import { describe, it } from 'node:test';
import { PHASE7_PULSE_METRIC_CODES } from '../src/modules/analytics/engine';

const repoRoot = join(__dirname, '../../..');

describe('Phase 7 Pulse metric curation', () => {
  it('engine codes match PERSONAL_PHASE7_PULSE_METRIC_MAP.json', () => {
    const raw = readFileSync(
      join(repoRoot, 'docs/implementation/PERSONAL_PHASE7_PULSE_METRIC_MAP.json'),
      'utf8',
    );
    const map = JSON.parse(raw) as { pulseMetrics: Array<{ metricCode: string }> };
    const fromMap = new Set(map.pulseMetrics.map((m) => m.metricCode));
    for (const code of PHASE7_PULSE_METRIC_CODES) {
      assert.ok(fromMap.has(code), `missing map entry for ${code}`);
    }
    for (const code of fromMap) {
      assert.ok(
        (PHASE7_PULSE_METRIC_CODES as readonly string[]).includes(code),
        `engine missing ${code}`,
      );
    }
  });

  it('V058 migration is registered in MIGRATION_ORDER', () => {
    const order = readFileSync(join(repoRoot, 'frds/manifest/MIGRATION_ORDER.txt'), 'utf8');
    assert.match(order, /V058__personal_phase7_pulse_metric_freeze\.sql/);
  });
});
