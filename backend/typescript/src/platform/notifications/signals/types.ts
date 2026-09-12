import type { NotificationCategory, NotificationPriority } from '../allowlist';

export type SignalContextType = 'PERSONAL' | 'GROUP' | 'BUSINESS';

export type DerivedNotificationSignal = {
  signalName: string;
  userId: string;
  contextType: SignalContextType;
  contextId?: string;
  momentId?: string;
  companyId?: string;
  category: NotificationCategory;
  importance: NotificationPriority;
  /** Deterministic facts from canonical projections — never invent finance here. */
  facts: Record<string, unknown>;
  dedupeKey: string;
  explanationCode: string;
  observedAt: Date;
  validUntil?: Date;
  /** 0–100; higher = more actionable (Wave 2 scoring). */
  actionabilityScore: number;
  /** Optional hysteresis payload stored with the claim (e.g. lastThreshold: 80). */
  hysteresisState?: Record<string, unknown>;
};

export type SignalEmitResult = {
  attempted: number;
  emitted: number;
  suppressed: number;
};
