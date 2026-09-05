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
  'BillReminder',
  'ChoreReminder',
  'ExpenseReminder',
  'PhotoReminder',
  'DigestReady',
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
  DigestReady: 'reminders',
  PurchaseItemAdded: 'social',
  ResidentAdded: 'social',
  MaintenanceRecordCreated: 'tasks',
  LivingRuleCreated: 'social',
  SharedAssetCreated: 'social',
  OwnershipRecordCreated: 'finance',
  DeliveryHandoverPlanned: 'tasks',
};

const PRIORITY_BY_EVENT: Record<string, NotificationPriority> = {
  ApprovalRequested: 'HIGH',
  SettlementRecorded: 'HIGH',
  GroupInviteMinted: 'HIGH',
  GroupInviteRedeemed: 'HIGH',
  TaskDueReminder: 'HIGH',
  BillReminder: 'HIGH',
  BusinessIssueCreated: 'HIGH',
  PollVoted: 'LOW',
  GroupParticipantRoleUpdated: 'LOW',
  DigestReady: 'NORMAL',
};

export function isPeerPushEvent(eventName: string): boolean {
  return PEER_PUSH_EVENT_NAMES.has(eventName);
}

export function notificationCategory(eventName: string): NotificationCategory {
  return CATEGORY_BY_EVENT[eventName] ?? 'system';
}

export function notificationPriority(eventName: string): NotificationPriority {
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

/** Skip push when GroupUpdatePosted was posted with notifyMembers=false. */
export function shouldSkipPushForPayload(
  eventName: string,
  payload?: Record<string, unknown> | null
): boolean {
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

function actorLabel(payload?: Record<string, unknown> | null): string {
  const name =
    (typeof payload?.actorDisplayName === 'string' && payload.actorDisplayName.trim()) ||
    (typeof payload?.actorName === 'string' && payload.actorName.trim()) ||
    null;
  return name || 'Someone';
}

function titleFromPayload(payload?: Record<string, unknown> | null): string | null {
  if (typeof payload?.title === 'string' && payload.title.trim()) return payload.title.trim();
  return null;
}

export function notificationCopy(
  eventName: string,
  payload?: Record<string, unknown> | null
): { title: string; body: string } {
  const actor = actorLabel(payload);
  const itemTitle = titleFromPayload(payload);

  switch (eventName) {
    case 'ExpenseCreated':
    case 'ExpenseRecorded':
    case 'GroupExpenseRecorded':
      return {
        title: 'New expense',
        body: itemTitle ? `${actor} recorded “${itemTitle}”.` : `${actor} recorded an expense.`,
      };
    case 'GroupExpenseUpdated':
      return { title: 'Expense updated', body: `${actor} updated an expense.` };
    case 'GroupExpenseVoided':
      return { title: 'Expense voided', body: `${actor} voided an expense.` };
    case 'SettlementRecorded':
      return { title: 'Settlement recorded', body: `${actor} recorded a settlement.` };
    case 'PollCreated':
      return {
        title: 'New poll',
        body: itemTitle ? `${actor} opened “${itemTitle}”.` : `${actor} opened a poll.`,
      };
    case 'PollVoted':
      return { title: 'Poll update', body: `${actor} voted on a poll.` };
    case 'PollClosed':
      return { title: 'Poll closed', body: `${actor} closed a poll.` };
    case 'TaskCreated':
      return {
        title: 'New task',
        body: itemTitle ? `${actor} added “${itemTitle}”.` : `${actor} added a task.`,
      };
    case 'TaskDueReminder':
      return {
        title: 'Task due',
        body: itemTitle ? `“${itemTitle}” is due soon.` : 'A task is due soon.',
      };
    case 'PlanningItemCreated':
      return { title: 'New plan item', body: `${actor} added a planning item.` };
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
        title: 'Approval needed',
        body: itemTitle ? `${actor} requested approval for “${itemTitle}”.` : `${actor} requested an approval.`,
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
    default:
      return { title: 'Momentra update', body: `Activity: ${eventName}` };
  }
}
