/** Stable notification thread keys for inbox + OS grouping. */

export type ThreadDomain = 'PERSONAL' | 'GROUP' | 'BUSINESS';

export function threadKeyFor(input: {
  domainCode?: string | null;
  momentId?: string | null;
  companyId?: string | null;
  eventName?: string | null;
}): string {
  const momentId = input.momentId?.trim() || null;
  const companyId = input.companyId?.trim() || null;
  const domain = (input.domainCode ?? '').toUpperCase();

  if (domain === 'BUSINESS' && companyId && momentId) {
    return `BUSINESS:${companyId}:${momentId}`;
  }
  if (domain === 'PERSONAL' && momentId) {
    return `PERSONAL:${momentId}`;
  }
  if (momentId) {
    return `GROUP:${momentId}`;
  }
  if (input.eventName === 'DigestReady') {
    return 'DIGEST:inbox';
  }
  return `EVENT:${input.eventName ?? 'unknown'}`;
}

/** Collapse only when a later notification intentionally replaces an earlier one (e.g. digest). */
export function collapseKeyFor(input: {
  threadKey: string;
  replacePrior: boolean;
}): string | null {
  return input.replacePrior ? input.threadKey : null;
}
