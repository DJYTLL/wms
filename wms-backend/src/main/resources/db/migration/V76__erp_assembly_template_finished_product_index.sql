CREATE INDEX idx_erp_assembly_template_finished_product
    ON erp_assembly_template (tenant_id, order_type, finished_product_id, updated_at DESC)
    WHERE deleted_at IS NULL;
