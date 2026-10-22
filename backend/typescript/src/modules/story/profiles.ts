/** Family-specific Moment Story composer profiles. */

export type StoryFamilyProfile =
  | 'SHARED_EXPERIENCE'
  | 'HOUSE_PARTY'
  | 'TRIP'
  | 'WEDDING'
  | 'SHARED_PURCHASE'
  | 'SHARED_LIVING'
  | 'MARKETING_CAMPAIGN'
  | 'RETAIL_FOOD_CONSUMER'
  | 'MANUFACTURING_MADE_TO_ORDER';

export type StoryChapterId =
  | 'cover'
  | 'moment'
  | 'together'
  | 'money'
  | 'close'
  | 'alive'
  | 'memories'
  | 'mc-moment'
  | 'mc-motion'
  | 'mc-results'
  | 'mc-money'
  | 'mc-channels'
  | 'mc-people'
  | 'mc-attention'
  | 'mc-sofar'
  | 'rf-moment'
  | 'rf-motion'
  | 'rf-revenue'
  | 'rf-money'
  | 'rf-operations'
  | 'rf-people'
  | 'rf-attention'
  | 'rf-sofar'
  | 'mf-moment'
  | 'mf-motion'
  | 'mf-output'
  | 'mf-money'
  | 'mf-materials'
  | 'mf-people'
  | 'mf-attention'
  | 'mf-sofar';

export interface StoryMetricDef {
  key: string;
  label: string;
}

export interface StoryFamilyComposer {
  profile: StoryFamilyProfile;
  displayLabel: string;
  coverEyebrow: string;
  closeLine: string;
  metricKeys: StoryMetricDef[];
  moneyCategoriesHint: string[];
  omitPlaces: boolean;
  shareBlurb: (title: string, metrics: Record<string, string | number>) => string;
}

const EXPERIENCE_BASE: Omit<StoryFamilyComposer, 'profile' | 'displayLabel' | 'coverEyebrow' | 'closeLine' | 'shareBlurb'> = {
  metricKeys: [
    { key: 'people', label: 'People' },
    { key: 'days', label: 'Days' },
    { key: 'plans', label: 'Plans' },
    { key: 'decisions', label: 'Decisions' },
    { key: 'photos', label: 'Photos' },
    { key: 'spent', label: 'Spent' },
  ],
  moneyCategoriesHint: ['Travel', 'Stay', 'Food', 'Activities', 'Other'],
  omitPlaces: false,
};

export function resolveStoryFamilyProfile(input: {
  groupFamily?: string | null;
  momentTypeCode?: string | null;
  businessFamily?: string | null;
}): StoryFamilyProfile {
  if ((input.businessFamily ?? '').toUpperCase() === 'MARKETING_CAMPAIGNS') return 'MARKETING_CAMPAIGN';
  if ((input.businessFamily ?? '').toUpperCase() === 'RETAIL_CONSUMER') return 'RETAIL_FOOD_CONSUMER';
  if ((input.businessFamily ?? '').toUpperCase() === 'MANUFACTURING_PRODUCTION') return 'MANUFACTURING_MADE_TO_ORDER';
  const type = (input.momentTypeCode ?? '').toUpperCase();
  const family = (input.groupFamily ?? '').toUpperCase();
  if (type === 'HOUSE_PARTY') return 'HOUSE_PARTY';
  if (type === 'TRIP') return 'TRIP';
  if (type === 'WEDDING') return 'WEDDING';
  if (family === 'SHARED_PURCHASE' || ['GROUP_PURCHASE', 'GIFT_POOL', 'SHARED_ASSET', 'COMMUNITY_PURCHASE'].includes(type)) {
    return 'SHARED_PURCHASE';
  }
  if (
    family === 'SHARED_LIVING' ||
    ['FLATMATES', 'FAMILY_HOUSEHOLD', 'CO_LIVING', 'COMMUNITY_LIVING', 'SHARED_LIVING'].includes(type)
  ) {
    return 'SHARED_LIVING';
  }
  if (family === 'SHARED_EXPERIENCE') return 'SHARED_EXPERIENCE';
  return 'SHARED_EXPERIENCE';
}

