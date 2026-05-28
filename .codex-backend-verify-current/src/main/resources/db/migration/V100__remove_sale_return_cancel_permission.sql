-- Sales returns can only be red-flushed after approval; remove the cancel permission.

DELETE FROM app_role_permission
WHERE permission_id IN (
    SELECT id
    FROM app_permission
    WHERE code = 'erp-sale-return-approved:cancel'
      AND deleted_at IS NULL
);

DELETE FROM app_permission
WHERE code = 'erp-sale-return-approved:cancel'
  AND deleted_at IS NULL;
