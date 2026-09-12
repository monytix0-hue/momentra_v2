/** Domain events that warrant peer FCM push (exclude actor unless targeted). */

export type NotificationCategory =
  | 'finance'
  | 'tasks'
  | 'social'
  | 'invites'
  | 'approvals'
  | 'reminders'
  | 'system';

export type NotificationPriority = 'HIGH' | 'NORMAL' | 'LOW';

export const PEER_PUSH_EVENT_NAMES = new Set<string>([
  'ExpenseCreated',
  'ExpenseRecorded',
  'GroupExpenseRecorded',
  'GroupExpenseUpdated',
  'GroupExpenseVoided',
  'SettlementRecorded',
  'PollCreated',
  'PollVoted',
  'PollClosed',
  'TaskCreated',
  'TaskDueReminder',
  'PlanningItemCreated',
  'PlanningItemUpdated',
  'BookingCreated',
  'MilestoneCreated',
  'GoalCreated',
  'GroupUpdatePosted',
  'MemoryCreated',
  'BusinessMemoryCreated',
  'GroupInviteMinted',
  'GroupInviteRedeemed',
  'GroupParticipantLeft',
  'GroupParticipantRemoved',
  'GroupParticipantRoleUpdated',
  'PurchaseItemAdded',
  'ResidentAdded',
  'MaintenanceRecordCreated',
  'LivingRuleCreated',
  'SharedAssetCreated',
  'OwnershipRecordCreated',
  'DeliveryHandoverPlanned',
  'BusinessUpdatePublished',
  'ApprovalRequested',
  'BusinessIssueCreated',
  'CompanyMemberAdded',
  'InvestorUpdateCreated',
  'MeetingRecordCreated',
  'WeeklyReminder',
  'DailyPersonalReminder',
  'BillReminder',
  'ChoreReminder',
  'ExpenseReminder',
  'PhotoReminder',
  'DigestReady',
  // Wave 2 derived signals
  'TripBudgetThresholdReached',
  'UserBalanceChangedMeaningfully',
  'SettlementSuggested',
  'GroupNearlySettled',
  'PollNeedsYourVote',
  'AssignedTaskDueSoon',
  'ApprovalsAccumulating',
  'ApprovalAging',
  'InvoiceDueSoon',
  'InvoiceOverdue',
  'ExpenseThresholdExceeded',
  'RunwayChangedMeaningfully',
  'BudgetThresholdReached',
  'BillDueSoon',
  'GoalMilestoneReached',
  'GoalAtRisk',
  'RecurringExpenseExpected',
  'MomentDigestReady',
]);

const CATEGORY_BY_EVENT: Record<string, NotificationCategory> = {
  ExpenseCreated: 'finance',
  ExpenseRecorded: 'finance',
  GroupExpenseRecorded: 'finance',
  GroupExpenseUpdated: 'finance',
  GroupExpenseVoided: 'finance',
  SettlementRecorded: 'finance',
  ExpenseReminder: 'finance',
  BillReminder: 'finance',
  PollCreated: 'social',
  PollVoted: 'social',
  PollClosed: 'social',
  GroupUpdatePosted: 'social',
  MemoryCreated: 'social',
  BusinessMemoryCreated: 'social',
  PhotoReminder: 'social',
  TaskCreated: 'tasks',
  TaskDueReminder: 'tasks',
  PlanningItemCreated: 'tasks',
  PlanningItemUpdated: 'tasks',
  BookingCreated: 'tasks',
  MilestoneCreated: 'tasks',
  GoalCreated: 'tasks',
  ChoreReminder: 'tasks',
  GroupInviteMinted: 'invites',
  GroupInviteRedeemed: 'invites',
  GroupParticipantLeft: 'invites',
  GroupParticipantRemoved: 'invites',
  GroupParticipantRoleUpdated: 'invites',
  CompanyMemberAdded: 'invites',
  ApprovalRequested: 'approvals',
  BusinessIssueCreated: 'approvals',
  BusinessUpdatePublished: 'approvals',
  InvestorUpdateCreated: 'approvals',
  MeetingRecordCreated: 'approvals',
  WeeklyReminder: 'reminders',
  DailyPersonalReminder: 'reminders',
  DigestReady: 'reminders',
  PurchaseItemAdded: 'social',
  ResidentAdded: 'social',
  MaintenanceRecordCreated: 'tasks',
  LivingRuleCreated: 'social',
  SharedAssetCreated: 'social',
  OwnershipRecordCreated: 'finance',
  DeliveryHandoverPlanned: 'tasks',
  TripBudgetThresholdReached: 'finance',
  UserBalanceChangedMeaningfully: 'finance',
  SettlementSuggested: 'finance',
  GroupNearlySettled: 'finance',
  PollNeedsYourVote: 'tasks',
  AssignedTaskDueSoon: 'tasks',
  ApprovalsAccumulating: 'approvals',
  ApprovalAging: 'approvals',
  InvoiceDueSoon: 'finance',
  InvoiceOverdue: 'finance',
  ExpenseThresholdExceeded: 'finance',
  RunwayChangedMeaningfully: 'finance',
  BudgetThresholdReached: 'finance',
  BillDueSoon: 'reminders',
  GoalMilestoneReached: 'tasks',
  GoalAtRisk: 'tasks',
  RecurringExpenseExpected: 'finance',
  MomentDigestReady: 'system',
};

