const SENSITIVE_KEY =
  /(note|description|body|amount|token|authorization|password|signed.?url|download.?url|mood|feeling|memory|content|push.?token|secret)/i;

const SENSITIVE_VALUE =
  /(bearer\s+[a-z0-9._-]{12,}|eyJ[A-Za-z0-9_-]{12,}|[?&]token=|supabase\.co\/storage\/v1\/object\/sign\/)/i;

const REDACTED = '[redacted]';

/** Strip notes, memory/mood text, amounts, tokens, and signed URLs before a value is logged. */
export function redactForLog(value: unknown, key?: string): unknown {
  if (key && SENSITIVE_KEY.test(key)) return REDACTED;
  if (typeof value === 'string') {
    return SENSITIVE_VALUE.test(value) ? REDACTED : value;
  }
  if (Array.isArray(value)) return value.map((item) => redactForLog(item));
  if (value && typeof value === 'object') {
    const out: Record<string, unknown> = {};
    for (const [childKey, child] of Object.entries(value as Record<string, unknown>)) {
      out[childKey] = redactForLog(child, childKey);
    }
    return out;
  }
  return value;
}
