import type { ProjectionCode } from '../../platform/projections/hints';

export function projectionCodesForDomain(domainCode: string): ProjectionCode[] {
  switch (domainCode) {
    case 'PERSONAL':
      return ['personal.moments', 'personal.pulse'];
    case 'GROUP':
      return ['group.moments', 'group.pulse'];
    case 'BUSINESS':
      return ['business.moments', 'business.pulse'];
    default:
      return [];
  }
}
