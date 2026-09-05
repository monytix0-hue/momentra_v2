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

export const groupSetupBlockSchema = z
  .object({
    budgetAmount: z.string().regex(/^\d+(\.\d{1,4})?$/),
    budgetCurrencyCode: z.string().length(3).regex(/^[A-Z]{3}$/),
    destinationText: z.string().max(500).optional(),
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
  })
  .strict();

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
