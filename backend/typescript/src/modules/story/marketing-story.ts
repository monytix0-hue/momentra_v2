import { MARKETING_STORY_CHAPTERS, getStoryComposer } from './profiles';
import type { StorySnapshot } from './snapshot';

/** Phase 1A: Business V4 tear-down — empty story builder. */
export interface MarketingStoryInput {
  momentId: string;
  title: string;
  subtype?: string;
  currency?: string;
  startAt?: string | null;
  endAt?: string | null;
  channels?: string[];
  participants?: string[];
  events?: unknown[];
  [key: string]: unknown;
}

export function composeMarketingStory(input: MarketingStoryInput): StorySnapshot {
  const composer = getStoryComposer('MARKETING_CAMPAIGN');
  return {
    identity: {
      momentId: input.momentId,
      title: input.title,
      momentTypeCode: null,
      groupFamily: null,
      familyProfile: 'MARKETING_CAMPAIGN',
      startAt: null,
      endAt: null,
      completedAt: null,
      currencyCode: String(input.currency ?? 'INR'),
    },
    people: [],
    metrics: {},
    timeline: [],
    money: {
      contributed: 0,
      spent: 0,
      remaining: 0,
      unsettled: 0,
      target: null,
      categories: [],
      contributors: [],
      payers: [],
      expenses: [],
    },
    places: [],
    decisions: [],
    memories: [],
    photos: [],
    narrative: { opening: input.title, insights: [] },
    chapters: [...MARKETING_STORY_CHAPTERS],
    display: composer,
  };
}
