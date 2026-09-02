/**
 * Generates authoritative openapi/momentra-v1.yaml (Phase 2 freeze).
 * Run: npm run openapi:build
 */
import { writeFileSync } from 'fs';
import { join } from 'path';

type HttpMethod = 'get' | 'post' | 'patch' | 'delete';
type EndpointKind = 'command' | 'read';

interface EndpointDef {
  method: HttpMethod;
  path: string;
  operationId: string;
  summary: string;
  tags: string[];
  kind: EndpointKind;
  idempotency?: boolean;
  occ?: boolean;
  pagination?: boolean;
  implStatus: 'IMPLEMENTED' | 'CONTRACT_ONLY';
}

const ENDPOINTS: EndpointDef[] = [
  { method: 'get', path: '/me', operationId: 'getMe', summary: 'Bootstrap authenticated identity', tags: ['Auth'], kind: 'read', implStatus: 'IMPLEMENTED' },
  { method: 'post', path: '/me/devices', operationId: 'registerDevice', summary: 'Register push device', tags: ['Devices'], kind: 'command', idempotency: true, implStatus: 'IMPLEMENTED' },
  { method: 'delete', path: '/me/devices/{deviceId}', operationId: 'revokeDevice', summary: 'Revoke push device', tags: ['Devices'], kind: 'command', implStatus: 'IMPLEMENTED' },
  { method: 'post', path: '/moments', operationId: 'createMoment', summary: 'Create moment', tags: ['Moments'], kind: 'command', idempotency: true, implStatus: 'IMPLEMENTED' },
  { method: 'get', path: '/moments/{momentId}', operationId: 'getMoment', summary: 'Get moment', tags: ['Moments'], kind: 'read', implStatus: 'IMPLEMENTED' },
  { method: 'patch', path: '/moments/{momentId}', operationId: 'updateMoment', summary: 'Update moment', tags: ['Moments'], kind: 'command', idempotency: true, occ: true, implStatus: 'IMPLEMENTED' },
  { method: 'post', path: '/moments/{momentId}/archive', operationId: 'archiveMoment', summary: 'Archive moment', tags: ['Moments'], kind: 'command', idempotency: true, occ: true, implStatus: 'IMPLEMENTED' },
  { method: 'post', path: '/moments/{momentId}/cancel', operationId: 'cancelMoment', summary: 'Cancel moment', tags: ['Moments'], kind: 'command', idempotency: true, occ: true, implStatus: 'IMPLEMENTED' },
  { method: 'delete', path: '/moments/{momentId}', operationId: 'deleteMoment', summary: 'Hard-delete moment (status=DELETED)', tags: ['Moments'], kind: 'command', idempotency: true, occ: true, implStatus: 'IMPLEMENTED' },
  { method: 'get', path: '/moments/{momentId}/activity', operationId: 'getMomentActivity', summary: 'Moment activity timeline', tags: ['Activity'], kind: 'read', pagination: true, implStatus: 'IMPLEMENTED' },
  { method: 'post', path: '/moments/{momentId}/goals', operationId: 'createGoal', summary: 'Create goal in moment', tags: ['Work'], kind: 'command', idempotency: true, implStatus: 'IMPLEMENTED' },
  { method: 'post', path: '/moments/{momentId}/milestones', operationId: 'createMilestone', summary: 'Create milestone in moment', tags: ['Work'], kind: 'command', idempotency: true, implStatus: 'IMPLEMENTED' },
  { method: 'post', path: '/moments/{momentId}/tasks', operationId: 'createTask', summary: 'Create task in moment', tags: ['Work'], kind: 'command', idempotency: true, implStatus: 'IMPLEMENTED' },
  { method: 'post', path: '/moments/{momentId}/expenses', operationId: 'createExpense', summary: 'Record expense in moment', tags: ['Finance'], kind: 'command', idempotency: true, implStatus: 'IMPLEMENTED' },
  { method: 'post', path: '/moments/{momentId}/movements', operationId: 'createMovement', summary: 'Record movement in moment', tags: ['Finance'], kind: 'command', idempotency: true, implStatus: 'IMPLEMENTED' },
  { method: 'post', path: '/moments/{momentId}/contributions', operationId: 'recordContribution', summary: 'Record contribution', tags: ['Finance'], kind: 'command', idempotency: true, implStatus: 'IMPLEMENTED' },
  { method: 'post', path: '/moments/{momentId}/polls', operationId: 'createPoll', summary: 'Create poll in moment', tags: ['Poll'], kind: 'command', idempotency: true, implStatus: 'IMPLEMENTED' },
  { method: 'get', path: '/polls/{pollId}', operationId: 'getPoll', summary: 'Get poll', tags: ['Poll'], kind: 'read', implStatus: 'IMPLEMENTED' },
  { method: 'post', path: '/polls/{pollId}/votes', operationId: 'votePoll', summary: 'Cast poll vote', tags: ['Poll'], kind: 'command', idempotency: true, implStatus: 'IMPLEMENTED' },
  { method: 'post', path: '/polls/{pollId}/close', operationId: 'closePoll', summary: 'Close poll', tags: ['Poll'], kind: 'command', idempotency: true, occ: true, implStatus: 'IMPLEMENTED' },
  { method: 'post', path: '/moments/{momentId}/planning-items', operationId: 'createPlanningItem', summary: 'Create planning item', tags: ['Collaboration'], kind: 'command', idempotency: true, implStatus: 'IMPLEMENTED' },
  { method: 'post', path: '/moments/{momentId}/bookings', operationId: 'createBooking', summary: 'Create booking', tags: ['Collaboration'], kind: 'command', idempotency: true, implStatus: 'IMPLEMENTED' },
  { method: 'post', path: '/moments/{momentId}/updates', operationId: 'postUpdate', summary: 'Post group update', tags: ['Collaboration'], kind: 'command', idempotency: true, implStatus: 'IMPLEMENTED' },
  { method: 'post', path: '/moments/{momentId}/participants', operationId: 'addParticipant', summary: 'Add participant', tags: ['Collaboration'], kind: 'command', idempotency: true, implStatus: 'IMPLEMENTED' },
  { method: 'post', path: '/moments/{momentId}/purchase-items', operationId: 'addPurchaseItem', summary: 'Add purchase item', tags: ['Collaboration'], kind: 'command', idempotency: true, implStatus: 'IMPLEMENTED' },
  { method: 'get', path: '/group/moments/{momentId}/delivery-handovers', operationId: 'listDeliveryHandovers', summary: 'List delivery handovers', tags: ['Collaboration'], kind: 'read', implStatus: 'IMPLEMENTED' },
  { method: 'post', path: '/moments/{momentId}/delivery-handovers', operationId: 'createDeliveryHandover', summary: 'Plan delivery handover', tags: ['Collaboration'], kind: 'command', idempotency: true, implStatus: 'IMPLEMENTED' },
  { method: 'get', path: '/group/moments/{momentId}/ownership-records', operationId: 'listOwnershipRecords', summary: 'List ownership records', tags: ['Collaboration'], kind: 'read', implStatus: 'IMPLEMENTED' },
  { method: 'post', path: '/moments/{momentId}/ownership-records', operationId: 'createOwnershipRecord', summary: 'Record ownership transfer', tags: ['Collaboration'], kind: 'command', idempotency: true, implStatus: 'IMPLEMENTED' },
  { method: 'post', path: '/moments/{momentId}/residents', operationId: 'addResident', summary: 'Add resident', tags: ['Collaboration'], kind: 'command', idempotency: true, implStatus: 'IMPLEMENTED' },
  { method: 'post', path: '/moments/{momentId}/memories', operationId: 'createMemory', summary: 'Create memory', tags: ['Collaboration'], kind: 'command', idempotency: true, implStatus: 'IMPLEMENTED' },
  { method: 'post', path: '/moments/{momentId}/observations', operationId: 'recordObservation', summary: 'Record observation', tags: ['Personal'], kind: 'command', idempotency: true, implStatus: 'IMPLEMENTED' },
  { method: 'post', path: '/moments/{momentId}/future-items', operationId: 'createFutureItem', summary: 'Create future item', tags: ['Personal'], kind: 'command', idempotency: true, implStatus: 'IMPLEMENTED' },
  { method: 'post', path: '/moments/{momentId}/lifestyle-activities', operationId: 'createLifestyleActivity', summary: 'Create lifestyle activity', tags: ['Personal'], kind: 'command', idempotency: true, implStatus: 'IMPLEMENTED' },
  { method: 'patch', path: '/moments/{momentId}/lifestyle-activities/{activityId}', operationId: 'updateLifestyleActivity', summary: 'Update lifestyle activity', tags: ['Personal'], kind: 'command', implStatus: 'IMPLEMENTED' },
  { method: 'delete', path: '/moments/{momentId}/lifestyle-activities/{activityId}', operationId: 'voidLifestyleActivity', summary: 'Void lifestyle activity', tags: ['Personal'], kind: 'command', implStatus: 'IMPLEMENTED' },
  { method: 'post', path: '/moments/{momentId}/relationship-activities', operationId: 'recordRelationshipActivity', summary: 'Record relationship activity', tags: ['Personal'], kind: 'command', idempotency: true, implStatus: 'IMPLEMENTED' },
  { method: 'get', path: '/personal/pulse', operationId: 'getPersonalPulse', summary: 'Personal pulse projection', tags: ['Personal'], kind: 'read', implStatus: 'IMPLEMENTED' },
  { method: 'get', path: '/personal/moments', operationId: 'listPersonalMoments', summary: 'Personal moments list', tags: ['Personal'], kind: 'read', pagination: true, implStatus: 'IMPLEMENTED' },
  { method: 'get', path: '/personal/life', operationId: 'getPersonalLife', summary: 'Personal life projection', tags: ['Personal'], kind: 'read', implStatus: 'IMPLEMENTED' },
  { method: 'get', path: '/personal/memory', operationId: 'getPersonalMemory', summary: 'Personal memory projection', tags: ['Personal'], kind: 'read', implStatus: 'IMPLEMENTED' },
  { method: 'get', path: '/personal/attention', operationId: 'getPersonalAttention', summary: 'Personal attention projection', tags: ['Personal'], kind: 'read', implStatus: 'IMPLEMENTED' },
  { method: 'get', path: '/personal/activity', operationId: 'getPersonalActivity', summary: 'Personal activity timeline', tags: ['Personal', 'Activity'], kind: 'read', pagination: true, implStatus: 'IMPLEMENTED' },
  { method: 'get', path: '/group/moments', operationId: 'listGroupMoments', summary: 'Group moments list', tags: ['Group'], kind: 'read', pagination: true, implStatus: 'IMPLEMENTED' },
  ...(['pulse', 'life', 'memory', 'finance', 'actions'] as const).map((facet) => ({
    method: 'get' as const,
    path: `/group/moments/{momentId}/${facet}`,
    operationId: `getGroupMoment${facet.charAt(0).toUpperCase()}${facet.slice(1)}`,
    summary: `Group moment ${facet} projection`,
    tags: ['Group'],
    kind: 'read' as const,
    implStatus: 'IMPLEMENTED' as const,
  })),
  { method: 'patch', path: '/group/moments/{momentId}/budget', operationId: 'patchGroupMomentBudget', summary: 'Update group moment budget', tags: ['Group', 'Finance'], kind: 'command', idempotency: true, implStatus: 'IMPLEMENTED' },
  { method: 'get', path: '/business/moments', operationId: 'listBusinessMoments', summary: 'Business moments list', tags: ['Business'], kind: 'read', pagination: true, implStatus: 'IMPLEMENTED' },
  ...(['pulse', 'life', 'memory', 'finance', 'actions'] as const).map((facet) => ({
    method: 'get' as const,
    path: `/business/moments/{momentId}/${facet}`,
    operationId: `getBusinessMoment${facet.charAt(0).toUpperCase()}${facet.slice(1)}`,
    summary: `Business moment ${facet} projection`,
    tags: ['Business'],
    kind: 'read' as const,
    implStatus: 'IMPLEMENTED' as const,
  })),
  { method: 'get', path: '/companies', operationId: 'listCompanies', summary: 'List authorized companies', tags: ['Business'], kind: 'read', implStatus: 'IMPLEMENTED' },
  { method: 'post', path: '/companies', operationId: 'createCompany', summary: 'Create company', tags: ['Business'], kind: 'command', idempotency: true, implStatus: 'IMPLEMENTED' },
  { method: 'get', path: '/companies/{companyId}', operationId: 'getCompany', summary: 'Get company', tags: ['Business'], kind: 'read', implStatus: 'IMPLEMENTED' },
  { method: 'patch', path: '/companies/{companyId}', operationId: 'updateCompany', summary: 'Update company', tags: ['Business'], kind: 'command', idempotency: true, occ: true, implStatus: 'IMPLEMENTED' },
  { method: 'post', path: '/company/invites', operationId: 'mintCompanyInvite', summary: 'Mint company invite code/QR', tags: ['Business'], kind: 'command', idempotency: true, implStatus: 'IMPLEMENTED' },
  { method: 'get', path: '/company/invites/{code}', operationId: 'getCompanyInvite', summary: 'Preview company invite by code', tags: ['Business'], kind: 'read', implStatus: 'IMPLEMENTED' },
  { method: 'post', path: '/company/invites/{code}/redeem', operationId: 'redeemCompanyInvite', summary: 'Redeem company invite and join', tags: ['Business'], kind: 'command', idempotency: true, implStatus: 'IMPLEMENTED' },
  { method: 'get', path: '/companies/{companyId}/locations', operationId: 'listCompanyLocations', summary: 'List company locations', tags: ['Business', 'CompanyLocation'], kind: 'read', implStatus: 'IMPLEMENTED' },
  { method: 'post', path: '/companies/{companyId}/locations', operationId: 'createCompanyLocation', summary: 'Create company location', tags: ['Business', 'CompanyLocation'], kind: 'command', idempotency: true, implStatus: 'IMPLEMENTED' },
  { method: 'patch', path: '/companies/{companyId}/locations/{locationId}', operationId: 'updateCompanyLocation', summary: 'Update company location', tags: ['Business', 'CompanyLocation'], kind: 'command', idempotency: true, occ: true, implStatus: 'IMPLEMENTED' },
  { method: 'get', path: '/companies/{companyId}/teams', operationId: 'listCompanyTeams', summary: 'List company teams', tags: ['Business'], kind: 'read', implStatus: 'IMPLEMENTED' },
  { method: 'post', path: '/companies/{companyId}/teams', operationId: 'createCompanyTeam', summary: 'Create company team', tags: ['Business'], kind: 'command', idempotency: true, implStatus: 'IMPLEMENTED' },
  { method: 'post', path: '/media/uploads', operationId: 'createMediaUpload', summary: 'Create signed upload intent', tags: ['Media'], kind: 'command', idempotency: true, implStatus: 'IMPLEMENTED' },
  { method: 'post', path: '/media/uploads/{uploadId}/complete', operationId: 'completeMediaUpload', summary: 'Complete media upload', tags: ['Media'], kind: 'command', idempotency: true, implStatus: 'IMPLEMENTED' },
  { method: 'get', path: '/life360', operationId: 'getLife360', summary: 'Circle (Life360) projection read', tags: ['Circle'], kind: 'read', implStatus: 'IMPLEMENTED' },
  { method: 'post', path: '/ai/action-proposals/{actionProposalId}/execute', operationId: 'executeActionProposal', summary: 'Execute AI action proposal', tags: ['AI'], kind: 'command', idempotency: true, implStatus: 'IMPLEMENTED' },
  { method: 'get', path: '/business/moments/{momentId}/capacity', operationId: 'getBusinessCapacity', summary: 'Business team capacity heuristic', tags: ['Business'], kind: 'read', implStatus: 'IMPLEMENTED' },
  { method: 'get', path: '/business/moments/{momentId}/workload', operationId: 'getBusinessWorkload', summary: 'Business workload by severity proxy', tags: ['Business'], kind: 'read', implStatus: 'IMPLEMENTED' },
  { method: 'get', path: '/business/moments/{momentId}/mom-deltas', operationId: 'getBusinessMomDeltas', summary: 'Business revenue/expense MoM deltas', tags: ['Business'], kind: 'read', implStatus: 'IMPLEMENTED' },
  { method: 'get', path: '/business/moments/{momentId}/progress-snapshot', operationId: 'getBusinessProgressSnapshot', summary: 'Business forecast/review progress snapshot', tags: ['Business'], kind: 'read', implStatus: 'IMPLEMENTED' },
  { method: 'get', path: '/business/moments/{momentId}/roster', operationId: 'getBusinessRoster', summary: 'Business company roster', tags: ['Business'], kind: 'read', implStatus: 'IMPLEMENTED' },
  { method: 'get', path: '/business/moments/{momentId}/weekly-report', operationId: 'getBusinessWeeklyReport', summary: 'Business weekly activity report', tags: ['Business'], kind: 'read', implStatus: 'IMPLEMENTED' },
  { method: 'post', path: '/business/moments/{momentId}/share-link', operationId: 'createBusinessShareLink', summary: 'Create business dashboard share link', tags: ['Business'], kind: 'command', idempotency: true, implStatus: 'IMPLEMENTED' },
];