const PRIORITY_BY_EVENT: Record<string, NotificationPriority> = {
  ApprovalRequested: 'HIGH',
  SettlementRecorded: 'HIGH',
  GroupInviteMinted: 'HIGH',
  GroupInviteRedeemed: 'HIGH',
  TaskDueReminder: 'HIGH',
  BillReminder: 'HIGH',
  BusinessIssueCreated: 'HIGH',
  // HIGH so the daily nudge is never swallowed by digest batching or quiet hours.
  DailyPersonalReminder: 'HIGH',
  PollVoted: 'LOW',
  GroupParticipantRoleUpdated: 'LOW',
  DigestReady: 'NORMAL',
  TripBudgetThresholdReached: 'HIGH',
  SettlementSuggested: 'HIGH',
  AssignedTaskDueSoon: 'HIGH',
  ApprovalsAccumulating: 'HIGH',
  ApprovalAging: 'HIGH',
  InvoiceDueSoon: 'HIGH',
  InvoiceOverdue: 'HIGH',
  GoalAtRisk: 'HIGH',
  GroupNearlySettled: 'LOW',
  RecurringExpenseExpected: 'LOW',
  MomentDigestReady: 'NORMAL',
};

export function isPeerPushEvent(eventName: string): boolean {
  return PEER_PUSH_EVENT_NAMES.has(eventName);
}

export function notificationCategory(eventName: string): NotificationCategory {
  return CATEGORY_BY_EVENT[eventName] ?? 'system';
}

export function notificationPriority(
  eventName: string,
  payload?: Record<string, unknown> | null
): NotificationPriority {
  const fromPayload = payload?.importance;
  if (fromPayload === 'HIGH' || fromPayload === 'NORMAL' || fromPayload === 'LOW') {
    return fromPayload;
  }
  return PRIORITY_BY_EVENT[eventName] ?? 'NORMAL';
}

/** BullMQ priority: lower number = higher priority. */
export function bullmqPriority(priority: NotificationPriority): number {
  switch (priority) {
    case 'HIGH':
      return 1;
    case 'LOW':
      return 10;
    default:
      return 5;
  }
}

/** Skip push for drafts, and for GroupUpdatePosted posted with notifyMembers=false. */
export function shouldSkipPushForPayload(
  eventName: string,
  payload?: Record<string, unknown> | null
): boolean {
  if (payload && payload.asDraft === true) return true;
  if (eventName !== 'GroupUpdatePosted') return false;
  if (payload && payload.notifyMembers === false) return true;
  return false;
}

