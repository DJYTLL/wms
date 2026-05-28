package com.example.wms.service.impl;

import com.example.wms.dto.TenantColumnSettingRequest;
import com.example.wms.dto.TenantColumnSettingResponse;
import com.example.wms.entity.TenantColumnSetting;
import com.example.wms.entity.Tenant;
import com.example.wms.entity.Permission;
import com.example.wms.mapper.PermissionMapper;
import com.example.wms.mapper.TenantColumnSettingMapper;
import com.example.wms.mapper.TenantMapper;
import com.example.wms.service.TenantColumnSettingService;
import com.example.wms.tenant.TenantContext;
import com.example.wms.aop.AuditLog;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

// 租户列配置服务实现
@Service
public class TenantColumnSettingServiceImpl implements TenantColumnSettingService {
    private final TenantColumnSettingMapper tenantColumnSettingMapper;
    private final TenantMapper tenantMapper;
    private final PermissionMapper permissionMapper;

    public TenantColumnSettingServiceImpl(TenantColumnSettingMapper tenantColumnSettingMapper,
                                          TenantMapper tenantMapper,
                                          PermissionMapper permissionMapper) {
        this.tenantColumnSettingMapper = tenantColumnSettingMapper;
        this.tenantMapper = tenantMapper;
        this.permissionMapper = permissionMapper;
    }

    @Override
    public TenantColumnSettingResponse getByPageKey(String pageKey) {
        TenantColumnSetting setting = tenantColumnSettingMapper.findOne(TenantContext.requireTenantId(), pageKey);
        if (setting == null) {
            return new TenantColumnSettingResponse(pageKey, List.of(), null, null);
        }
        return toResponse(setting);
    }

    @Override
    @AuditLog(action = "TENANT_COLUMN_UPDATE", entityType = "tenant", entityId = "{arg0}", detail = "visibleColumns={arg1.visibleColumns}")
@Transactional
    public TenantColumnSettingResponse update(String pageKey, TenantColumnSettingRequest request) {
        if (request == null || request.visibleColumns() == null) {
            throw new IllegalArgumentException("可见列不能为空");
        }
        Long tenantId = TenantContext.requireTenantId();
        List<String> sanitizedColumns = sanitizeColumns(request.visibleColumns());
        validateColumns(pageKey, sanitizedColumns);
        TenantColumnSetting existing = tenantColumnSettingMapper.findOne(tenantId, pageKey);
        TenantColumnSetting setting = existing == null ? new TenantColumnSetting() : existing;
        setting.setTenantId(tenantId);
        setting.setPageKey(pageKey);
        setting.setVisibleColumns(String.join(",", sanitizedColumns));
        setting.setUpdatedBy(resolveUsername());
        setting.setUpdatedAt(Instant.now());
        if (existing == null) {
            tenantColumnSettingMapper.insert(setting);
        } else {
            tenantColumnSettingMapper.update(setting);
        }
        return toResponse(setting);
    }

    @Override
    public TenantColumnSettingResponse getByTenantAndPageKey(Long tenantId, String pageKey) {
        validateTenant(tenantId);
        TenantColumnSetting setting = tenantColumnSettingMapper.findOne(tenantId, pageKey);
        if (setting == null) {
            return new TenantColumnSettingResponse(pageKey, List.of(), null, null);
        }
        return toResponse(setting);
    }

    @Override
    @AuditLog(action = "TENANT_COLUMN_UPDATE", entityType = "tenant", entityId = "{arg0}", detail = "visibleColumns={arg2.visibleColumns}")
@Transactional
    public TenantColumnSettingResponse updateForTenant(Long tenantId, String pageKey, TenantColumnSettingRequest request) {
        if (request == null || request.visibleColumns() == null) {
            throw new IllegalArgumentException("可见列不能为空");
        }
        validateTenant(tenantId);
        List<String> sanitizedColumns = sanitizeColumns(request.visibleColumns());
        validateColumns(pageKey, sanitizedColumns);
        TenantColumnSetting existing = tenantColumnSettingMapper.findOne(tenantId, pageKey);
        TenantColumnSetting setting = existing == null ? new TenantColumnSetting() : existing;
        setting.setTenantId(tenantId);
        setting.setPageKey(pageKey);
        setting.setVisibleColumns(String.join(",", sanitizedColumns));
        setting.setUpdatedBy(resolveUsername());
        setting.setUpdatedAt(Instant.now());
        if (existing == null) {
            tenantColumnSettingMapper.insert(setting);
        } else {
            tenantColumnSettingMapper.update(setting);
        }
        return toResponse(setting);
    }

    private TenantColumnSettingResponse toResponse(TenantColumnSetting setting) {
        List<String> columns = setting.getVisibleColumns() == null || setting.getVisibleColumns().isBlank()
            ? List.of()
            : java.util.Arrays.asList(setting.getVisibleColumns().split(","));
        return new TenantColumnSettingResponse(
            setting.getPageKey(),
            columns,
            setting.getUpdatedBy(),
            setting.getUpdatedAt()
        );
    }

    private String resolveUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return null;
        }
        return authentication.getName();
    }

    private void validateTenant(Long tenantId) {
        Tenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null || tenant.getDeletedAt() != null) {
            throw new IllegalArgumentException("租户不存在");
        }
    }

    // 列强校验：仅允许配置当前页面已注册的列权限
    private void validateColumns(String pageKey, List<String> columns) {
        String prefix = "column:" + pageKey + ":";
        List<Permission> permissions = permissionMapper.findByCodePrefix(prefix);
        if (permissions.isEmpty()) {
            throw new IllegalArgumentException("页面不存在或未注册列权限");
        }
        java.util.Set<String> allowed = permissions.stream()
            .map(Permission::getCode)
            .filter(code -> code != null && code.startsWith(prefix))
            .map(code -> code.substring(prefix.length()))
            .collect(java.util.stream.Collectors.toSet());
        boolean allAllowed = columns.stream().allMatch(allowed::contains);
        if (!allAllowed) {
            throw new IllegalArgumentException("包含未注册的列标识");
        }
    }

    private List<String> sanitizeColumns(List<String> columns) {
        return columns.stream()
            .filter(item -> item != null && !item.isBlank())
            .map(String::trim)
            .distinct()
            .toList();
    }
}
