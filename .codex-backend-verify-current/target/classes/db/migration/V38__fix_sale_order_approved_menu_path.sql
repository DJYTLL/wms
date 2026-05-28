-- 修正销售单（已审核）菜单路径，避免误跳到销售退货
UPDATE app_menu
SET path = '/erp/sale-orders/approved',
    updated_at = NOW()
WHERE code = 'erp-sale-approved'
  AND (path IS NULL OR path <> '/erp/sale-orders/approved');