function pathParams(path: string): string[] {
  return [...path.matchAll(/\{(\w+)\}/g)].map((m) => m[1]);
}

function buildOperation(ep: EndpointDef): string {
  const lines: string[] = [
    `    ${ep.method}:`,
    `      operationId: ${ep.operationId}`,
    `      summary: ${ep.summary}`,
    `      tags: [${ep.tags.map((t) => `"${t}"`).join(', ')}]`,
    `      x-momentra-kind: ${ep.kind}`,
    `      x-momentra-impl: ${ep.implStatus}`,
  ];
  if (ep.idempotency) lines.push('      x-momentra-idempotency: required');
  if (ep.occ) lines.push('      x-momentra-occ: required');
  if (ep.pagination) lines.push('      x-momentra-pagination: cursor');

  const params = pathParams(ep.path);
  if (params.length || ep.idempotency || ep.pagination) {
    lines.push('      parameters:');
    for (const p of params) {
      lines.push(`        - $ref: '#/components/parameters/${p}Param'`);
    }
    if (ep.pagination) {
      lines.push("        - $ref: '#/components/parameters/cursorQuery'");
      lines.push("        - $ref: '#/components/parameters/limitQuery'");
    }
    if (ep.idempotency) lines.push("        - $ref: '#/components/parameters/idempotencyKeyHeader'");
    lines.push("        - $ref: '#/components/parameters/correlationIdHeader'");
  }

  lines.push('      security:');
  lines.push('        - bearerAuth: []');

  if (ep.method === 'post' || ep.method === 'patch') {
    lines.push('      requestBody:');
    lines.push('        required: true');
    lines.push('        content:');
    lines.push('          application/json:');
    lines.push('            schema:');
    lines.push('              type: object');
  }

  const successCode = ep.method === 'post' && ep.kind === 'command' ? '201' : '200';
  const envelope = ep.kind === 'read' ? 'ProjectionEnvelope' : 'CommandEnvelope';

  lines.push('      responses:');
  lines.push(`        '${successCode}':`);
  lines.push('          description: Success');
  lines.push('          content:');
  lines.push('            application/json:');
  lines.push('              schema:');
  lines.push(`                $ref: './schemas/common.yaml#/${envelope}'`);
  lines.push("        '400':");
  lines.push("          $ref: './schemas/responses.yaml#/Error400'");
  lines.push("        '401':");
  lines.push("          $ref: './schemas/responses.yaml#/Error401'");
  lines.push("        '403':");
  lines.push("          $ref: './schemas/responses.yaml#/Error403'");
  lines.push("        '404':");
  lines.push("          $ref: './schemas/responses.yaml#/Error404'");
  if (ep.occ || ep.idempotency) {
    lines.push("        '409':");
    lines.push("          $ref: './schemas/responses.yaml#/Error409Version'");
  }
  lines.push("        '500':");
  lines.push("          $ref: './schemas/responses.yaml#/Error500'");

  return lines.join('\n');
}

