package com.example.wms.service;

import com.example.wms.dto.TenantColumnSettingRequest;
import com.example.wms.dto.TenantColumnSettingResponse;

// 租户列配置服务
public interface TenantColumnSettingService {
    TenantColumnSettingResponse getByPageKey(String pageKey);

    TenantColumnSettingResponse update(String pageKey, TenantColumnSettingRequest request);

    TenantColumnSettingResponse getByTenantAndPageKey(Long tenantId, String pageKey);

    TenantColumnSettingResponse updateForTenant(Long tenantId, String pageKey, TenantColumnSettingRequest request);
}
