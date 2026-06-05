package com.example.wms.dto.erp;

public record ErpProductImportFieldOption(
    String key,
    String label,
    boolean required,
    Long customerCategoryId
) {
    public ErpProductImportFieldOption(String key, String label, boolean required) {
        this(key, label, required, null);
    }
}
