import dotenv from 'dotenv';
import path from 'path';

dotenv.config({ path: path.resolve(__dirname, '../../.env') });
dotenv.config({ path: path.resolve(__dirname, '../../../.env') });
// backend/.env wins over backend/typescript/.env
dotenv.config({ path: path.resolve(__dirname, '../../../.env'), override: true });

function requireEnv(name: string, fallback?: string): string {
  const v = process.env[name] ?? fallback;
  if (!v) throw new Error(`Missing required env: ${name}`);
  return v;
}

function envFlag(name: string): boolean {
  return process.env[name] === '1' || process.env[name] === 'true';
}

export interface AppConfig {
  port: number;
  nodeEnv: string;
  isProduction: boolean;
  logLevel: string;
  uuidNamespace: string;
  allowDevAuth: boolean;
  governanceFailOpen: boolean;
  firebase: {
    projectId: string;
    credentialsJson: string;
  };
  database: {
    url: string;
    directUrl: string;
    poolMax: number;
    /** Soft floor of idle connections to keep (prewarm at startup). Prefer 2–3; avoid high min. */
    poolMin: number;
    poolIdleMs: number;
    statementTimeoutMs: number;
    connectionTimeoutMs: number;
  };
  corsOrigins: string[];
  publicAppOrigin: string;
  schemaRelease: string;
  admin: {
    apiKey: string;
    corsOrigins: string[];
  };
}

function loadConfig(): AppConfig {
  const nodeEnv = process.env.NODE_ENV ?? 'development';
  const isProduction = nodeEnv === 'production';
  const allowDevAuth = envFlag('ALLOW_DEV_AUTH');
  const governanceFailOpen = envFlag('GOVERNANCE_FAIL_OPEN');
  const firebaseProjectId = process.env.FIREBASE_PROJECT_ID ?? '';

  if (isProduction) {
    if (allowDevAuth) {
      throw new Error('Production fail-closed: ALLOW_DEV_AUTH must not be enabled.');
    }
    if (governanceFailOpen) {
      throw new Error('Production fail-closed: GOVERNANCE_FAIL_OPEN must not be enabled.');
    }
    if (!firebaseProjectId) {
      throw new Error('Production fail-closed: FIREBASE_PROJECT_ID is required.');
    }
    if (!process.env.DATABASE_URL) {
      throw new Error('Production fail-closed: DATABASE_URL is required.');
    }
  }

  return {
    port: parseInt(process.env.PORT ?? '3000', 10),
    nodeEnv,
    isProduction,
    logLevel: process.env.LOG_LEVEL ?? (isProduction ? 'info' : 'debug'),
    uuidNamespace:
      process.env.MOMENTRA_UUID_NAMESPACE ??
      process.env.MOMENTRA_IDENTITY_NAMESPACE ??
      'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
    allowDevAuth,
    governanceFailOpen,
    firebase: {
      projectId: firebaseProjectId,
      credentialsJson: process.env.FIREBASE_SERVICE_ACCOUNT_JSON ?? '',
    },
    database: {
      url: requireEnv('DATABASE_URL'),
      directUrl: process.env.DATABASE_URL_DIRECT ?? process.env.DATABASE_URL!,
      poolMax: parseInt(process.env.DB_POOL_MAX ?? '10', 10),
      // Default 2: cuts cold connect stampede without parking many idle pooler sockets.
      poolMin: parseInt(process.env.DB_POOL_MIN ?? '2', 10),
      poolIdleMs: parseInt(process.env.DB_POOL_IDLE_MS ?? '30000', 10),
      statementTimeoutMs: parseInt(process.env.DB_STATEMENT_TIMEOUT_MS ?? '30000', 10),
      connectionTimeoutMs: parseInt(process.env.DB_CONNECTION_TIMEOUT_MS ?? '10000', 10),
    },
    corsOrigins: (process.env.CORS_ORIGINS ?? 'http://localhost:3000').split(',').map((s) => s.trim()),
    publicAppOrigin: process.env.PUBLIC_APP_ORIGIN ?? 'https://momentra.app',
    schemaRelease: process.env.SCHEMA_RELEASE ?? 'V001-V049',
    admin: {
      apiKey: process.env.ADMIN_API_KEY ?? '',
      corsOrigins: (process.env.ADMIN_CORS_ORIGINS ?? 'http://localhost:5180').split(',').map((s) => s.trim()),
    },
  };
}

export const config = loadConfig();

/** Used by fail-closed tests without mutating process globals. */
export function assertProductionSafe(env: NodeJS.ProcessEnv = process.env): void {
  if (env.NODE_ENV !== 'production') return;
  if (env.ALLOW_DEV_AUTH === '1' || env.ALLOW_DEV_AUTH === 'true') {
    throw new Error('Production fail-closed: ALLOW_DEV_AUTH must not be enabled.');
  }
  if (env.GOVERNANCE_FAIL_OPEN === '1' || env.GOVERNANCE_FAIL_OPEN === 'true') {
    throw new Error('Production fail-closed: GOVERNANCE_FAIL_OPEN must not be enabled.');
  }
  if (!env.FIREBASE_PROJECT_ID) {
    throw new Error('Production fail-closed: FIREBASE_PROJECT_ID is required.');
  }
  if (!env.DATABASE_URL) {
    throw new Error('Production fail-closed: DATABASE_URL is required.');
  }
}
