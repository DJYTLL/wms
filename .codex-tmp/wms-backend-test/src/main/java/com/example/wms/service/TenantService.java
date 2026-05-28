package com.example.wms.service;

import com.example.wms.dto.TenantCreateRequest;
import com.example.wms.dto.TenantResponse;
import com.example.wms.dto.TenantStatusUpdateRequest;
import com.example.wms.dto.TenantSwitchRequest;
import com.example.wms.dto.TenantUpdateRequest;
import com.example.wms.dto.TokenPairResponse;

import java.util.List;

// 租户管理服务
public interface TenantService {
    List<TenantResponse> listAll();

    TenantResponse getById(Long id);

    TenantResponse create(TenantCreateRequest request);

    TenantResponse updateStatus(Long id, TenantStatusUpdateRequest request);

    TenantResponse updateName(Long id, TenantUpdateRequest request);

    void delete(Long id);

    TokenPairResponse switchTenant(TenantSwitchRequest request, String authorizationHeader);
}
