/**
 * Structured Lifestyle eligibility for Master Expense.
 * Driven by finance.expense_category / subcategory codes (V045) — not description keywords.
 */

const LIFESTYLE_YES_SUBCATEGORIES = new Set([
  'DINING_OUT',
  'FOOD_DINING',
  'TAKEAWAY',
  'COFFEE',
  'CELEBRATIONS',
]);

const LIFESTYLE_NO_SUBCATEGORIES = new Set(['GROCERIES']);

const LIFESTYLE_YES_CATEGORIES = new Set(['CAFE', 'ENTERTAINMENT']);

const LIFESTYLE_NO_CATEGORIES = new Set([
  'TRANSPORT',
  'HOUSING',
  'BILLS',
  'SHOPPING',
  'OTHER',
  'HEALTH',
]);

/** Ambiguous codes (documented; treated as NO unless subcategory upgrades). */
export const LIFESTYLE_AMBIGUOUS_CATEGORIES = ['HEALTH', 'SHOPPING', 'TRANSPORT', 'OTHER'] as const;

export function isLifestyleEligible(
  categoryCode: string | null | undefined,
  subcategoryCode: string | null | undefined
): boolean {
  const cat = categoryCode?.trim().toUpperCase() || null;
  const sub = subcategoryCode?.trim().toUpperCase() || null;

  if (sub && LIFESTYLE_NO_SUBCATEGORIES.has(sub)) return false;
  if (sub && LIFESTYLE_YES_SUBCATEGORIES.has(sub)) return true;

  if (cat && LIFESTYLE_YES_CATEGORIES.has(cat)) return true;
  if (cat && LIFESTYLE_NO_CATEGORIES.has(cat)) return false;

  // FOOD without groceries subcategory: dining-like by default (matches Master Expense FOOD default).
  if (cat === 'FOOD') return true;

  return false;
}

/** Human-readable mapping rows for verification reports. */
export function lifestyleEligibilityCatalog(): Array<{
  category: string;
  subcategory: string | null;
  lifestyle: boolean;
  rule: string;
}> {
  return [
    { category: 'FOOD', subcategory: 'DINING_OUT', lifestyle: true, rule: 'dining subcategory' },
    { category: 'FOOD', subcategory: 'FOOD_DINING', lifestyle: true, rule: 'dining subcategory' },
    { category: 'FOOD', subcategory: 'TAKEAWAY', lifestyle: true, rule: 'dining subcategory' },
    { category: 'FOOD', subcategory: 'COFFEE', lifestyle: true, rule: 'dining subcategory' },
    { category: 'FOOD', subcategory: 'CELEBRATIONS', lifestyle: true, rule: 'dining subcategory' },
    { category: 'FOOD', subcategory: 'GROCERIES', lifestyle: false, rule: 'groceries exclusion' },
    { category: 'FOOD', subcategory: null, lifestyle: true, rule: 'FOOD default without GROCERIES' },
    { category: 'CAFE', subcategory: null, lifestyle: true, rule: 'category allowlist' },
    { category: 'ENTERTAINMENT', subcategory: null, lifestyle: true, rule: 'category allowlist' },
    { category: 'HEALTH', subcategory: null, lifestyle: false, rule: 'ambiguous → NO' },
    { category: 'TRANSPORT', subcategory: null, lifestyle: false, rule: 'ops spend' },
    { category: 'HOUSING', subcategory: null, lifestyle: false, rule: 'ops spend' },
    { category: 'BILLS', subcategory: null, lifestyle: false, rule: 'ops spend' },
    { category: 'SHOPPING', subcategory: null, lifestyle: false, rule: 'ambiguous → NO' },
    { category: 'OTHER', subcategory: null, lifestyle: false, rule: 'ambiguous → NO' },
  ];
}