export function deepLinkForEvent(
  eventName: string,
  payload?: Record<string, unknown> | null
): string | null {
  const momentId =
    (typeof payload?.momentId === 'string' ? payload.momentId : null) ??
    (typeof payload?.scopeId === 'string' ? payload.scopeId : null);
  if (!momentId) {
    if (eventName === 'DigestReady') return 'momentra://inbox';
    return null;
  }
  const category = notificationCategory(eventName);
  return `momentra://moment/${momentId}?category=${category}&event=${encodeURIComponent(eventName)}`;
}

import type { RecipientContext } from './decision-types';
import { moneyLabel } from './decision-types';

function actorLabel(payload?: Record<string, unknown> | null): string {
  const name =
    (typeof payload?.actorDisplayName === 'string' && payload.actorDisplayName.trim()) ||
    (typeof payload?.actorName === 'string' && payload.actorName.trim()) ||
    null;
  return name || 'Someone';
}

function titleFromPayload(payload?: Record<string, unknown> | null): string | null {
  if (typeof payload?.title === 'string' && payload.title.trim()) return payload.title.trim();
  if (typeof payload?.description === 'string' && payload.description.trim()) {
    return payload.description.trim();
  }
  return null;
}

function momentPrefix(payload?: Record<string, unknown> | null, ctx?: RecipientContext | null): string | null {
  const t =
    ctx?.momentTitle?.trim() ||
    (typeof payload?.momentTitle === 'string' ? payload.momentTitle.trim() : '') ||
    null;
  return t || null;
}

function withMomentTitle(base: string, payload?: Record<string, unknown> | null, ctx?: RecipientContext | null): string {
  const m = momentPrefix(payload, ctx);
  return m ? `${m} · ${base}` : base;
}

function expenseConsequenceBody(
  actor: string,
  itemTitle: string | null,
  payload: Record<string, unknown> | null | undefined,
  ctx?: RecipientContext | null
): string {
  const amount = typeof payload?.amount === 'string' ? payload.amount : null;
  const currency = ctx?.currencyCode ?? (typeof payload?.currencyCode === 'string' ? payload.currencyCode : null);
  const paid = moneyLabel(amount, currency);
  const head = paid ? `${actor} paid ${paid}` : itemTitle ? `${actor} recorded “${itemTitle}”.` : `${actor} recorded an expense.`;
  if (ctx?.relationship === 'payer' && ctx.owedToRecipient) {
    return `${head} · You're owed ${moneyLabel(ctx.owedToRecipient, currency)}`;
  }
  if (ctx?.recipientShare) {
    return `${head} · Your share ${moneyLabel(ctx.recipientShare, currency)}`;
  }
  if (paid && itemTitle) return `${actor} recorded “${itemTitle}” · ${paid}`;
  if (paid) return head;
  return itemTitle ? `${actor} recorded “${itemTitle}”.` : `${actor} recorded an expense.`;
}

/**
 * Recipient-aware copy. Pass recipientCtx from the decision layer for personalized wording.
 * Legacy callers may omit ctx (global fallback copy).
 */
