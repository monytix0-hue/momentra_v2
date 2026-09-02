/**
 * S9-QA-G/J — Offline Expected_Results / Group_Splits math from joined CSVs.
 * Does not require a device or DB. Writes docs/qa/reconciliation/*.
 *
 * Platform-divergent ratios (plan Wave 5):
 *   Android PERCENTAGE 60/25/15 · SHARES 2:1:1 · EXACT 500+400+300 (scaled)
 *   iOS     PERCENTAGE 50/30/20 · SHARES 3:2:1 · EXACT 333+333+334 (scaled)
 */
import { readFileSync, writeFileSync, mkdirSync, existsSync } from 'fs';
import path from 'path';

type Row = Record<string, string>;

function parseCsv(text: string): Row[] {
  const lines = text.split(/\r?\n/).filter(Boolean);
  if (!lines.length) return [];
  const headers = lines[0].split(',');
  return lines.slice(1).map((line) => {
    const cols: string[] = [];
    let cur = '';
    let q = false;
    for (let i = 0; i < line.length; i++) {
      const ch = line[i];
      if (ch === '"') {
        if (q && line[i + 1] === '"') {
          cur += '"';
          i++;
        } else q = !q;
      } else if (ch === ',' && !q) {
        cols.push(cur);
        cur = '';
      } else cur += ch;
    }
    cols.push(cur);
    const row: Row = {};
    headers.forEach((h, idx) => {
      row[h] = cols[idx] ?? '';
    });
    return row;
  });
}

function round2(n: number): number {
  return Math.round(n * 100) / 100;
}

function equalShares(total: number, n: number): number[] {
  const cents = Math.round(total * 100);
  const base = Math.floor(cents / n);
  const rem = cents - base * n;
  return Array.from({ length: n }, (_, i) => (base + (i < rem ? 1 : 0)) / 100);
}

function weightedShares(total: number, weights: number[]): number[] {
  const sumW = weights.reduce((a, b) => a + b, 0);
  const cents = Math.round(total * 100);
  const raw = weights.map((w) => Math.floor((cents * w) / sumW));
  let allocated = raw.reduce((a, b) => a + b, 0);
  let i = 0;
  while (allocated < cents) {
    raw[i % raw.length] += 1;
    allocated += 1;
    i += 1;
  }
  return raw.map((c) => c / 100);
}

function platformWeights(
  platform: 'android' | 'ios',
  method: string
): number[] | null {
  const m = method.toUpperCase();
  if (m === 'PERCENTAGE') {
    return platform === 'android' ? [60, 25, 15] : [50, 30, 20];
  }
  if (m === 'SHARES') {
    return platform === 'android' ? [2, 1, 1] : [3, 2, 1];
  }
  if (m === 'EXACT') {
    return platform === 'android' ? [500, 400, 300] : [333, 333, 334];
  }
  return null;
}

function computeSplits(
  platform: 'android' | 'ios',
  amount: number,
  participants: number,
  method: string
): { shares: number[]; ok: boolean; note: string } {
  const n = participants || 3;
  const m = (method || 'EQUAL').toUpperCase();
  if (m === 'EQUAL' || !m) {
    const shares = equalShares(amount, n);
    const sum = round2(shares.reduce((a, b) => a + b, 0));
    return { shares, ok: Math.abs(sum - amount) < 0.01, note: 'EQUAL remainder on first n' };
  }
  const weights = platformWeights(platform, m);
  if (!weights) {
    return { shares: [], ok: false, note: `Unknown split method ${method}` };
  }
  // Resize weights to participant count
  let w = weights.slice(0, n);
  while (w.length < n) w.push(1);
  const shares = weightedShares(amount, w);
  const sum = round2(shares.reduce((a, b) => a + b, 0));
  return {
    shares,
    ok: Math.abs(sum - amount) < 0.01,
    note: `${m} weights=${w.join(':')}`,
  };
}

