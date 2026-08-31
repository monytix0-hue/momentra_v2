export const EVENT_VERSION = 1;

export interface MomentCreatedV1 {
  momentId: string;
  domainCode: 'PERSONAL' | 'GROUP' | 'BUSINESS';
  momentTypeCode: string;
  title: string;
  userId: string;
}

export interface ExpenseRecordedV1 {
  expenseId: string;
  momentId: string;
  domainCode: 'PERSONAL' | 'GROUP' | 'BUSINESS';
  amount: string;
  currencyCode: string;
  userId: string;
}

export interface RoleRevokedV1 {
  userId: string;
  roleCode: string;
  scopeType: string;
  scopeId: string;
}

export const EventNames = {
  MomentCreated: 'MomentCreated',
  ExpenseRecorded: 'ExpenseRecorded',
  RoleRevoked: 'RoleRevoked',
} as const;
