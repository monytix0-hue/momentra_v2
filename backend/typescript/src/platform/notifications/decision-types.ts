import type { DeliveryRoute } from './rate-policy';

export type NotificationCadence = 'ALL' | 'IMPORTANT' | 'DIGEST_ONLY' | 'MUTED';

export type RecipientRelationship =
  | 'actor'
  | 'payer'
  | 'payee'
  | 'splittee'
  | 'assignee'
  | 'approver'
  | 'voter'
  | 'organizer'
  | 'peer'
  | 'self'
  | 'uninvolved';

export type RecipientContext = {
  userId: string;
  relationship: RecipientRelationship;
  cadence: NotificationCadence;
  recipientShare?: string | null;
  owedToRecipient?: string | null;
  currencyCode?: string | null;
  momentTitle?: string | null;
};

export function asCadence(raw: unknown): NotificationCadence {
  const v = typeof raw === 'string' ? raw.toUpperCase() : '';
  if (v === 'IMPORTANT' || v === 'DIGEST_ONLY' || v === 'MUTED' || v === 'ALL') return v;
  return 'ALL';
}

export function cadenceFromNotifyFlag(
  notifyOnChanges: boolean,
  stored?: string | null
): NotificationCadence {
  if (!notifyOnChanges) return 'MUTED';
  return asCadence(stored ?? 'ALL');
}

export function mapCadenceToNotifyOnChanges(cadence: NotificationCadence): boolean {
  return cadence !== 'MUTED';
}

export function moneyLabel(amount: string | null | undefined, currency: string | null | undefined): string {
  if (!amount) return '';
  const n = Number(amount);
  const formatted = Number.isFinite(n)
    ? n.toLocaleString('en-IN', { maximumFractionDigits: 2 })
    : amount;
  const code = (currency ?? '').toUpperCase();
  if (code === 'INR' || code === '') return `₹${formatted}`;
  return `${code} ${formatted}`;
}

export function inferRelationship(
  eventName: string,
  recipientUserId: string,
  actorUserId: string,
  payload: Record<string, unknown>
): RecipientRelationship {
  if (recipientUserId === actorUserId) {
    if (
      payload.derivedSignal === true ||
      eventName === 'WeeklyReminder' ||
      eventName === 'DailyPersonalReminder' ||
      eventName === 'TaskDueReminder' ||
      eventName === 'BillReminder' ||
      eventName === 'ChoreReminder' ||
      eventName === 'ExpenseReminder' ||
      eventName === 'PhotoReminder' ||
      eventName === 'DigestReady' ||
      eventName === 'MomentDigestReady' ||
      eventName === 'TripBudgetThresholdReached' ||
      eventName === 'UserBalanceChangedMeaningfully' ||
      eventName === 'SettlementSuggested' ||
      eventName === 'GroupNearlySettled' ||
      eventName === 'PollNeedsYourVote' ||
      eventName === 'AssignedTaskDueSoon' ||
      eventName === 'ApprovalsAccumulating' ||
      eventName === 'ApprovalAging' ||
      eventName === 'InvoiceDueSoon' ||
      eventName === 'InvoiceOverdue' ||
      eventName === 'ExpenseThresholdExceeded' ||
      eventName === 'RunwayChangedMeaningfully' ||
      eventName === 'BudgetThresholdReached' ||
      eventName === 'BillDueSoon' ||
      eventName === 'GoalMilestoneReached' ||
      eventName === 'GoalAtRisk' ||
      eventName === 'RecurringExpenseExpected'
    ) {
      return 'self';
    }
    return 'actor';
  }

  const payerUserId = typeof payload.payerUserId === 'string' ? payload.payerUserId : null;
  const payeeUserId = typeof payload.payeeUserId === 'string' ? payload.payeeUserId : null;
  const paidByUserId = typeof payload.paidByUserId === 'string' ? payload.paidByUserId : null;

  if (eventName === 'SettlementRecorded') {
    if (recipientUserId === payeeUserId) return 'payee';
    if (recipientUserId === payerUserId) return 'payer';
    return 'uninvolved';
  }

  if (
    eventName === 'GroupExpenseRecorded' ||
    eventName === 'GroupExpenseUpdated' ||
    eventName === 'ExpenseRecorded'
  ) {
    if (recipientUserId === paidByUserId) return 'payer';
    const shares = payload.sharesByUserId;
    if (shares && typeof shares === 'object' && !Array.isArray(shares)) {
      if (recipientUserId in (shares as Record<string, unknown>)) return 'splittee';
    }
    return 'peer';
  }

  if (eventName === 'TaskCreated' || eventName === 'TaskDueReminder') {
    const assignees: string[] = [];
    if (Array.isArray(payload.assigneeUserIds)) {
      for (const id of payload.assigneeUserIds) {
        if (typeof id === 'string') assignees.push(id);
      }
    }
    if (typeof payload.assigneeUserId === 'string') assignees.push(payload.assigneeUserId);
    if (assignees.includes(recipientUserId)) return 'assignee';
  }

  if (eventName === 'ApprovalRequested') {
    const approvers: string[] = [];
    if (Array.isArray(payload.approverUserIds)) {
      for (const id of payload.approverUserIds) {
        if (typeof id === 'string') approvers.push(id);
      }
    }
    if (approvers.includes(recipientUserId)) return 'approver';
  }

  if (eventName === 'GroupInviteRedeemed') return 'organizer';
  if (eventName === 'PollCreated') return 'voter';

  return 'peer';
}

export function buildRecipientFinanceCtx(
  recipientUserId: string,
  relationship: RecipientRelationship,
  payload: Record<string, unknown>
): Pick<RecipientContext, 'recipientShare' | 'owedToRecipient' | 'currencyCode' | 'momentTitle'> {
  const currencyCode = typeof payload.currencyCode === 'string' ? payload.currencyCode : null;
  const momentTitle = typeof payload.momentTitle === 'string' ? payload.momentTitle : null;
  const shares = payload.sharesByUserId;
  let recipientShare: string | null = null;
  if (shares && typeof shares === 'object' && !Array.isArray(shares)) {
    const v = (shares as Record<string, unknown>)[recipientUserId];
    if (typeof v === 'string' || typeof v === 'number') recipientShare = String(v);
  }
  let owedToRecipient: string | null = null;
  if (relationship === 'payer' && typeof payload.owedToPayer === 'string') {
    owedToRecipient = payload.owedToPayer;
  }
  const owedMap = payload.owedToUserId;
  if (owedMap && typeof owedMap === 'object' && !Array.isArray(owedMap)) {
    const v = (owedMap as Record<string, unknown>)[recipientUserId];
    if (typeof v === 'string' || typeof v === 'number') owedToRecipient = String(v);
  }
  return { recipientShare, owedToRecipient, currencyCode, momentTitle };
}

export type { DeliveryRoute };
