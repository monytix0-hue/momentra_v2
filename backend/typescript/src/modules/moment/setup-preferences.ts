import { z } from 'zod';
import { AppError, ErrorCode } from '../../platform/errors/errors';
import {
  BUSINESS_SETUP_CATALOG,
  type BusinessSetupFamilyCode,
} from '../business/setup-service';
import {
  PERSONAL_SETUP_CATALOG,
  type PersonalSetupSystemCode,
} from '../personal/setup-service';
import { TRAVEL_CURRENCY_CODES, isTravelCurrencyCode } from '../finance/travel-currencies';

/** Preference keys that must be string[]. */
const ARRAY_KEYS = new Set<string>([]);

/** Preference keys that must be boolean (Figma weekly remind toggles). */
const BOOLEAN_KEYS = new Set([
  'reflectWeekly',
  'remindWeekly',
]);

function allowedKeysForCatalog(defaults: Record<string, unknown>): Set<string> {
  return new Set(Object.keys(defaults));
}

function validatePreferenceValue(key: string, value: unknown): void {
  if (BOOLEAN_KEYS.has(key)) {
    if (typeof value !== 'boolean') {
      throw new AppError(
        ErrorCode.VALIDATION_FAILED,
        `preferences.${key} must be a boolean.`,
        400
      );
    }
    return;
  }
  if (ARRAY_KEYS.has(key)) {
    if (!Array.isArray(value) || value.some((v) => typeof v !== 'string')) {
      throw new AppError(
        ErrorCode.VALIDATION_FAILED,
        `preferences.${key} must be an array of strings.`,
        400
      );
    }
    return;
  }
  if (typeof value !== 'string') {
    throw new AppError(
      ErrorCode.VALIDATION_FAILED,
      `preferences.${key} must be a string.`,
      400
    );
  }
}

export function validateAndMergePersonalPreferences(
  systemCode: PersonalSetupSystemCode,
  input: Record<string, unknown> | undefined
): Record<string, unknown> {
  const catalog = PERSONAL_SETUP_CATALOG.find((s) => s.systemCode === systemCode);
  if (!catalog) {
    throw new AppError(ErrorCode.VALIDATION_FAILED, `Unknown personal setup: ${systemCode}`, 400);
  }
  const allowed = allowedKeysForCatalog(catalog.defaultPreferences);
  const prefs = input ?? {};
  for (const key of Object.keys(prefs)) {
    if (!allowed.has(key)) {
      throw new AppError(ErrorCode.VALIDATION_FAILED, `Unknown preference key: ${key}`, 400);
    }
    validatePreferenceValue(key, prefs[key]);
  }
  return { ...catalog.defaultPreferences, ...prefs };
}

export function validateAndMergeBusinessPreferences(
  familyCode: BusinessSetupFamilyCode,
  input: Record<string, unknown> | undefined
): Record<string, unknown> {
  const catalog = BUSINESS_SETUP_CATALOG.find((s) => s.familyCode === familyCode);
  if (!catalog) {
    throw new AppError(ErrorCode.VALIDATION_FAILED, `Unknown business setup: ${familyCode}`, 400);
  }
  const allowed = allowedKeysForCatalog(catalog.defaultPreferences);
  const prefs = input ?? {};
  for (const key of Object.keys(prefs)) {
    if (!allowed.has(key)) {
      throw new AppError(ErrorCode.VALIDATION_FAILED, `Unknown preference key: ${key}`, 400);
    }
    if (typeof prefs[key] !== 'string' && typeof prefs[key] !== 'boolean' && typeof prefs[key] !== 'number') {
      throw new AppError(ErrorCode.VALIDATION_FAILED, `Invalid preference value for ${key}`, 400);
    }
  }
  return { ...catalog.defaultPreferences, ...prefs };
}

export const personalSetupBlockSchema = z
  .object({
    systemCode: z.enum(['LIFE_OPERATIONS', 'FUTURE_BUILDING', 'LIFESTYLE', 'RELATIONSHIPS']),
    preferences: z.record(z.string(), z.unknown()).optional(),
  })
  .strict();

export const businessSetupBlockSchema = z
  .object({
    familyCode: z.enum(['TEAM_OPERATIONS', 'BUSINESS_RUNWAY', 'BUSINESS_OPERATIONS']),
    preferences: z.record(z.string(), z.unknown()).optional(),
  })
  .strict();

const travelCurrencySchema = z
  .string()
  .length(3)
  .regex(/^[A-Z]{3}$/)
  .refine((c) => isTravelCurrencyCode(c), { message: 'Unsupported travel currency code' });

