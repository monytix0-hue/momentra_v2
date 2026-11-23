/**
 * Business V4 catalog surface — re-exports operating families/subtypes and Quick Add registries.
 * Industries share one catalog; phase delivery gates which subtypes clients create.
 */
export {
  OPERATING_FAMILIES,
  isOperatingFamily,
  isLegacyBusinessFamily,
  normalizeOperatingPreferences,
  projectOperatingMoment,
} from './operating-catalog';

export { BUSINESS_SETUP_CATALOG, BUSINESS_SETUP_FAMILY_CODES, getSetupCatalog } from './setup-service';

/** Delivery phase for each operating subtype (B0 freeze). */
export const SUBTYPE_DELIVERY_PHASE: Record<string, string> = {
  TRADING_WHOLESALE: 'B3',
  DISTRIBUTION_DEALERSHIP: 'B4',
  CLIENT_CAMPAIGN: 'B5',
  OWN_BUSINESS_CAMPAIGN: 'B5',
  RETAIL_STORE: 'B6',
  RESTAURANT_FOOD: 'B6',
  CONSUMER_SERVICE: 'B6',
  STANDARD_PRODUCTION: 'B7',
  MADE_TO_ORDER: 'B7',
};