function buildPaths(endpoints: EndpointDef[]): string {
  const byPath = new Map<string, EndpointDef[]>();
  for (const ep of endpoints) {
    if (!byPath.has(ep.path)) byPath.set(ep.path, []);
    byPath.get(ep.path)!.push(ep);
  }
  const lines: string[] = [];
  for (const [path, eps] of [...byPath.entries()].sort((a, b) => a[0].localeCompare(b[0]))) {
    lines.push(`  ${path}:`);
    for (const ep of eps) {
      lines.push(buildOperation(ep));
    }
  }
  return lines.join('\n');
}

const components = `
components:
  securitySchemes:
    bearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT
      description: Firebase ID token (Authorization Bearer)
  parameters:
    correlationIdHeader:
      name: X-Correlation-Id
      in: header
      required: false
      schema:
        type: string
        format: uuid
      description: Optional client correlation ID; echoed in responses.
    idempotencyKeyHeader:
      name: Idempotency-Key
      in: header
      required: true
      schema:
        type: string
        minLength: 8
        maxLength: 128
    cursorQuery:
      name: cursor
      in: query
      schema:
        type: string
    limitQuery:
      name: limit
      in: query
      schema:
        type: integer
        minimum: 1
        maximum: 100
        default: 20
    momentIdParam:
      name: momentId
      in: path
      required: true
      schema:
        type: string
        format: uuid
    deviceIdParam:
      name: deviceId
      in: path
      required: true
      schema:
        type: string
    pollIdParam:
      name: pollId
      in: path
      required: true
      schema:
        type: string
        format: uuid
    companyIdParam:
      name: companyId
      in: path
      required: true
      schema:
        type: string
        format: uuid
    codeParam:
      name: code
      in: path
      required: true
      schema:
        type: string
        minLength: 8
        maxLength: 8
        pattern: '^[a-hj-np-z2-9]{8}$'
    locationIdParam:
      name: locationId
      in: path
      required: true
      schema:
        type: string
        format: uuid
    uploadIdParam:
      name: uploadId
      in: path
      required: true
      schema:
        type: string
        format: uuid
    actionProposalIdParam:
      name: actionProposalId
      in: path
      required: true
      schema:
        type: string
        format: uuid
  schemas:
    Money:
      $ref: './schemas/common.yaml#/Money'
    ErrorEnvelope:
      $ref: './schemas/common.yaml#/ErrorEnvelope'
    CommandEnvelope:
      $ref: './schemas/common.yaml#/CommandEnvelope'
    ProjectionEnvelope:
      $ref: './schemas/common.yaml#/ProjectionEnvelope'
    MeResponse:
      $ref: './schemas/common.yaml#/MeResponse'
    MomentCreateRequest:
      $ref: './schemas/common.yaml#/MomentCreateRequest'
    ExpenseCreateRequest:
      $ref: './schemas/common.yaml#/ExpenseCreateRequest'
    PollResponse:
      $ref: './schemas/common.yaml#/PollResponse'
    CompanyLocationResponse:
      $ref: './schemas/common.yaml#/CompanyLocationResponse'
    ActivityItem:
      $ref: './schemas/common.yaml#/ActivityItem'
`;