export function notificationCopy(
  eventName: string,
  payload?: Record<string, unknown> | null,
  recipientCtx?: RecipientContext | null
): { title: string; body: string } {
  const actor = actorLabel(payload);
  const itemTitle = titleFromPayload(payload);

  switch (eventName) {
    case 'ExpenseCreated':
    case 'ExpenseRecorded':
    case 'GroupExpenseRecorded':
      return {
        title: withMomentTitle(itemTitle ?? 'New expense', payload, recipientCtx),
        body: expenseConsequenceBody(actor, itemTitle, payload, recipientCtx),
      };
    case 'GroupExpenseUpdated':
      return {
        title: withMomentTitle('Expense updated', payload, recipientCtx),
        body: `${actor} updated an expense.`,
      };
    case 'GroupExpenseVoided':
      return {
        title: withMomentTitle('Expense voided', payload, recipientCtx),
        body: `${actor} voided an expense.`,
      };
    case 'SettlementRecorded': {
      const amount = typeof payload?.amount === 'string' ? payload.amount : null;
      const currency =
        recipientCtx?.currencyCode ??
        (typeof payload?.currencyCode === 'string' ? payload.currencyCode : null);
      const money = moneyLabel(amount, currency);
      const payerName =
        (typeof payload?.payerDisplayName === 'string' && payload.payerDisplayName.trim()) || actor;
      const payeeName =
        (typeof payload?.payeeDisplayName === 'string' && payload.payeeDisplayName.trim()) || 'them';
      let body = `${actor} recorded a settlement.`;
      if (recipientCtx?.relationship === 'payee' && money) {
        body = `${payerName} paid you ${money}`;
      } else if (recipientCtx?.relationship === 'payer' && money) {
        body = `You paid ${payeeName} ${money}`;
      } else if (money) {
        body = `${payerName} paid ${payeeName} ${money}`;
      }
      return { title: withMomentTitle('Settlement', payload, recipientCtx), body };
    }
    case 'PollCreated':
      return {
        title: withMomentTitle('New poll', payload, recipientCtx),
        body: itemTitle ? `${actor} opened “${itemTitle}”.` : `${actor} opened a poll.`,
      };
    case 'PollVoted':
      return { title: withMomentTitle('Poll update', payload, recipientCtx), body: `${actor} voted on a poll.` };
    case 'PollClosed':
      return { title: withMomentTitle('Poll closed', payload, recipientCtx), body: `${actor} closed a poll.` };
    case 'TaskCreated':
      return {
        title: withMomentTitle('New task', payload, recipientCtx),
        body:
          recipientCtx?.relationship === 'assignee'
            ? itemTitle
              ? `You’re assigned: ${itemTitle}`
              : `You’re assigned a task.`
            : itemTitle
              ? `${actor} added “${itemTitle}”.`
              : `${actor} added a task.`,
      };
    case 'TaskDueReminder':
      return {
        title: 'Task due',
        body: itemTitle ? `“${itemTitle}” is due soon.` : 'A task is due soon.',
      };
    case 'PlanningItemCreated':
      return { title: 'New plan item', body: `${actor} added a planning item.` };
    case 'PlanningItemUpdated':
      return {
        title: 'Plan updated',
        body: itemTitle ? `${actor} updated “${itemTitle}”.` : `${actor} updated a planning item.`,
      };
    case 'BookingCreated':
      return { title: 'New booking', body: `${actor} added a booking.` };
    case 'MilestoneCreated':
      return {
        title: 'New milestone',
        body: itemTitle ? `${actor} added “${itemTitle}”.` : `${actor} added a milestone.`,
      };
    case 'GoalCreated':
      return {
        title: 'New goal',
        body: itemTitle ? `${actor} added “${itemTitle}”.` : `${actor} added a goal.`,
      };
    case 'GroupUpdatePosted':
      return { title: 'New update', body: `${actor} posted an update.` };
    case 'MemoryCreated':
    case 'BusinessMemoryCreated':
      return { title: 'New memory', body: `${actor} added a memory.` };
    case 'GroupInviteMinted':
      return { title: 'Invite ready', body: `${actor} created a group invite.` };
    case 'GroupInviteRedeemed':
      return { title: 'Someone joined', body: `${actor} joined your group.` };
    case 'GroupParticipantLeft':
      return { title: 'Member left', body: `${actor} left your group.` };
    case 'GroupParticipantRemoved':
      return { title: 'Member removed', body: `A member was removed from your group.` };
    case 'GroupParticipantRoleUpdated':
      return { title: 'Role updated', body: `A member role was updated.` };
    case 'PurchaseItemAdded':
      return { title: 'Purchase item', body: `${actor} added a purchase item.` };
    case 'ResidentAdded':
      return { title: 'Resident added', body: `${actor} added a resident.` };
    case 'MaintenanceRecordCreated':
      return { title: 'Maintenance', body: `${actor} added a maintenance record.` };
    case 'LivingRuleCreated':
      return { title: 'House rule', body: `${actor} added a living rule.` };
    case 'SharedAssetCreated':
      return { title: 'Shared asset', body: `${actor} added a shared asset.` };
    case 'OwnershipRecordCreated':
      return { title: 'Ownership', body: `${actor} recorded ownership.` };
    case 'DeliveryHandoverPlanned':
      return { title: 'Delivery', body: `${actor} planned a delivery handover.` };
    case 'BusinessUpdatePublished':
      return { title: 'Business update', body: `${actor} published a business update.` };
    case 'ApprovalRequested':
      return {
        title: withMomentTitle('Approval needed', payload, recipientCtx),
        body:
          recipientCtx?.relationship === 'approver'
            ? itemTitle
              ? `Approval needed from you for “${itemTitle}”.`
              : `Approval needed from you.`
            : itemTitle
              ? `${actor} requested approval for “${itemTitle}”.`
              : `${actor} requested an approval.`,
      };
    case 'BusinessIssueCreated':
      return {
        title: 'New issue',
        body: itemTitle ? `${actor} opened “${itemTitle}”.` : `${actor} opened an issue.`,
      };
    case 'CompanyMemberAdded':
      return { title: 'New member', body: `${actor} added a company member.` };
    case 'InvestorUpdateCreated':
      return { title: 'Investor update', body: `${actor} posted an investor update.` };
    case 'MeetingRecordCreated':
      return { title: 'Meeting recorded', body: `${actor} added a meeting record.` };
    case 'WeeklyReminder':
      return {
        title: 'Weekly check-in',
        body: typeof payload?.body === 'string' ? payload.body : 'Time for your weekly Momentra check-in.',
      };
    case 'DailyPersonalReminder':
      return {
        title: 'Your daily Personal check-in',
        body:
          typeof payload?.body === 'string'
            ? payload.body
            : 'Open Personal and log one thing from today.',
      };
    case 'BillReminder':
      return { title: 'Bill reminder', body: 'A shared bill may need attention.' };
    case 'ChoreReminder':
      return { title: 'Chore reminder', body: 'A shared chore may need attention.' };
    case 'ExpenseReminder':
      return { title: 'Expense reminder', body: 'Log recent group expenses while they’re fresh.' };
    case 'PhotoReminder':
      return { title: 'Photo reminder', body: 'Add photos from your shared experience.' };
    case 'DigestReady': {
      const n = typeof payload?.count === 'number' ? payload.count : 0;
      return {
        title: 'Momentra digest',
        body: n > 0 ? `You have ${n} updates waiting.` : 'You have updates waiting.',
      };
    }
    // Wave 2 derived signals — narrate verified facts only
    case 'TripBudgetThresholdReached':
    case 'UserBalanceChangedMeaningfully':
    case 'SettlementSuggested':
    case 'GroupNearlySettled':
    case 'PollNeedsYourVote':
    case 'AssignedTaskDueSoon':
    case 'ApprovalsAccumulating':
    case 'ApprovalAging':
    case 'InvoiceDueSoon':
    case 'InvoiceOverdue':
    case 'ExpenseThresholdExceeded':
    case 'RunwayChangedMeaningfully':
    case 'BudgetThresholdReached':
    case 'BillDueSoon':
    case 'GoalMilestoneReached':
    case 'GoalAtRisk':
    case 'RecurringExpenseExpected':
    case 'MomentDigestReady': {
      const momentTitle =
        typeof payload?.momentTitle === 'string' ? payload.momentTitle : null;
      const companyName =
        typeof payload?.companyName === 'string' ? payload.companyName : null;
      const goalTitle = typeof payload?.goalTitle === 'string' ? payload.goalTitle : null;
      const body =
        typeof payload?.body === 'string' && payload.body.trim()
          ? payload.body
          : 'A meaningful update needs your attention.';
      const title =
        momentTitle ??
        companyName ??
        goalTitle ??
        (eventName === 'MomentDigestReady' ? 'Moment digest' : 'Momentra insight');
      return { title, body };
    }
    default:
      return { title: 'Momentra update', body: `Activity: ${eventName}` };
  }
}
