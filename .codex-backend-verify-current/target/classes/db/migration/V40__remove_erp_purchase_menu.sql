-- Remove legacy purchase menu group now that draft/approved are top-level under ERP.
UPDATE app_menu
SET parent_id = (SELECT id FROM app_menu WHERE code = 'erp')
WHERE code IN ('erp-purchase-draft', 'erp-purchase-approved');

DELETE FROM app_tenant_menu
WHERE menu_id IN (SELECT id FROM app_menu WHERE code = 'erp-purchase');

DELETE FROM app_menu
WHERE code = 'erp-purchase';
