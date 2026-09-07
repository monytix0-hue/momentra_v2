/**
 * Rebuild Group finance projections from the expense/settlement ledger.
 *
 * Usage:
 *   npx tsx scripts/rebuild-group-finance-projections.ts
 *   npx tsx scripts/rebuild-group-finance-projections.ts --moment <uuid>
 *
 * Mandatory after net_position formula fixes so historical balances heal.
 */
import { getPool, closePool } from '../src/platform/database/pool';
import { rebuildGroupFinanceProjection } from '../src/modules/finance/group-expense';

async function main(): Promise<void> {
  const momentFlag = process.argv.indexOf('--moment');
  const momentId =
    momentFlag >= 0 && process.argv[momentFlag + 1] ? process.argv[momentFlag + 1] : null;

  const pool = getPool();
  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    let momentIds: string[] = [];
    if (momentId) {
      momentIds = [momentId];
    } else {
      const rows = await client.query<{ moment_id: string }>(
        `SELECT DISTINCT e.moment_id
         FROM finance.expense e
         INNER JOIN finance.group_expense_context g ON g.expense_id = e.expense_id
         WHERE e.domain_code = 'GROUP' AND e.status = 'POSTED'
         ORDER BY e.moment_id`
      );
      momentIds = rows.rows.map((r) => r.moment_id);
    }

    console.log(`Rebuilding finance projections for ${momentIds.length} moment(s)…`);
    for (const id of momentIds) {
      const result = await rebuildGroupFinanceProjection(client, id);
      console.log(
        `  ${id}: expenses=${result.expenseCount} settlements=${result.settlementCount} currencies=[${result.currencies.join(',')}]`
      );
    }
    await client.query('COMMIT');
    console.log('Done.');
  } catch (e) {
    await client.query('ROLLBACK');
    throw e;
  } finally {
    client.release();
    await closePool();
  }
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
