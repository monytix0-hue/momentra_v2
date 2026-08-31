export interface RequestContext {
  firebaseUid: string;
  firebaseProjectId: string;
  userId: string;
  email?: string;
  displayName?: string;
  momentId?: string;
  participantId?: string;
  companyId?: string;
  correlationId: string;
  roles: string[];
  permissions: string[];
}
