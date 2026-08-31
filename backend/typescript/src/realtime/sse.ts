import type { Request, Response } from 'express';
import { verifyFirebaseToken, resolveIdentityFromToken, resolveDevIdentity } from '../platform/auth';
import { config } from '../platform/config';
import { firebaseUserId } from '../platform/auth/uuid';

interface SseClient {
  userId: string;
  res: Response;
}

const clients = new Set<SseClient>();

export function attachSseRoutes(app: import('express').Express): void {
  app.get('/v1/realtime/sse', sseHandler);
}

async function sseHandler(req: Request, res: Response): Promise<void> {
  try {
    let userId: string;

    if (!config.firebase.projectId) {
      const devUid = req.header('x-dev-firebase-uid')?.trim() || 'dev-local-user';
      userId = resolveDevIdentity(devUid).userId;
    } else {
      const header = req.header('authorization');
      if (!header?.startsWith('Bearer ')) {
        res.status(401).json({ code: 'UNAUTHORIZED', message: 'Missing Firebase Bearer token.' });
        return;
      }
      const token = header.slice(7);
      const decoded = await verifyFirebaseToken(token);
      userId = resolveIdentityFromToken(decoded).userId;
    }

    res.setHeader('Content-Type', 'text/event-stream');
    res.setHeader('Cache-Control', 'no-cache');
    res.setHeader('Connection', 'keep-alive');
    res.flushHeaders?.();

    const client: SseClient = { userId, res };
    clients.add(client);

    res.write(`event: connected\ndata: ${JSON.stringify({ userId })}\n\n`);

    req.on('close', () => {
      clients.delete(client);
    });
  } catch {
    if (!res.headersSent) {
      res.status(401).json({ code: 'UNAUTHORIZED', message: 'Invalid Firebase token.' });
    }
  }
}

export function publishProjectionUpdated(
  userId: string,
  projections: string[],
  correlationId: string,
  projectionVersion = Date.now()
): void {
  const payload = JSON.stringify({
    type: 'PROJECTION_UPDATED',
    projection: projections[0],
    projections,
    scopeType: 'USER',
    scopeId: userId,
    projectionVersion,
    correlationId,
  });

  for (const client of clients) {
    if (client.userId === userId && !client.res.writableEnded) {
      client.res.write(`event: PROJECTION_UPDATED\ndata: ${payload}\n\n`);
    }
  }
}

export { firebaseUserId };
