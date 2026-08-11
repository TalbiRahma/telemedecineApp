-- MediLink MFA state audit and safe legacy reset (PostgreSQL).
-- Review the SELECT result before running the UPDATE in each environment.
-- Valid rows are:
--   disabled: mfa_enabled = false, pending != true, secret is null/blank
--   pending:  mfa_enabled = false, pending = true,  secret is present
--   enabled:  mfa_enabled = true,  pending = false, secret is present

SELECT id, email, mfa_enabled, mfa_enrollment_pending,
       CASE WHEN secret IS NULL OR btrim(secret) = '' THEN false ELSE true END AS has_secret
FROM _user
WHERE (mfa_enabled = true AND
       (mfa_enrollment_pending IS DISTINCT FROM false OR secret IS NULL OR btrim(secret) = ''))
   OR (mfa_enabled = false AND mfa_enrollment_pending = true AND
       (secret IS NULL OR btrim(secret) = ''))
   OR (mfa_enabled = false AND mfa_enrollment_pending IS DISTINCT FROM true AND
       secret IS NOT NULL AND btrim(secret) <> '');

BEGIN;

-- Fail-safe recovery: remove unverifiable/ambiguous TOTP state. The affected
-- user's current login is rejected by the application; their next password
-- login succeeds without MFA, after which they can use /mfa-setup to enroll.
CREATE TEMP TABLE mfa_legacy_recovery_ids ON COMMIT DROP AS
SELECT id
FROM _user
WHERE (mfa_enabled = true AND
       (mfa_enrollment_pending IS DISTINCT FROM false OR secret IS NULL OR btrim(secret) = ''))
   OR (mfa_enabled = false AND mfa_enrollment_pending = true AND
       (secret IS NULL OR btrim(secret) = ''))
   OR (mfa_enabled = false AND mfa_enrollment_pending IS DISTINCT FROM true AND
       secret IS NOT NULL AND btrim(secret) <> '');

UPDATE token
SET expired = true,
    revoked = true
WHERE user_id IN (SELECT id FROM mfa_legacy_recovery_ids);

UPDATE _user
SET mfa_enabled = false,
    mfa_enrollment_pending = false,
    secret = NULL
WHERE id IN (SELECT id FROM mfa_legacy_recovery_ids);

-- Inspect the affected row count and COMMIT explicitly when satisfied.
-- COMMIT;
ROLLBACK;
