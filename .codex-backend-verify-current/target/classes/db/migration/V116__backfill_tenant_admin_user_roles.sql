-- Backfill missing tenant admin user-role bindings.
-- Some existing tenants have an admin user and admin role, but no app_user_role row.

WITH inserted AS (
    INSERT INTO app_user_role (tenant_id, user_id, role_id, created_at, updated_at)
    SELECT u.tenant_id, u.id, r.id, NOW(), NOW()
    FROM app_user u
    JOIN app_tenant t
      ON t.id = u.tenant_id
     AND t.deleted_at IS NULL
    JOIN app_role r
      ON r.tenant_id = u.tenant_id
     AND r.code = 'admin'
     AND r.deleted_at IS NULL
    WHERE u.username = 'admin'
      AND u.deleted_at IS NULL
      AND NOT EXISTS (
          SELECT 1
          FROM app_user_role existing
          WHERE existing.tenant_id = u.tenant_id
            AND existing.user_id = u.id
            AND existing.role_id = r.id
            AND existing.deleted_at IS NULL
      )
    RETURNING tenant_id, user_id
)
UPDATE app_user u
SET auth_version = auth_version + 1,
    updated_at = NOW()
FROM inserted
WHERE u.tenant_id = inserted.tenant_id
  AND u.id = inserted.user_id;
