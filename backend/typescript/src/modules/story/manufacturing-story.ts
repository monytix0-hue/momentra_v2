import { MANUFACTURING_STORY_CHAPTERS, getStoryComposer } from './profiles';
import type { StorySnapshot } from './snapshot';

/** Phase 1A: Business V4 tear-down — empty story builder. */
export interface ManufacturingStoryInput {
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

export function composeManufacturingStory(input: ManufacturingStoryInput): StorySnapshot {
  const composer = getStoryComposer('MANUFACTURING_MADE_TO_ORDER');
  return {
    identity: {
      momentId: input.momentId,
      title: input.title,
      momentTypeCode: null,
      groupFamily: null,
      familyProfile: 'MANUFACTURING_MADE_TO_ORDER',
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
    chapters: [...MANUFACTURING_STORY_CHAPTERS],
    display: composer,
  };
}