const header = `openapi: 3.1.0
info:
  title: Momentra API
  version: 1.0.0
  description: |
    Authoritative /v1 transport contract (Phase 2 freeze).
    PostgreSQL is canonical truth; this spec defines HTTP transport only.
    Work and Finance commands are moment-scoped (single canonical engines).
    Group context uses GROUP-domain moments — no separate /groups CRUD entity.
    Circle full CRUD is CONTRACT_DEFERRED; GET /life360 is the Circle read projection.
servers:
  - url: /v1
    description: Product API
paths:
`;

const root = join(__dirname, '..');
const outPath = join(root, 'openapi', 'momentra-v1.yaml');
writeFileSync(outPath, header + buildPaths(ENDPOINTS) + components, 'utf8');

const inventory = ENDPOINTS.map((e) => ({
  method: e.method.toUpperCase(),
  path: `/v1${e.path}`,
  operationId: e.operationId,
  domain: e.tags[0],
  kind: e.kind,
  auth: true,
  idempotency: !!e.idempotency,
  occ: !!e.occ,
  pagination: !!e.pagination,
  implStatus: e.implStatus,
}));

writeFileSync(join(root, 'openapi', 'endpoint-inventory.json'), JSON.stringify(inventory, null, 2));

console.log('Generated', outPath);
console.log(
  JSON.stringify(
    {
      total: ENDPOINTS.length,
      get: ENDPOINTS.filter((e) => e.method === 'get').length,
      post: ENDPOINTS.filter((e) => e.method === 'post').length,
      patch: ENDPOINTS.filter((e) => e.method === 'patch').length,
      delete: ENDPOINTS.filter((e) => e.method === 'delete').length,
      commands: ENDPOINTS.filter((e) => e.kind === 'command').length,
      reads: ENDPOINTS.filter((e) => e.kind === 'read').length,
    },
    null,
    2
  )
);
