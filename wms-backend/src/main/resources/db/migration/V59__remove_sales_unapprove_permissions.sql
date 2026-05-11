-- 销售流程只允许红冲，不再保留销售/销售退货反审核权限。
DELETE FROM app_role_permission
WHERE permission_id IN (
    SELECT id
    FROM app_permission
    WHERE code IN ('erp-sale:unapprove', 'erp-sale-return:unapprove')
);

DELETE FROM app_permission
WHERE code IN ('erp-sale:unapprove', 'erp-sale-return:unapprove');
