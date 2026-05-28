package com.example.wms.service.impl;

import com.example.wms.audit.RequestAuditContext;
import com.example.wms.dto.EffectiveListPreferencesResponse;
import com.example.wms.dto.MyListPreferencesResponse;
import com.example.wms.dto.MyListPreferencesUpdateRequest;
import com.example.wms.entity.UserAccount;
import com.example.wms.entity.UserTableSetting;
import com.example.wms.mapper.UserTableSettingMapper;
import com.example.wms.security.AuthenticatedUser;
import com.example.wms.service.MyPreferenceService;
import com.example.wms.service.TenantSettingService;
import com.example.wms.service.UserAccountService;
import com.example.wms.tenant.TenantContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

// 当前用户偏好服务实现
@Service
public class MyPreferenceServiceImpl implements MyPreferenceService {
    private static final String PAGE_KEY = "my-list-preferences";
    private static final String PAGE_SIZE_KEY = "pageSize";
    private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final UserTableSettingMapper userTableSettingMapper;
    private final UserAccountService userAccountService;
    private final TenantSettingService tenantSettingService;
    private final ObjectMapper objectMapper;

    public MyPreferenceServiceImpl(UserTableSettingMapper userTableSettingMapper,
                                   UserAccountService userAccountService,
                                   TenantSettingService tenantSettingService,
                                   ObjectMapper objectMapper) {
        this.userTableSettingMapper = userTableSettingMapper;
        this.userAccountService = userAccountService;
        this.tenantSettingService = tenantSettingService;
        this.objectMapper = objectMapper;
    }

    @Override
    public MyListPreferencesResponse getListPreferences() {
        UserTableSetting setting = findCurrentSetting();
        if (setting == null) {
            return new MyListPreferencesResponse(null, null);
        }
        return new MyListPreferencesResponse(readPageSize(setting.getConfigJson()), setting.getUpdatedAt());
    }

    @Override
@Transactional
    public MyListPreferencesResponse updateListPreferences(MyListPreferencesUpdateRequest request) {
        Integer pageSize = TenantSettingServiceImpl.sanitizePageSize(request == null ? null : request.pageSize());
        if (pageSize == null) {
            throw new IllegalArgumentException("每页显示条数必须为 5 到 200 的整数");
        }
        Long tenantId = TenantContext.requireTenantId();
        CurrentUserRef currentUser = resolveCurrentUser();
        UserTableSetting existing = userTableSettingMapper.findOne(tenantId, currentUser.id(), PAGE_KEY);
        UserTableSetting setting = existing == null ? new UserTableSetting() : existing;
        setting.setTenantId(tenantId);
        setting.setUserId(currentUser.id());
        setting.setPageKey(PAGE_KEY);
        setting.setConfigJson(writeConfig(Map.of(PAGE_SIZE_KEY, pageSize)));
        setting.setUpdatedBy(currentUser.username());
        setting.setUpdatedAt(Instant.now());
        if (existing == null) {
            userTableSettingMapper.insert(setting);
        } else {
            userTableSettingMapper.update(setting);
        }
        return new MyListPreferencesResponse(pageSize, setting.getUpdatedAt());
    }

    @Override
    public EffectiveListPreferencesResponse getEffectiveListPreferences() {
        MyListPreferencesResponse mine = getListPreferences();
        if (mine.pageSize() != null) {
            return new EffectiveListPreferencesResponse(mine.pageSize(), "USER");
        }
        Integer tenantDefault = tenantSettingService.getConfiguredDefaultPageSize();
        if (tenantDefault != null) {
            return new EffectiveListPreferencesResponse(tenantDefault, "TENANT");
        }
        return new EffectiveListPreferencesResponse(TenantSettingServiceImpl.FALLBACK_PAGE_SIZE, "DEFAULT");
    }

    private UserTableSetting findCurrentSetting() {
        Long tenantId = TenantContext.requireTenantId();
        CurrentUserRef currentUser = resolveCurrentUser();
        return userTableSettingMapper.findOne(tenantId, currentUser.id(), PAGE_KEY);
    }

    private Integer readPageSize(String configJson) {
        if (configJson == null || configJson.isBlank()) {
            return null;
        }
        try {
            Map<String, Object> config = objectMapper.readValue(configJson, MAP_TYPE);
            Object raw = config.get(PAGE_SIZE_KEY);
            if (raw instanceof Number number) {
                return TenantSettingServiceImpl.sanitizePageSize(number.intValue());
            }
            if (raw instanceof String text) {
                return TenantSettingServiceImpl.parsePageSize(text);
            }
            return null;
        } catch (Exception ex) {
            throw new IllegalStateException("用户分页偏好解析失败", ex);
        }
    }

    private String writeConfig(Map<String, Object> config) {
        try {
            return objectMapper.writeValueAsString(config);
        } catch (Exception ex) {
            throw new IllegalStateException("用户分页偏好保存失败", ex);
        }
    }

    private CurrentUserRef resolveCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new IllegalStateException("当前用户未登录");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedUser authenticatedUser && authenticatedUser.getUserId() != null) {
            return new CurrentUserRef(authenticatedUser.getUserId(), authenticatedUser.getUsername());
        }
        Long tenantId = TenantContext.requireTenantId();
        RequestAuditContext auditContext = RequestAuditContext.get();
        Long authTenantId = auditContext == null ? tenantId : auditContext.getAuthTenantId();
        Long lookupTenantId = authTenantId == null ? tenantId : authTenantId;
        if (Objects.equals(lookupTenantId, tenantId)) {
            UserAccount currentUser = userAccountService.loadUserAccount(authentication.getName());
            return new CurrentUserRef(currentUser.getId(), currentUser.getUsername());
        }
        try {
            TenantContext.setTenantId(lookupTenantId);
            UserAccount currentUser = userAccountService.loadUserAccount(authentication.getName());
            return new CurrentUserRef(currentUser.getId(), currentUser.getUsername());
        } finally {
            TenantContext.setTenantId(tenantId);
        }
    }

    private record CurrentUserRef(Long id, String username) {
    }
}
