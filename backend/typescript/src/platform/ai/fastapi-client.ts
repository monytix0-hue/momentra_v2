/**
 * Internal TypeScript → FastAPI client.
 * Optional compute plane — failures must not break core CRUD.
 * Core /health/ready does NOT depend on FastAPI.
 */
import { randomUUID } from 'crypto';

export const ANALYTICS_CONTRACT_VERSION = '1';

export type AuthorizedFact = {
  code: string;
  value: unknown;
  unit?: string | null;
  label?: string | null;
};

export type AnalyticsInput = {
  contractVersion: string;
  userId: string;
  context: string;
  companyId?: string | null;
  momentId?: string | null;
  currency?: string | null;
  timeWindow: string;
  metricCode?: string | null;
  sourceVersion: string;
  authorizedFacts: AuthorizedFact[];
  correlationId?: string | null;
  purpose?: string;
};

export type ComputeResult = {
  contractVersion: string;
  status: string;
  version: string;
  computedAt: string;
  dataThrough?: string | null;
  title?: string | null;
  body?: string | null;
  insightCode?: string | null;
  severity?: string | null;
  provider?: string;
  latencyMs?: number;
  correlationId?: string | null;
};

type CircuitState = {
  failures: number;
  openUntilMs: number;
};

const circuit: CircuitState = { failures: 0, openUntilMs: 0 };

function baseUrl(): string | null {
  const raw = process.env.FASTAPI_AI_URL?.trim() || process.env.MOMENTRA_AI_URL?.trim();
  return raw ? raw.replace(/\/$/, '') : null;
}

function internalKey(): string {
  return process.env.MOMENTRA_AI_INTERNAL_KEY?.trim() ?? '';
}

function timeoutMs(): number {
  return parseInt(process.env.FASTAPI_AI_TIMEOUT_MS ?? '4000', 10);
}

export function fastapiConfigured(): boolean {
  return Boolean(baseUrl());
}

export function fastapiCircuitOpen(): boolean {
  return Date.now() < circuit.openUntilMs;
}

function recordFailure(): void {
  circuit.failures += 1;
  if (circuit.failures >= 3) {
    circuit.openUntilMs = Date.now() + 30_000;
    circuit.failures = 0;
  }
}

function recordSuccess(): void {
  circuit.failures = 0;
  circuit.openUntilMs = 0;
}

export async function callNarrativeCompute(
  input: AnalyticsInput,
  opts?: { correlationId?: string },
): Promise<ComputeResult | null> {
  const url = baseUrl();
  if (!url) return null;
  if (fastapiCircuitOpen()) {
    console.log(JSON.stringify({ level: 'warn', msg: 'fastapi_circuit_open' }));
    return null;
  }

  const correlationId = opts?.correlationId ?? input.correlationId ?? randomUUID();
  const body: AnalyticsInput = {
    ...input,
    contractVersion: input.contractVersion || ANALYTICS_CONTRACT_VERSION,
    correlationId,
    purpose: input.purpose ?? 'narrative',
  };

  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs());
  const started = Date.now();
  try {
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      Accept: 'application/json',
      'X-Correlation-Id': correlationId,
    };
    const key = internalKey();
    if (key) headers['X-Momentra-Internal-Key'] = key;

    let lastErr: unknown;
    for (let attempt = 0; attempt < 2; attempt++) {
      try {
        const res = await fetch(`${url}/v1/compute/narrative`, {
          method: 'POST',
          headers,
          body: JSON.stringify(body),
          signal: controller.signal,
        });
        if (!res.ok) {
          lastErr = new Error(`fastapi_http_${res.status}`);
          if (res.status >= 500) continue;
          recordFailure();
          return null;
        }
        const json = (await res.json()) as ComputeResult;
        recordSuccess();
        console.log(
          JSON.stringify({
            level: 'info',
            msg: 'fastapi_narrative_ok',
            correlationId,
            durationMs: Date.now() - started,
            insightCode: json.insightCode,
          }),
        );
        return json;
      } catch (e) {
        lastErr = e;
      }
    }
    recordFailure();
    console.log(
      JSON.stringify({
        level: 'warn',
        msg: 'fastapi_narrative_failed',
        correlationId,
        durationMs: Date.now() - started,
        err: String(lastErr),
      }),
    );
    return null;
  } finally {
    clearTimeout(timer);
  }
}

/** Test helper — reset circuit. */
export function resetFastapiCircuitForTests(): void {
  circuit.failures = 0;
  circuit.openUntilMs = 0;
}
