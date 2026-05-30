ALTER TABLE erp_supplier_import_batch
    ADD COLUMN IF NOT EXISTS uncategorized_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE erp_supplier_import_batch
    ADD COLUMN IF NOT EXISTS settlement_unmatched_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE erp_supplier_import_batch
    ADD COLUMN IF NOT EXISTS pending_subject_merge_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE erp_supplier_import_item
    ADD COLUMN IF NOT EXISTS warning_message TEXT;

ALTER TABLE erp_supplier_import_item
    ADD COLUMN IF NOT EXISTS matched_strategy VARCHAR(64);

INSERT INTO erp_supplier_type (
    tenant_id,
    code,
    name,
    enabled,
    sort,
    remark,
    created_at,
    updated_at
)
SELECT
    t.id,
    'UNCATEGORIZED',
    '未分类',
    TRUE,
    -999,
    '系统内置未分类供应商类型',
    NOW(),
    NOW()
FROM app_tenant t
WHERE t.deleted_at IS NULL
  AND NOT EXISTS (
    SELECT 1
    FROM erp_supplier_type st
    WHERE st.tenant_id = t.id
      AND st.code = 'UNCATEGORIZED'
      AND st.deleted_at IS NULL
);