export function getStoryComposer(profile: StoryFamilyProfile): StoryFamilyComposer {
  switch (profile) {
    case 'HOUSE_PARTY':
      return {
        profile,
        displayLabel: 'House Party Story',
        coverEyebrow: 'Celebration',
        closeLine: 'The night is saved.',
        metricKeys: [
          { key: 'people', label: 'Guests' },
          { key: 'hours', label: 'Hours' },
          { key: 'plans', label: 'Plans' },
          { key: 'decisions', label: 'Decisions' },
          { key: 'photos', label: 'Photos' },
          { key: 'spent', label: 'Spent' },
        ],
        moneyCategoriesHint: ['Food', 'Drinks', 'Decor', 'Supplies', 'Other'],
        omitPlaces: true,
        shareBlurb: (title, m) =>
          `${title} — ${m.people ?? '?'} guests, ${m.photos ?? 0} photos. Relive it on Momentra.`,
      };
    case 'SHARED_PURCHASE':
      return {
        profile,
        displayLabel: 'Shared Purchase Story',
        coverEyebrow: 'We bought this together',
        closeLine: 'Shared & sorted.',
        metricKeys: [
          { key: 'people', label: 'Contributors' },
          { key: 'target', label: 'Target' },
          { key: 'raised', label: 'Raised' },
          { key: 'spent', label: 'Purchased' },
          { key: 'remaining', label: 'Remaining' },
          { key: 'photos', label: 'Photos' },
        ],
        moneyCategoriesHint: ['Item', 'Shipping', 'Tax', 'Other'],
        omitPlaces: true,
        shareBlurb: (title, m) =>
          `${title} — pooled ${m.raised ?? m.spent ?? 'together'}. See the Story on Momentra.`,
      };
    case 'SHARED_LIVING':
      return {
        profile,
        displayLabel: 'Living Chapter Story',
        coverEyebrow: 'Living chapter',
        closeLine: 'This chapter of home, together.',
        metricKeys: [
          { key: 'people', label: 'Housemates' },
          { key: 'months', label: 'Months' },
          { key: 'bills', label: 'Bills' },
          { key: 'decisions', label: 'Decisions' },
          { key: 'photos', label: 'Photos' },
          { key: 'spent', label: 'Shared' },
        ],
        moneyCategoriesHint: ['Rent', 'Groceries', 'Utilities', 'Internet', 'Other'],
        omitPlaces: true,
        shareBlurb: (title, m) =>
          `${title} — ${m.months ?? '?'} months with ${m.people ?? '?'} housemates. Momentra Story.`,
      };
    case 'WEDDING':
      return {
        ...EXPERIENCE_BASE,
        profile,
        displayLabel: 'Wedding Story',
        coverEyebrow: 'Wedding',
        closeLine: 'A celebration to keep.',
        moneyCategoriesHint: ['Venue', 'Catering', 'Decor', 'Photography', 'Other'],
        shareBlurb: (title, m) =>
          `${title} — ${m.people ?? '?'} people, ${m.photos ?? 0} photos. Momentra Story.`,
      };
    case 'TRIP':
      return {
        ...EXPERIENCE_BASE,
        profile,
        displayLabel: 'Trip Story',
        coverEyebrow: 'Trip',
        closeLine: 'Life happens in moments.',
        shareBlurb: (title, m) =>
          `${title} — ${m.days ?? '?'} days, ${m.people ?? '?'} people. Relive it on Momentra.`,
      };
    case 'MARKETING_CAMPAIGN':
      return {
        profile,
        displayLabel: 'Campaign Story',
        coverEyebrow: 'Marketing campaign',
        closeLine: 'The campaign, as recorded.',
        metricKeys: [
          { key: 'results', label: 'Results' },
          { key: 'spent', label: 'Spent' },
          { key: 'channels', label: 'Channels' },
          { key: 'open', label: 'Open' },
        ],
        moneyCategoriesHint: ['Media', 'Production', 'Influencer', 'Events', 'Agency', 'Tools', 'Other'],
        omitPlaces: true,
        shareBlurb: (title, m) => `${title} — campaign story. Results ${m.results ?? 'n/a'}. Momentra.`,
      };
    case 'RETAIL_FOOD_CONSUMER':
      return {
        profile,
        displayLabel: 'Business Story',
        coverEyebrow: 'Retail, food, and services',
        closeLine: 'The business, as recorded.',
        metricKeys: [
          { key: 'revenue', label: 'Revenue' },
          { key: 'receivable', label: 'To collect' },
          { key: 'cash', label: 'Cash' },
          { key: 'sales', label: 'Charges' },
        ],
        moneyCategoriesHint: [],
        omitPlaces: true,
        shareBlurb: (title, m) => `${title} — business story. Revenue ${m.revenue ?? 'not recorded'}. Momentra.`,
      };
    case 'MANUFACTURING_MADE_TO_ORDER':
      return {
        profile,
        displayLabel: 'Manufacturing Story',
        coverEyebrow: 'Manufacturing and made-to-order',
        closeLine: 'The factory, as recorded.',
        metricKeys: [
          { key: 'sales', label: 'Net sales' },
          { key: 'orders', label: 'Orders' },
          { key: 'accepted', label: 'Accepted' },
          { key: 'recorded', label: 'Recorded output' },
        ],
        moneyCategoriesHint: [],
        omitPlaces: true,
        shareBlurb: (title, m) => `${title} — manufacturing story. Sales ${m.sales ?? 'not recorded'}. Momentra.`,
      };
    default:
      return {
        ...EXPERIENCE_BASE,
        profile: 'SHARED_EXPERIENCE',
        displayLabel: 'Moment Story',
        coverEyebrow: 'Shared experience',
        closeLine: 'Life happens in moments.',
        shareBlurb: (title, m) =>
          `${title} — ${m.people ?? '?'} people. See the full Story on Momentra.`,
      };
  }
}

export const STORY_CHAPTER_ORDER: StoryChapterId[] = ['cover', 'moment', 'together', 'money', 'close'];

export const MARKETING_STORY_CHAPTERS: StoryChapterId[] = [
  'mc-moment',
  'mc-motion',
  'mc-results',
  'mc-money',
  'mc-channels',
  'mc-people',
  'mc-attention',
  'mc-sofar',
];

export const RETAIL_STORY_CHAPTERS: StoryChapterId[] = [
  'rf-moment',
  'rf-motion',
  'rf-revenue',
  'rf-money',
  'rf-operations',
  'rf-people',
  'rf-attention',
  'rf-sofar',
];

export const MANUFACTURING_STORY_CHAPTERS: StoryChapterId[] = [
  'mf-moment',
  'mf-motion',
  'mf-output',
  'mf-money',
  'mf-materials',
  'mf-people',
  'mf-attention',
  'mf-sofar',
];
