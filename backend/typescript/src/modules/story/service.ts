import { createHash, randomUUID } from 'crypto';
import type { PoolClient } from 'pg';
import type { RequestContext } from '../../platform/request-context/context';
import { AppError, ErrorCode } from '../../platform/errors/errors';
import { getPool, withTransaction } from '../../platform/database/pool';
import { assertGovernanceAllowed } from '../governance/resolver';
import { insertDomainEventAndOutbox } from '../../platform/events/outbox';
import { listOtherMemberUserIds } from '../collaboration/group-membership';
import { buildMomentStorySnapshot, hydrateStoryMoneyCategories, loadFreshStoryPhotos, type StorySnapshot } from './snapshot';
import {
  renderBookletHtml,
  renderChapterSvg,
  renderInteractiveStoryHtml,
  renderShareCoverSvg,
  renderVideoReelSpec,
} from './render';
import { renderStoryPdf } from './pdf';
import {
  getStoryComposer,
  STORY_CHAPTER_ORDER,
  type StoryChapterId,
  type StoryFamilyProfile,
} from './profiles';

function checksum(body: string): string {
  return createHash('sha256').update(body).digest('hex').slice(0, 32);
}

async function insertArtifact(
  client: PoolClient,
  storyId: string,
  artifactType: string,
  contentType: string,
  inlineBody: string
): Promise<string> {
  const artifactId = randomUUID();
  await client.query(
    `INSERT INTO core.moment_story_artifact (
       artifact_id, story_id, artifact_type, content_type, inline_body, checksum, status
     ) VALUES ($1, $2, $3, $4, $5, $6, 'READY')`,
    [artifactId, storyId, artifactType, contentType, inlineBody, checksum(inlineBody)]
  );
  return artifactId;
}

export async function queueMomentStoryGeneration(
  client: PoolClient,
  ctx: RequestContext,
  args: { momentId: string; completionId: string; completedByUserId: string }
): Promise<{ storyId: string; storyVersion: number }> {
  const ver = await client.query<{ v: string }>(
    `SELECT COALESCE(MAX(story_version), 0)::text AS v FROM core.moment_story WHERE moment_id = $1`,
    [args.momentId]
  );
  const storyVersion = parseInt(ver.rows[0]?.v ?? '0', 10) + 1;
  const storyId = randomUUID();

  const family = await client.query<{ group_family: string | null; moment_type_code: string | null }>(
    `SELECT gmc.group_family, mt.code AS moment_type_code
     FROM core.moment m
     LEFT JOIN collaboration.group_moment_context gmc ON gmc.moment_id = m.moment_id
     LEFT JOIN core.moment_type mt ON mt.moment_type_id = m.moment_type_id
     WHERE m.moment_id = $1`,
    [args.momentId]
  );

  const { resolveStoryFamilyProfile } = await import('./profiles');
  const familyProfile = resolveStoryFamilyProfile({
    groupFamily: family.rows[0]?.group_family,
    momentTypeCode: family.rows[0]?.moment_type_code,
  });

  await client.query(
    `INSERT INTO core.moment_story (
       story_id, moment_id, completion_id, story_version, status, family_profile,
       generated_by_user_id, created_at, updated_at
     ) VALUES ($1, $2, $3, $4, 'GENERATING', $5, $6, now(), now())`,
    [storyId, args.momentId, args.completionId, storyVersion, familyProfile, args.completedByUserId]
  );

  // Generate synchronously within the same transaction for reliability in v1.
  await generateMomentStoryArtifacts(client, ctx, storyId);

  return { storyId, storyVersion };
}

