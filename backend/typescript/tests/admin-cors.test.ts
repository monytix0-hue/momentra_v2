/**
 * Admin dashboard runs on its own origin (ADMIN_CORS_ORIGINS) and sends X-Admin-Key,
 * so every call is preflighted. Production used to consult CORS_ORIGINS only, which
 * left the deployed dashboard with no Access-Control-Allow-Origin header.
 */
process.env.ALLOW_DEV_AUTH = '1';

import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import express from 'express';
import request from 'supertest';
import { adminCors, createApp } from '../src/app';
import { config, normalizeOrigins } from '../src/platform/config';

const ADMIN_ORIGIN = 'https://admin.momentra.tech';

function appWithAdminCors(origins: string[]): express.Express {
  const app = express();
  app.use('/admin/api', adminCors(origins));
  app.get('/admin/api/telemetry/overview', (_req, res) => {
    res.json({ ok: true });
  });
  return app;
}

describe('admin CORS', () => {
  it('answers the X-Admin-Key preflight for a configured admin origin', async () => {
    const res = await request(appWithAdminCors([ADMIN_ORIGIN]))
      .options('/admin/api/telemetry/overview')
      .set('Origin', ADMIN_ORIGIN)
      .set('Access-Control-Request-Method', 'GET')
      .set('Access-Control-Request-Headers', 'x-admin-key');
    assert.equal(res.headers['access-control-allow-origin'], ADMIN_ORIGIN);
    assert.match(res.headers['access-control-allow-headers'] ?? '', /x-admin-key/i);
  });

  it('sets the origin header on the actual admin request too', async () => {
    const res = await request(appWithAdminCors([ADMIN_ORIGIN]))
      .get('/admin/api/telemetry/overview')
      .set('Origin', ADMIN_ORIGIN)
      .set('X-Admin-Key', 'irrelevant-here');
    assert.equal(res.status, 200);
    assert.equal(res.headers['access-control-allow-origin'], ADMIN_ORIGIN);
  });

  it('withholds the origin header from an unlisted origin', async () => {
    const res = await request(appWithAdminCors([ADMIN_ORIGIN]))
      .options('/admin/api/telemetry/overview')
      .set('Origin', 'https://evil.example.com')
      .set('Access-Control-Request-Method', 'GET')
      .set('Access-Control-Request-Headers', 'x-admin-key');
    assert.equal(res.headers['access-control-allow-origin'], undefined);
  });

  it('honours ADMIN_CORS_ORIGINS on the real app regardless of NODE_ENV', async () => {
    const configured = config.admin.corsOrigins[0];
    assert.ok(configured, 'ADMIN_CORS_ORIGINS must resolve to at least one origin');

    const res = await request(createApp())
      .options('/admin/api/telemetry/overview')
      .set('Origin', configured)
      .set('Access-Control-Request-Method', 'GET')
      .set('Access-Control-Request-Headers', 'x-admin-key');
    assert.equal(res.headers['access-control-allow-origin'], configured);
  });

  it('normalizeOrigins strips trailing slashes, whitespace, and empty segments', () => {
    assert.deepEqual(normalizeOrigins('https://admin.momentra.tech/', 'fallback'), [ADMIN_ORIGIN]);
    assert.deepEqual(normalizeOrigins('https://admin.momentra.tech///', 'fallback'), [ADMIN_ORIGIN]);
    assert.deepEqual(normalizeOrigins('  https://a.test , https://b.test/ ', 'fallback'), [
      'https://a.test',
      'https://b.test',
    ]);
    assert.deepEqual(normalizeOrigins('https://a.test,,', 'fallback'), ['https://a.test']);
    // Semicolon separator is a common env typo — it must not collapse into one bogus origin.
    assert.deepEqual(normalizeOrigins('http://localhost:5180;https://admin.momentra.tech/', 'fallback'), [
      'http://localhost:5180',
      ADMIN_ORIGIN,
    ]);
    assert.deepEqual(normalizeOrigins(undefined, 'http://localhost:5180'), ['http://localhost:5180']);
  });
});