export const groupSetupPlaceSchema = z
  .object({
    label: z.string().min(1).max(500),
    startAt: z.string().datetime().nullish(),
    endAt: z.string().datetime().nullish(),
  })
  .strict()
  .superRefine((row, ctx) => {
    if (row.startAt && row.endAt && row.endAt < row.startAt) {
      ctx.addIssue({ code: 'custom', message: 'place endAt must be >= startAt', path: ['endAt'] });
    }
  });

export const groupSetupBudgetSchema = z
  .object({
    currencyCode: travelCurrencySchema,
    amount: z.string().regex(/^\d+(\.\d{1,4})?$/),
    isPrimary: z.boolean().optional(),
  })
  .strict();

export const groupSetupBlockSchema = z
  .object({
    /** @deprecated Prefer budgets[]; kept for backward compatibility. */
    budgetAmount: z.string().regex(/^\d+(\.\d{1,4})?$/).optional(),
    /** @deprecated Prefer budgets[]. */
    budgetCurrencyCode: travelCurrencySchema.optional(),
    destinationText: z.string().max(500).optional(),
    places: z.array(groupSetupPlaceSchema).max(30).optional(),
    budgets: z.array(groupSetupBudgetSchema).max(20).optional(),
    multiCurrencyEnabled: z.boolean().optional(),
    splitStyle: z.enum(['EQUAL', 'PERCENTAGE', 'EXACT', 'SHARES', 'POOLED']).optional(),
    primaryGoal: z.string().max(500).optional(),
    reminderPreferences: z
      .object({
        billReminders: z.boolean().optional(),
        choreReminders: z.boolean().optional(),
        expenseReminders: z.boolean().optional(),
        photoReminders: z.boolean().optional(),
        paymentReminders: z.boolean().optional(),
      })
      .strict()
      .optional(),
    setupPreferences: z.record(z.string(), z.unknown()).optional(),
  })
  .strict()
  .superRefine((body, ctx) => {
    const hasLegacy = Boolean(body.budgetAmount && body.budgetCurrencyCode);
    const hasBudgets = Boolean(body.budgets && body.budgets.length > 0);
    if (!hasLegacy && !hasBudgets) {
      ctx.addIssue({
        code: 'custom',
        message: 'Provide budgets[] or budgetAmount+budgetCurrencyCode.',
        path: ['budgets'],
      });
    }
    if (hasBudgets && body.budgets) {
      const codes = body.budgets.map((b) => b.currencyCode);
      if (new Set(codes).size !== codes.length) {
        ctx.addIssue({ code: 'custom', message: 'Duplicate currency in budgets.', path: ['budgets'] });
      }
      const primaries = body.budgets.filter((b) => b.isPrimary);
      if (primaries.length > 1) {
        ctx.addIssue({ code: 'custom', message: 'At most one primary budget.', path: ['budgets'] });
      }
    }
  });

export { TRAVEL_CURRENCY_CODES };

export function resolvePersonalSetupTitle(
  systemCode: PersonalSetupSystemCode,
  titleOverride?: string
): string {
  const catalog = PERSONAL_SETUP_CATALOG.find((s) => s.systemCode === systemCode)!;
  return titleOverride ?? catalog.defaultTitle;
}

export function resolvePersonalSetupMomentType(
  systemCode: PersonalSetupSystemCode,
  momentTypeOverride?: string
): string {
  const catalog = PERSONAL_SETUP_CATALOG.find((s) => s.systemCode === systemCode)!;
  return momentTypeOverride ?? catalog.defaultMomentTypeCode;
}

export function resolveBusinessSetupTitle(
  familyCode: BusinessSetupFamilyCode,
  titleOverride?: string
): string {
  const catalog = BUSINESS_SETUP_CATALOG.find((s) => s.familyCode === familyCode)!;
  return titleOverride ?? catalog.defaultTitle;
}

export function resolveBusinessSetupMomentType(
  familyCode: BusinessSetupFamilyCode,
  momentTypeOverride?: string
): string {
  const catalog = BUSINESS_SETUP_CATALOG.find((s) => s.familyCode === familyCode)!;
  return momentTypeOverride ?? catalog.defaultMomentTypeCode;
}

/** Normalize groupSetup to a budgets list (legacy or new). */
export function normalizeGroupSetupBudgets(
  groupSetup: z.infer<typeof groupSetupBlockSchema>
): Array<{ currencyCode: string; amount: string; isPrimary: boolean }> {
  if (groupSetup.budgets && groupSetup.budgets.length > 0) {
    const hasPrimary = groupSetup.budgets.some((b) => b.isPrimary);
    return groupSetup.budgets.map((b, i) => ({
      currencyCode: b.currencyCode,
      amount: b.amount,
      isPrimary: hasPrimary ? Boolean(b.isPrimary) : i === 0,
    }));
  }
  return [
    {
      currencyCode: groupSetup.budgetCurrencyCode!,
      amount: groupSetup.budgetAmount!,
      isPrimary: true,
    },
  ];
}