export async function generateMomentStoryArtifacts(
  client: PoolClient,
  ctx: RequestContext,
  storyId: string,
  opts?: { notify?: boolean; keepReady?: boolean }
): Promise<void> {
  const story = await client.query<{ moment_id: string; story_version: number }>(
    `SELECT moment_id, story_version FROM core.moment_story WHERE story_id = $1`,
    [storyId]
  );
  const row = story.rows[0];
  if (!row) throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Story not found.', 404);

  try {
    const { snapshot, sourceManifest } = await buildMomentStorySnapshot(client, row.moment_id);
    const snapshotId = randomUUID();
    await client.query(
      `INSERT INTO core.moment_story_snapshot (snapshot_id, story_id, snapshot_json, source_manifest)
       VALUES ($1, $2, $3::jsonb, $4::jsonb)
       ON CONFLICT (story_id) DO UPDATE
       SET snapshot_json = EXCLUDED.snapshot_json, source_manifest = EXCLUDED.source_manifest`,
      [snapshotId, storyId, JSON.stringify(snapshot), JSON.stringify(sourceManifest)]
    );

    await insertArtifact(client, storyId, 'STORY_PAYLOAD', 'application/json', JSON.stringify(snapshot));
    const html = renderInteractiveStoryHtml(snapshot);
    await insertArtifact(client, storyId, 'CHAPTER_HTML', 'text/html; charset=utf-8', html);
    const booklet = renderBookletHtml(snapshot);
    await insertArtifact(client, storyId, 'BOOKLET_HTML', 'text/html; charset=utf-8', booklet);
    const cover = renderShareCoverSvg(snapshot);
    await insertArtifact(client, storyId, 'SHARE_COVER', 'image/svg+xml', cover);

    const chapterSvgs: Record<string, string> = {};
    for (const chapter of snapshot.chapters) {
      chapterSvgs[chapter] = renderChapterSvg(snapshot, chapter);
    }
    await insertArtifact(
      client,
      storyId,
      'CHAPTER_PNG_SVG',
      'application/json',
      JSON.stringify(chapterSvgs)
    );

    const reel = renderVideoReelSpec(snapshot);
    await insertArtifact(client, storyId, 'VIDEO_REEL_SPEC', 'application/json', JSON.stringify(reel));

    await client.query(
      `UPDATE core.moment_story
       SET status = 'READY', generated_at = now(), updated_at = now(), family_profile = $2
       WHERE story_id = $1`,
      [storyId, snapshot.identity.familyProfile]
    );

    if (opts?.notify !== false) {
      const peers = await listOtherMemberUserIds(client, row.moment_id, ctx.userId);
      const allTargets = Array.from(new Set([ctx.userId, ...peers]));
      await insertDomainEventAndOutbox(client, ctx, {
        eventName: 'MomentStoryReady',
        domainCode: 'GROUP',
        aggregateType: 'MOMENT',
        aggregateId: row.moment_id,
        payload: {
          momentId: row.moment_id,
          storyId,
          storyVersion: row.story_version,
          title: snapshot.identity.title,
          targetUserIds: allTargets,
          deepLink: `momentra://moments/${row.moment_id}/story`,
        },
      });
    }
  } catch (err) {
    const message = err instanceof Error ? err.message : 'Story generation failed';
    if (!opts?.keepReady) {
      try {
        await client.query(
          `UPDATE core.moment_story
           SET status = 'FAILED', error_message = $2, updated_at = now()
           WHERE story_id = $1`,
          [storyId, message.slice(0, 500)]
        );
      } catch {
        // The surrounding transaction may already be aborted. The caller records FAILED separately.
      }
    }
    throw err;
  }
}

/** Persist a failed story on its own connection so a rolled-back completion transaction cannot erase it. */
export async function recordStoryGenerationFailure(args: {
  momentId: string;
  completionId: string;
  completedByUserId: string;
  message: string;
}): Promise<void> {
  const client = await getPool().connect();
  try {
    const ver = await client.query<{ v: string }>(
      `SELECT COALESCE(MAX(story_version), 0)::text AS v FROM core.moment_story WHERE moment_id = $1`,
      [args.momentId]
    );
    const storyVersion = parseInt(ver.rows[0]?.v ?? '0', 10) + 1;
    await client.query(
      `INSERT INTO core.moment_story (
         story_id, moment_id, completion_id, story_version, status, family_profile,
         generated_by_user_id, error_message, created_at, updated_at
       ) VALUES ($1, $2, $3, $4, 'FAILED', 'SHARED_EXPERIENCE', $5, $6, now(), now())`,
      [
        randomUUID(),
        args.momentId,
        args.completionId,
        storyVersion,
        args.completedByUserId,
        args.message.slice(0, 500),
      ]
    );
  } finally {
    client.release();
  }
}

