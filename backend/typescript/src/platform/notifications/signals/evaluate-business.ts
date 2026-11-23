import type { Pool } from 'pg';
import type { DerivedNotificationSignal } from './types';

/**
 * Phase 1A: Business V4 tear-down — business derived signals disabled.
 */
export async function evaluateBusinessSignals(_pool: Pool): Promise<DerivedNotificationSignal[]> {
  return [];
}
