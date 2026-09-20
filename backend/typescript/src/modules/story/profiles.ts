/** Family-specific Moment Story composer profiles. */

export type StoryFamilyProfile =
  | 'SHARED_EXPERIENCE'
  | 'HOUSE_PARTY'
  | 'TRIP'
  | 'WEDDING'
  | 'SHARED_PURCHASE'
  | 'SHARED_LIVING';

export type StoryChapterId = 'cover' | 'alive' | 'money' | 'memories' | 'close';

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
}): StoryFamilyProfile {
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

export const STORY_CHAPTER_ORDER: StoryChapterId[] = ['cover', 'alive', 'money', 'memories', 'close'];