export async function getMomentStoryStatus(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string
): Promise<{
  status: 'NOT_STARTED' | 'GENERATING' | 'READY' | 'FAILED';
  storyId: string | null;
  storyVersion: number | null;
  familyProfile: string | null;
  errorMessage: string | null;
}> {
  await assertGovernanceAllowed(client, ctx, { actionCode: 'GROUP_ACCESS', resourceType: 'MOMENT', momentId });
  const ready = await client.query<{
    story_id: string;
    story_version: number;
    family_profile: string;
  }>(
    `SELECT story_id, story_version, family_profile
     FROM core.moment_story
     WHERE moment_id = $1 AND status = 'READY'
     ORDER BY story_version DESC LIMIT 1`,
    [momentId]
  );
  if (ready.rows[0]) {
    return {
      status: 'READY',
      storyId: ready.rows[0].story_id,
      storyVersion: ready.rows[0].story_version,
      familyProfile: ready.rows[0].family_profile,
      errorMessage: null,
    };
  }
  const row = await client.query<{
    story_id: string;
    status: string;
    story_version: number;
    family_profile: string;
    error_message: string | null;
  }>(
    `SELECT story_id, status, story_version, family_profile, error_message
     FROM core.moment_story WHERE moment_id = $1
     ORDER BY story_version DESC LIMIT 1`,
    [momentId]
  );
  if (!row.rows[0]) {
    return { status: 'NOT_STARTED', storyId: null, storyVersion: null, familyProfile: null, errorMessage: null };
  }
  const r = row.rows[0];
  return {
    status: r.status as 'GENERATING' | 'READY' | 'FAILED',
    storyId: r.story_id,
    storyVersion: r.story_version,
    familyProfile: r.family_profile,
    errorMessage: r.error_message,
  };
}

export async function getMomentStory(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string
): Promise<{
  storyId: string;
  storyVersion: number;
  status: string;
  familyProfile: string;
  snapshot: StorySnapshot;
  chapters: StoryChapterId[];
}> {
  await assertGovernanceAllowed(client, ctx, { actionCode: 'GROUP_ACCESS', resourceType: 'MOMENT', momentId });
  // Story reads use a non-transaction connection, so a savepoint is illegal here.
  // Refresh on its own transaction: a failure rolls back and the existing READY story is still returned.
  try {
    await withTransaction(
      (tx) => refreshReadyMomentStory(tx, ctx, momentId, { keepReady: true }),
      ctx.userId
    );
  } catch (err) {
    console.log(
      JSON.stringify({ level: 'warn', msg: 'story_refresh_on_read_failed', momentId, err: String(err) })
    );
  }
  const story = await client.query<{
    story_id: string;
    story_version: number;
    status: string;
    family_profile: string;
  }>(
    `SELECT story_id, story_version, status, family_profile
     FROM core.moment_story
     WHERE moment_id = $1 AND status = 'READY'
     ORDER BY story_version DESC LIMIT 1`,
    [momentId]
  );
  if (!story.rows[0]) {
    throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Story not found.', 404);
  }
  const snap = await client.query<{ snapshot_json: StorySnapshot }>(
    `SELECT snapshot_json FROM core.moment_story_snapshot WHERE story_id = $1`,
    [story.rows[0].story_id]
  );
  if (!snap.rows[0]) {
    throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Story snapshot not ready.', 404);
  }
  const snapshot = snap.rows[0].snapshot_json;
  // Re-sign memory media on every read — frozen snapshot URLs expire (~1h).
  const freshPhotos = await loadFreshStoryPhotos(client, momentId, 5);
  const hydrated: StorySnapshot = hydrateStoryMoneyCategories({
    ...snapshot,
    photos: freshPhotos,
  });
  return {
    storyId: story.rows[0].story_id,
    storyVersion: story.rows[0].story_version,
    status: story.rows[0].status,
    familyProfile: story.rows[0].family_profile,
    snapshot: hydrated,
    chapters: hydrated.chapters ?? STORY_CHAPTER_ORDER,
  };
}

export async function getStoryArtifact(
  client: PoolClient,
  ctx: RequestContext,
  storyId: string,
  artifactType: string
): Promise<{ artifactId: string; contentType: string; body: string }> {
  const story = await client.query<{ moment_id: string }>(
    `SELECT moment_id FROM core.moment_story WHERE story_id = $1`,
    [storyId]
  );
  if (!story.rows[0]) throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Story not found.', 404);
  await assertGovernanceAllowed(client, ctx, {
    actionCode: 'GROUP_ACCESS',
    resourceType: 'MOMENT',
    momentId: story.rows[0].moment_id,
  });
  const art = await client.query<{ artifact_id: string; content_type: string; inline_body: string | null }>(
    `SELECT artifact_id, content_type, inline_body
     FROM core.moment_story_artifact
     WHERE story_id = $1 AND artifact_type = $2
     ORDER BY generated_at DESC LIMIT 1`,
    [storyId, artifactType]
  );
  if (!art.rows[0]?.inline_body) {
    throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Artifact not found.', 404);
  }
  return {
    artifactId: art.rows[0].artifact_id,
    contentType: art.rows[0].content_type,
    body: art.rows[0].inline_body,
  };
}

