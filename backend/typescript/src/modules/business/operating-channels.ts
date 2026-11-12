/**
 * Business-specific default channels (corrections BS-01 / BS-02).
 * Clients may still store custom channel names; these are the suggested picker seeds.
 */

export type ChannelBusinessFamily =
  | 'TRADING'
  | 'DISTRIBUTION'
  | 'RETAIL'
  | 'MANUFACTURING'
  | 'RESTAURANT'
  | 'CONSUMER_SERVICE'
  | 'MARKETING';

export const OPERATING_CHANNEL_DEFAULTS: Record<ChannelBusinessFamily, string[]> = {
  TRADING: ['Wholesale', 'Retail', 'B2B Direct', 'Online', 'Distributor', 'Other'],
  DISTRIBUTION: [
    'General Trade',
    'Modern Trade',
    'Wholesale',
    'Institutional',
    'Direct Distribution',
    'Van Sales',
    'Other',
  ],
  RETAIL: ['In-store', 'Online', 'Marketplace', 'Social Commerce', 'Other'],
  MANUFACTURING: ['Direct B2B', 'Distributor', 'Dealer Network', 'Export', 'Online', 'Other'],
  RESTAURANT: ['Dine-in', 'Takeaway', 'Own Delivery', 'Aggregator', 'Catering', 'Other'],
  CONSUMER_SERVICE: ['Walk-in', 'Appointment', 'Online', 'On-site', 'Referral', 'Other'],
  MARKETING: [
    'Paid Social',
    'Search Ads',
    'Display / Video Ads',
    'Organic Social',
    'Email',
    'Influencer',
    'SEO / Content',
    'SMS / Messaging',
  ],
};

export function defaultChannelsForSubtype(subtype: string | null | undefined): string[] {
  const code = String(subtype ?? '').toUpperCase();
  switch (code) {
    case 'TRADING_WHOLESALE':
      return OPERATING_CHANNEL_DEFAULTS.TRADING;
    case 'DISTRIBUTION_DEALERSHIP':
      return OPERATING_CHANNEL_DEFAULTS.DISTRIBUTION;
    case 'RETAIL_STORE':
      return OPERATING_CHANNEL_DEFAULTS.RETAIL;
    case 'STANDARD_PRODUCTION':
    case 'MADE_TO_ORDER':
      return OPERATING_CHANNEL_DEFAULTS.MANUFACTURING;
    case 'RESTAURANT_FOOD':
      return OPERATING_CHANNEL_DEFAULTS.RESTAURANT;
    case 'CONSUMER_SERVICE':
      return OPERATING_CHANNEL_DEFAULTS.CONSUMER_SERVICE;
    case 'CLIENT_CAMPAIGN':
    case 'OWN_BUSINESS_CAMPAIGN':
      return OPERATING_CHANNEL_DEFAULTS.MARKETING;
    default:
      return [];
  }
}

/** Shared unit master (corrections BS-04). Packaging conversions stay item-specific. */
export type UnitCategory = 'COUNT' | 'MASS' | 'VOLUME' | 'LENGTH' | 'AREA';

export interface UnitMasterEntry {
  code: string;
  label: string;
  category: UnitCategory;
  symbol?: string;
}

export const UNIT_MASTER: UnitMasterEntry[] = [
  { code: 'PC', label: 'Piece (pc)', category: 'COUNT', symbol: 'pc' },
  { code: 'PAIR', label: 'Pair', category: 'COUNT' },
  { code: 'SET', label: 'Set', category: 'COUNT' },
  { code: 'PACK', label: 'Pack', category: 'COUNT' },
  { code: 'BOX', label: 'Box', category: 'COUNT' },
  { code: 'CARTON', label: 'Carton', category: 'COUNT' },
  { code: 'DOZEN', label: 'Dozen (12 pc)', category: 'COUNT' },
  { code: 'KG', label: 'Kilogram (kg)', category: 'MASS', symbol: 'kg' },
  { code: 'G', label: 'Gram (g)', category: 'MASS', symbol: 'g' },
  { code: 'TONNE', label: 'Tonne', category: 'MASS' },
  { code: 'L', label: 'Litre (L)', category: 'VOLUME', symbol: 'L' },
  { code: 'ML', label: 'Millilitre (mL)', category: 'VOLUME', symbol: 'mL' },
  { code: 'M', label: 'Metre (m)', category: 'LENGTH', symbol: 'm' },
  { code: 'CM', label: 'Centimetre (cm)', category: 'LENGTH', symbol: 'cm' },
  { code: 'FT', label: 'Foot', category: 'LENGTH', symbol: 'ft' },
  { code: 'SQFT', label: 'Square Foot (sq ft)', category: 'AREA', symbol: 'sq ft' },
  { code: 'SQM', label: 'Square Metre (sq m)', category: 'AREA', symbol: 'sq m' },
];

export const UNIT_CATEGORY_LABELS: Record<UnitCategory, string> = {
  COUNT: 'Count',
  MASS: 'Mass',
  VOLUME: 'Volume',
  LENGTH: 'Length',
  AREA: 'Area',
};

export function unitsForCategory(category: UnitCategory): UnitMasterEntry[] {
  return UNIT_MASTER.filter((unit) => unit.category === category);
}

export function resolveUnitLabel(codeOrLabel: string | null | undefined): string | null {
  const raw = String(codeOrLabel ?? '').trim();
  if (!raw) return null;
  const upper = raw.toUpperCase();
  const hit = UNIT_MASTER.find(
    (unit) =>
      unit.code === upper ||
      unit.label.toUpperCase() === upper ||
      (unit.symbol && unit.symbol.toUpperCase() === upper)
  );
  return hit?.label ?? raw;
}