function main() {
  const repoRoot = path.resolve(__dirname, '../../../..');
  const outDir = path.join(repoRoot, 'docs', 'qa', 'reconciliation');
  mkdirSync(outDir, { recursive: true });

  const platforms: Array<'android' | 'ios'> = ['android', 'ios'];
  const summary: Record<string, unknown> = {
    generatedAt: new Date().toISOString(),
    gate: 'S9-QA-J offline math',
    platforms: {} as Record<string, unknown>,
  };

  let totalFail = 0;

  for (const platform of platforms) {
    const groupPath = path.join(repoRoot, '.maestro', 'data', platform, 'group.csv');
    const personalPath = path.join(repoRoot, '.maestro', 'data', platform, 'personal.csv');
    const businessPath = path.join(repoRoot, '.maestro', 'data', platform, 'business.csv');
    if (!existsSync(groupPath)) throw new Error(`Missing ${groupPath}`);

    const groupRows = parseCsv(readFileSync(groupPath, 'utf8')).filter(
      (r) => r.join_status && !r.join_status.startsWith('SKIP')
    );
    const splitRows: Array<Record<string, unknown>> = [];
    let splitFail = 0;
    const methodCounts: Record<string, number> = {};

    for (const r of groupRows) {
      const amount = Number(r.Amount);
      const n = Number(r.Participant_Count || '3');
      const method = r.Split_Method || 'EQUAL';
      methodCounts[method] = (methodCounts[method] || 0) + 1;
      const computed = computeSplits(platform, amount, n, method);
      if (!computed.ok) splitFail += 1;
      const net = computed.shares.map((s, i) => {
        // Payer (index 0 heuristic) is owed (amount - own share); others owe their share
        // Net balances must sum to 0
        return i === 0 ? round2(amount - s) : round2(-s);
      });
      // Correct net: payer paid amount, each owes share → payer net = amount - ownShare, others = -share
      const netSum = round2(net.reduce((a, b) => a + b, 0));
      if (Math.abs(netSum) > 0.01) splitFail += 1;
      splitRows.push({
        txnId: r.Txn_ID,
        amount,
        method,
        participants: n,
        shares: computed.shares,
        shareSum: round2(computed.shares.reduce((a, b) => a + b, 0)),
        netBalances: net,
        netSum,
        ok: computed.ok && Math.abs(netSum) < 0.01,
        note: computed.note,
      });
    }

    // Sign polarity checks for personal/business money rows
    const moneyCheck = (rows: Row[], label: string) => {
      let fail = 0;
      let checked = 0;
      let transferNeutral = 0;
      for (const r of rows) {
        if (!r.join_status || r.join_status.startsWith('SKIP')) continue;
        const semantic = (r.Semantic_Type || '').toLowerCase();
        if (!['expense', 'income', 'contribution', 'settlement', 'transfer'].includes(semantic)) {
          continue;
        }
        checked += 1;
        const amount = Number(r.Amount);
        const signed = Number(r.Expected_Signed_Amount);
        // Transfers are net-neutral in the ledger (Expected_Signed_Amount = 0).
        if (semantic === 'transfer') {
          if (Math.abs(signed) > 0.02) fail += 1;
          else transferNeutral += 1;
          continue;
        }
        const expectNeg = semantic === 'expense' || semantic === 'settlement';
        const expectPos = semantic === 'income' || semantic === 'contribution';
        if (expectNeg && signed > 0 && amount > 0) fail += 1;
        if (expectPos && signed < 0 && amount > 0) fail += 1;
        if (Math.abs(Math.abs(signed) - Math.abs(amount)) > 0.02) fail += 1;
      }
      return { label, checked, fail, transferNeutral };
    };

    const personal = existsSync(personalPath)
      ? moneyCheck(parseCsv(readFileSync(personalPath, 'utf8')), 'personal')
      : { label: 'personal', checked: 0, fail: 0 };
    const business = existsSync(businessPath)
      ? moneyCheck(parseCsv(readFileSync(businessPath, 'utf8')), 'business')
      : { label: 'business', checked: 0, fail: 0 };

    writeFileSync(
      path.join(outDir, `${platform}_group_splits.json`),
      JSON.stringify({ platform, methodCounts, rows: splitRows }, null, 2),
      'utf8'
    );

    const platformFail = splitFail + personal.fail + business.fail;
    totalFail += platformFail;
    (summary.platforms as Record<string, unknown>)[platform] = {
      groupRows: groupRows.length,
      methodCounts,
      splitInvariantFails: splitFail,
      personalSign: personal,
      businessSign: business,
      ok: platformFail === 0,
    };
  }

  summary.ok = totalFail === 0;
  summary.totalFails = totalFail;
  summary.crossDeviceSyncNote =
    'Run shared Group/Business workspace on one APK + one iOS account only after per-platform PASS (isolated accounts remain default).';

  writeFileSync(path.join(outDir, 'offline-math-report.json'), JSON.stringify(summary, null, 2), 'utf8');

  const md = `# Offline math reconciliation (S9-QA-J)

Generated: ${summary.generatedAt}

| Platform | Group rows | Split fails | Personal sign fails | Business sign fails | Status |
|----------|-----------:|------------:|--------------------:|--------------------:|--------|
${platforms
  .map((p) => {
    const s = (summary.platforms as Record<string, any>)[p];
    return `| ${p} | ${s.groupRows} | ${s.splitInvariantFails} | ${s.personalSign.fail}/${s.personalSign.checked} | ${s.businessSign.fail}/${s.businessSign.checked} | ${s.ok ? 'PASS' : 'FAIL'} |`;
  })
  .join('\n')}

Artifacts: \`docs/qa/reconciliation/{android,ios}_group_splits.json\`

Device Actual_Results still required after Maestro runs (\`qa:verify-ledger-batch\`).
`;
  writeFileSync(path.join(outDir, 'OFFLINE_MATH.md'), md, 'utf8');
  console.log(JSON.stringify(summary, null, 2));
  if (totalFail > 0) process.exitCode = 1;
}

main();
