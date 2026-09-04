/** Domain events that warrant peer FCM push (exclude actor). */
export const PEER_PUSH_EVENT_NAMES = new Set<string>([
  'ExpenseCreated',
  'GroupExpenseRecorded',
  'GroupExpenseUpdated',
  'GroupExpenseVoided',
  'SettlementRecorded',
  'PollCreated',
  'PollVoted',
  'PollClosed',
  'TaskCreated',
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
]);

export function isPeerPushEvent(eventName: string): boolean {
  return PEER_PUSH_EVENT_NAMES.has(eventName);
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

export function notificationCopy(
  eventName: string,
  _payload?: Record<string, unknown> | null
): { title: string; body: string } {
  switch (eventName) {
    case 'ExpenseCreated':
    case 'GroupExpenseRecorded':
      return { title: 'New expense', body: 'An expense was recorded in your moment.' };
    case 'GroupExpenseUpdated':
      return { title: 'Expense updated', body: 'An expense was updated in your moment.' };
    case 'GroupExpenseVoided':
      return { title: 'Expense voided', body: 'An expense was voided in your moment.' };
    case 'SettlementRecorded':
      return { title: 'Settlement recorded', body: 'A settlement was recorded in your moment.' };
    case 'PollCreated':
      return { title: 'New poll', body: 'A poll needs your vote.' };
    case 'PollVoted':
      return { title: 'Poll update', body: 'Someone voted on a poll.' };
    case 'PollClosed':
      return { title: 'Poll closed', body: 'A poll was closed.' };
    case 'TaskCreated':
      return { title: 'New task', body: 'A task was added to your moment.' };
    case 'PlanningItemCreated':
      return { title: 'New plan item', body: 'A planning item was added.' };
    case 'BookingCreated':
      return { title: 'New booking', body: 'A booking was added to your moment.' };
    case 'MilestoneCreated':
      return { title: 'New milestone', body: 'A milestone was added.' };
    case 'GoalCreated':
      return { title: 'New goal', body: 'A goal was added to your moment.' };
    case 'GroupUpdatePosted':
      return { title: 'New update', body: 'Someone posted an update in your moment.' };
    case 'MemoryCreated':
    case 'BusinessMemoryCreated':
      return { title: 'New memory', body: 'A memory was added to your moment.' };
    case 'GroupInviteMinted':
      return { title: 'Invite ready', body: 'A group invite link was created.' };
    case 'GroupInviteRedeemed':
      return { title: 'Someone joined', body: 'A member joined your group moment.' };
    case 'GroupParticipantLeft':
      return { title: 'Member left', body: 'A member left your group moment.' };
    case 'GroupParticipantRemoved':
      return { title: 'Member removed', body: 'A member was removed from your group moment.' };
    case 'GroupParticipantRoleUpdated':
      return { title: 'Role updated', body: 'A member role was updated.' };
    case 'PurchaseItemAdded':
      return { title: 'Purchase item', body: 'A purchase item was added.' };
    case 'ResidentAdded':
      return { title: 'Resident added', body: 'A resident was added to your home.' };
    case 'MaintenanceRecordCreated':
      return { title: 'Maintenance', body: 'A maintenance record was added.' };
    case 'LivingRuleCreated':
      return { title: 'House rule', body: 'A living rule was added.' };
    case 'SharedAssetCreated':
      return { title: 'Shared asset', body: 'A shared asset was added.' };
    case 'OwnershipRecordCreated':
      return { title: 'Ownership', body: 'An ownership record was added.' };
    case 'DeliveryHandoverPlanned':
      return { title: 'Delivery', body: 'A delivery handover was planned.' };
    case 'BusinessUpdatePublished':
      return { title: 'Business update', body: 'A business update was published.' };
    case 'ApprovalRequested':
      return { title: 'Approval needed', body: 'An approval was requested.' };
    case 'BusinessIssueCreated':
      return { title: 'New issue', body: 'A business issue was created.' };
    case 'CompanyMemberAdded':
      return { title: 'New member', body: 'A company member was added.' };
    case 'InvestorUpdateCreated':
      return { title: 'Investor update', body: 'An investor update was posted.' };
    case 'MeetingRecordCreated':
      return { title: 'Meeting recorded', body: 'A meeting record was added.' };
    default:
      return { title: 'Momentra update', body: `Activity: ${eventName}` };
  }
}