const DEFAULT_PUBLIC_WEB_BASE = 'https://api.momentra.tech';

export function publicStoryWebBase(): string {
  const configured = process.env.PUBLIC_WEB_BASE_URL?.trim().replace(/\/$/, '');
  return configured || DEFAULT_PUBLIC_WEB_BASE;
}

/** Rebuild the latest READY snapshot in place after a completed group moment's money changes. */
export async function refreshReadyMomentStory(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  opts?: { keepReady?: boolean }
): Promise<void> {
  const moment = await client.query<{ status: string; domain_code: string }>(
    `SELECT status, domain_code FROM core.moment WHERE moment_id = $1`,
    [momentId]
  );
  const row = moment.rows[0];
  if (!row || row.domain_code !== 'GROUP' || row.status !== 'COMPLETED') return;
  const story = await client.query<{ story_id: string }>(
    `SELECT story_id FROM core.moment_story
     WHERE moment_id = $1 AND status = 'READY'
     ORDER BY story_version DESC LIMIT 1`,
    [momentId]
  );
  const storyId = story.rows[0]?.story_id;
  if (!storyId) return;
  await generateMomentStoryArtifacts(client, ctx, storyId, { notify: false, keepReady: opts?.keepReady });
  await client.query(
    `UPDATE core.moment_story
     SET data_version = data_version + 1, updated_at = now()
     WHERE story_id = $1`,
    [storyId]
  );
}

/** Runs after the expense transaction commits. A failure here does not undo the expense. */
export async function refreshStoryAfterExpense(ctx: RequestContext, momentId: string): Promise<void> {
  try {
    await withTransaction((client) => refreshReadyMomentStory(client, ctx, momentId), ctx.userId);
  } catch (err) {
    console.log(JSON.stringify({ level: 'warn', msg: 'story_refresh_failed', momentId, err: String(err) }));
  }
}

export async function createStoryShare(
  client: PoolClient,
  ctx: RequestContext,
  storyId: string
): Promise<{ shareId: string; shareToken: string; webPath: string }> {
  const story = await client.query<{ moment_id: string; status: string }>(
    `SELECT moment_id, status FROM core.moment_story WHERE story_id = $1`,
    [storyId]
  );
  if (!story.rows[0]) throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Story not found.', 404);
  if (story.rows[0].status !== 'READY') {
    throw new AppError(ErrorCode.VALIDATION_FAILED, 'Story is not ready to share.', 400);
  }
  await assertGovernanceAllowed(client, ctx, {
    actionCode: 'GROUP_ACCESS',
    resourceType: 'MOMENT',
    momentId: story.rows[0].moment_id,
  });
  const shareId = randomUUID();
  const shareToken = randomUUID().replace(/-/g, '') + randomUUID().replace(/-/g, '').slice(0, 8);
  await client.query(
    `INSERT INTO core.moment_story_share (
       share_id, story_id, share_token, created_by_user_id, access_mode, expires_at
     ) VALUES ($1, $2, $3, $4, 'LINK_VIEW', now() + interval '30 days')`,
    [shareId, storyId, shareToken, ctx.userId]
  );
  return {
    shareId,
    shareToken,
    webPath: `/story/${shareToken}`,
  };
}

