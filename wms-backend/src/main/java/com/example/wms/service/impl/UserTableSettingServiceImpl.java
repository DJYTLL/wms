package com.example.wms.service.impl;

import com.example.wms.audit.RequestAuditContext;
import com.example.wms.aop.AuditLog;
import com.example.wms.dto.UserTableSettingRequest;
import com.example.wms.dto.UserTableSettingResponse;
import com.example.wms.entity.UserAccount;
import com.example.wms.entity.UserTableSetting;
import com.example.wms.mapper.UserTableSettingMapper;
import com.example.wms.service.UserAccountService;
import com.example.wms.service.UserTableSettingService;
import com.example.wms.tenant.TenantContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

// 用户表格配置服务实现
@Service
public class UserTableSettingServiceImpl implements UserTableSettingService {
    private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final UserTableSettingMapper userTableSettingMapper;
    private final UserAccountService userAccountService;
    private final ObjectMapper objectMapper;

    public UserTableSettingServiceImpl(UserTableSettingMapper userTableSettingMapper,
                                       UserAccountService userAccountService,
                                       ObjectMapper objectMapper) {
        this.userTableSettingMapper = userTableSettingMapper;
        this.userAccountService = userAccountService;
        this.objectMapper = objectMapper;
    }

    @Override
    public UserTableSettingResponse getByPageKey(String pageKey) {
        String normalizedPageKey = normalizePageKey(pageKey);
        Long tenantId = TenantContext.requireTenantId();
        UserAccount currentUser = resolveCurrentUser();
        UserTableSetting setting = userTableSettingMapper.findOne(tenantId, currentUser.getId(), normalizedPageKey);
        if (setting == null) {
            return new UserTableSettingResponse(normalizedPageKey, Map.of(), null, null);
        }
        return toResponse(setting);
    }

    @Override
    @AuditLog(action = "USER_TABLE_SETTING_UPDATE", entityType = "user_table_setting", entityId = "{arg0}")
    public UserTableSettingResponse update(String pageKey, UserTableSettingRequest request) {
        String normalizedPageKey = normalizePageKey(pageKey);
        Map<String, Object> sanitizedConfig = sanitizeConfig(request);
        Long tenantId = TenantContext.requireTenantId();
        UserAccount currentUser = resolveCurrentUser();
        UserTableSetting existing = userTableSettingMapper.findOne(tenantId, currentUser.getId(), normalizedPageKey);
        UserTableSetting setting = existing == null ? new UserTableSetting() : existing;
        setting.setTenantId(tenantId);
        setting.setUserId(currentUser.getId());
        setting.setPageKey(normalizedPageKey);
        setting.setConfigJson(writeConfig(sanitizedConfig));
        setting.setUpdatedBy(currentUser.getUsername());
        setting.setUpdatedAt(Instant.now());
        if (existing == null) {
            userTableSettingMapper.insert(setting);
        } else {
            userTableSettingMapper.update(setting);
        }
        return toResponse(setting);
    }

    private UserTableSettingResponse toResponse(UserTableSetting setting) {
        return new UserTableSettingResponse(
            setting.getPageKey(),
            readConfig(setting.getConfigJson()),
            setting.getUpdatedBy(),
            setting.getUpdatedAt()
        );
    }

    private String normalizePageKey(String pageKey) {
        if (pageKey == null || pageKey.isBlank()) {
            throw new IllegalArgumentException("页面标识不能为空");
        }
        return pageKey.trim();
    }

    private Map<String, Object> sanitizeConfig(UserTableSettingRequest request) {
        if (request == null || request.config() == null) {
            return Map.of();
        }
        return new LinkedHashMap<>(request.config());
    }

    private Map<String, Object> readConfig(String configJson) {
        if (configJson == null || configJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(configJson, MAP_TYPE);
        } catch (Exception ex) {
            throw new IllegalStateException("用户表格配置解析失败", ex);
        }
    }

    private String writeConfig(Map<String, Object> config) {
        try {
            return objectMapper.writeValueAsString(config);
        } catch (Exception ex) {
            throw new IllegalStateException("用户表格配置保存失败", ex);
        }
    }

    private UserAccount resolveCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new IllegalStateException("当前用户未登录");
        }
        Long tenantId = TenantContext.requireTenantId();
        RequestAuditContext auditContext = RequestAuditContext.get();
        Long authTenantId = auditContext == null ? tenantId : auditContext.getAuthTenantId();
        Long lookupTenantId = authTenantId == null ? tenantId : authTenantId;
        if (Objects.equals(lookupTenantId, tenantId)) {
            return userAccountService.loadUserAccount(authentication.getName());
        }
        try {
            TenantContext.setTenantId(lookupTenantId);
            return userAccountService.loadUserAccount(authentication.getName());
        } finally {
            TenantContext.setTenantId(tenantId);
        }
    }
}
