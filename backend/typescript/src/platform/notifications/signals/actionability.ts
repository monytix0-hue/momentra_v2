/** Actionability scoring for derived signals (0–100). Deterministic heuristics only. */

export function scoreActionability(input: {
  importance: 'LOW' | 'NORMAL' | 'HIGH';
  hasClearNextAction: boolean;
  amountImpact?: number | null;
  ageHours?: number | null;
  thresholdProximity?: number | null;
}): number {
  let score = 20;
  if (input.importance === 'HIGH') score += 35;
  else if (input.importance === 'NORMAL') score += 20;
  else score += 5;

  if (input.hasClearNextAction) score += 25;

  if (input.amountImpact != null && Number.isFinite(input.amountImpact)) {
    // Log-ish scale: ₹100 → +5, ₹10k → +15, ₹1L+ → +20
    const a = Math.abs(input.amountImpact);
    if (a >= 100_000) score += 20;
    else if (a >= 10_000) score += 15;
    else if (a >= 1_000) score += 10;
    else if (a >= 100) score += 5;
  }

  if (input.ageHours != null && input.ageHours >= 48) score += 10;
  else if (input.ageHours != null && input.ageHours >= 24) score += 5;

  if (input.thresholdProximity != null) {
    // 0 = at threshold, 1 = far; closer → higher
    score += Math.round((1 - Math.min(1, Math.max(0, input.thresholdProximity))) * 10);
  }

  return Math.max(0, Math.min(100, score));
}

/** Bump importance when actionability is very high. */
export function importanceFromScore(
  base: 'LOW' | 'NORMAL' | 'HIGH',
  score: number
): 'LOW' | 'NORMAL' | 'HIGH' {
  if (base === 'HIGH') return 'HIGH';
  if (score >= 75) return 'HIGH';
  if (score >= 45) return base === 'LOW' ? 'NORMAL' : base;
  return base;
}
