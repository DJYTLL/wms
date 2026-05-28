package com.example.wms.service;

import com.example.wms.dto.TenantDisplaySettingsResponse;
import com.example.wms.dto.TenantDisplaySettingsUpdateRequest;
import com.example.wms.dto.TenantBusinessSettingsResponse;
import com.example.wms.dto.TenantBusinessSettingsUpdateRequest;
import com.example.wms.service.erp.support.FinanceAutoFlowMode;

public interface TenantSettingService {
    TenantDisplaySettingsResponse getDisplaySettings();

    TenantDisplaySettingsResponse updateDisplaySettings(TenantDisplaySettingsUpdateRequest request);

    Integer getConfiguredDefaultPageSize();

    TenantBusinessSettingsResponse getBusinessSettings();

    TenantBusinessSettingsResponse updateBusinessSettings(TenantBusinessSettingsUpdateRequest request);

    FinanceAutoFlowMode getFinanceAutoFlowMode();
}