export async function getSharePack(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string
): Promise<{
  coverSvg: string;
  blurb: string;
  webUrl: string | null;
  webPath: string | null;
  appDeepLink: string;
  storyId: string;
  title: string;
}> {
  const story = await getMomentStory(client, ctx, momentId);
  if (story.status !== 'READY') {
    throw new AppError(ErrorCode.VALIDATION_FAILED, 'Story is not ready.', 400);
  }
  const existing = await client.query<{ share_token: string }>(
    `SELECT share_token FROM core.moment_story_share
     WHERE story_id = $1
       AND revoked_at IS NULL
       AND (expires_at IS NULL OR expires_at > now())
     ORDER BY created_at DESC
     LIMIT 1`,
    [story.storyId]
  );
  const share = existing.rows[0]
    ? { shareToken: existing.rows[0].share_token, webPath: `/story/${existing.rows[0].share_token}` }
    : await createStoryShare(client, ctx, story.storyId);
  const cover = await getStoryArtifact(client, ctx, story.storyId, 'SHARE_COVER');
  const metrics = story.snapshot.metrics ?? {};
  const title = story.snapshot.identity.title;
  const profile = (story.familyProfile ||
    story.snapshot.identity.familyProfile ||
    'SHARED_EXPERIENCE') as StoryFamilyProfile;
  const blurb = getStoryComposer(profile).shareBlurb(title, metrics);
  const webPath = share.webPath;
  const webUrl = `${publicStoryWebBase()}${webPath}`;
  return {
    coverSvg: cover.body,
    blurb,
    webUrl,
    webPath,
    appDeepLink: `momentra://moments/${momentId}/story`,
    storyId: story.storyId,
    title,
  };
}

/** Public (token) web story — no auth; audience-safe snapshot only. */
export async function getPublicStoryByToken(
  client: PoolClient,
  shareToken: string
): Promise<{ html: string; title: string; revoked: boolean }> {
  const share = await client.query<{
    story_id: string;
    revoked_at: Date | null;
    expires_at: Date | null;
  }>(
    `SELECT story_id, revoked_at, expires_at FROM core.moment_story_share WHERE share_token = $1`,
    [shareToken]
  );
  const s = share.rows[0];
  if (!s || s.revoked_at || (s.expires_at && s.expires_at.getTime() < Date.now())) {
    return { html: '<html><body><p>This Moment Story link is no longer available.</p></body></html>', title: 'Unavailable', revoked: true };
  }
  const snapRow = await client.query<{ moment_id: string; snapshot_json: StorySnapshot; title: string | null }>(
    `SELECT st.moment_id, ss.snapshot_json, ss.snapshot_json->'identity'->>'title' AS title
     FROM core.moment_story st
     JOIN core.moment_story_snapshot ss ON ss.story_id = st.story_id
     WHERE st.story_id = $1`,
    [s.story_id]
  );
  const stored = snapRow.rows[0];
  if (!stored) {
    return { html: '<html><body><p>Story not ready.</p></body></html>', title: 'Moment Story', revoked: false };
  }
  const freshPhotos = await loadFreshStoryPhotos(client, stored.moment_id, 5);
  const hydrated = hydrateStoryMoneyCategories({ ...stored.snapshot_json, photos: freshPhotos });
  const webUrl = `${publicStoryWebBase()}/story/${shareToken}`;
  return {
    html: renderInteractiveStoryHtml(hydrated, { ogUrl: webUrl }),
    title: stored.title ?? hydrated.identity.title ?? 'Moment Story',
    revoked: false,
  };
}

export async function revokeStoryShare(
  client: PoolClient,
  ctx: RequestContext,
  shareId: string
): Promise<{ shareId: string; revokedAt: string }> {
  const share = await client.query<{ story_id: string; moment_id: string }>(
    `SELECT s.story_id, st.moment_id
     FROM core.moment_story_share s
     JOIN core.moment_story st ON st.story_id = s.story_id
     WHERE s.share_id = $1`,
    [shareId]
  );
  if (!share.rows[0]) throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Share not found.', 404);
  await assertGovernanceAllowed(client, ctx, {
    actionCode: 'GROUP_ACCESS',
    resourceType: 'MOMENT',
    momentId: share.rows[0].moment_id,
  });
  const updated = await client.query<{ revoked_at: Date }>(
    `UPDATE core.moment_story_share SET revoked_at = now()
     WHERE share_id = $1 RETURNING revoked_at`,
    [shareId]
  );
  return { shareId, revokedAt: updated.rows[0].revoked_at.toISOString() };
}

export async function renderMomentStoryPdf(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string
): Promise<Buffer> {
  const story = await getMomentStory(client, ctx, momentId);
  return renderStoryPdf(story.snapshot);
}

export async function regenerateMomentStory(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string
): Promise<{ storyId: string; storyVersion: number }> {
  await assertGovernanceAllowed(client, ctx, { actionCode: 'GROUP_ACCESS', resourceType: 'MOMENT', momentId });
  const completionId = randomUUID();
  return queueMomentStoryGeneration(client, ctx, {
    momentId,
    completionId,
    completedByUserId: ctx.userId,
  });
}
