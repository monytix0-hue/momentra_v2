BEGIN;

ALTER TABLE collaboration.moment_invite
    DROP CONSTRAINT ck_moment_invite__code;

ALTER TABLE collaboration.moment_invite
    ADD CONSTRAINT ck_moment_invite__code CHECK (
        invite_code ~ '^[a-hj-np-z2-9]{8}$'
        OR invite_code ~ '^[a-z0-9]+-[a-z0-9-]+-[a-f0-9]{8}$'
    );

COMMENT ON COLUMN collaboration.moment_invite.invite_code IS
  'Short public join token (8 chars). Never contains JWTs, user ids, or API origins.';

COMMIT;
