import * as Sentry from '@sentry/node';

let initialized = false;

export function initSentry(): void {
  const dsn = process.env.SENTRY_DSN?.trim();
  if (!dsn || initialized) return;
  Sentry.init({
    dsn,
    environment: process.env.NODE_ENV ?? 'development',
    tracesSampleRate: 0.1,
  });
  initialized = true;
  console.log(JSON.stringify({ level: 'info', msg: 'sentry_initialized' }));
}

export function captureException(err: unknown, extras?: Record<string, unknown>): void {
  if (!initialized) return;
  Sentry.withScope((scope) => {
    if (extras) {
      for (const [k, v] of Object.entries(extras)) {
        scope.setExtra(k, v);
      }
    }
    Sentry.captureException(err);
  });
}
