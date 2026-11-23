import { RETAIL_STORY_CHAPTERS, getStoryComposer } from './profiles';
import type { StorySnapshot } from './snapshot';

/** Phase 1A: Business V4 tear-down — empty story builder. */
export interface RetailStoryInput {
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

export function composeRetailStory(input: RetailStoryInput): StorySnapshot {
  const composer = getStoryComposer('RETAIL_FOOD_CONSUMER');
  return {
    identity: {
      momentId: input.momentId,
      title: input.title,
      momentTypeCode: null,
      groupFamily: null,
      familyProfile: 'RETAIL_FOOD_CONSUMER',
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
    chapters: [...RETAIL_STORY_CHAPTERS],
    display: composer,
  };
}
