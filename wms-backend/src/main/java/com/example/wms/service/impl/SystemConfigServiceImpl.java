package com.example.wms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.wms.dto.SystemConfigRequest;
import com.example.wms.dto.SystemConfigResponse;
import com.example.wms.entity.SystemConfig;
import com.example.wms.exception.NotFoundException;
import com.example.wms.mapper.SystemConfigMapper;
import com.example.wms.service.SystemConfigService;
import com.example.wms.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

// 系统配置服务实现
@Service
public class SystemConfigServiceImpl implements SystemConfigService {
    private final SystemConfigMapper systemConfigMapper;

    public SystemConfigServiceImpl(SystemConfigMapper systemConfigMapper) {
        this.systemConfigMapper = systemConfigMapper;
    }

    @Override
    public List<SystemConfigResponse> listAll() {
        return systemConfigMapper.findAll(TenantContext.requireTenantId()).stream().map(this::toResponse).toList();
    }

    @Override
    public List<SystemConfigResponse> listPublic() {
        return systemConfigMapper.findPublic(TenantContext.requireTenantId()).stream().map(this::toResponse).toList();
    }

    @Override
    public SystemConfigResponse getByKey(String key) {
        SystemConfig config = loadByKey(key);
        return toResponse(config);
    }

    @Override
    public SystemConfigResponse create(String key, SystemConfigRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("配置键不能为空");
        }
        SystemConfig existing = systemConfigMapper.findByKey(tenantId, key);
        if (existing != null) {
            throw new IllegalArgumentException("配置键已存在");
        }
        SystemConfig config = new SystemConfig();
        config.setTenantId(tenantId);
        config.setConfigKey(key.trim());
        applyRequest(config, request);
        systemConfigMapper.insert(config);
        return toResponse(config);
    }

    @Override
    public SystemConfigResponse update(String key, SystemConfigRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        SystemConfig config = loadByKey(tenantId, key);
        applyRequest(config, request);
        config.setUpdatedAt(Instant.now());
        systemConfigMapper.update(
            config,
            new QueryWrapper<SystemConfig>().eq("tenant_id", tenantId).eq("config_key", key)
        );
        return toResponse(config);
    }

    private SystemConfig loadByKey(String key) {
        return loadByKey(TenantContext.requireTenantId(), key);
    }

    private SystemConfig loadByKey(Long tenantId, String key) {
        SystemConfig config = systemConfigMapper.findByKey(tenantId, key);
        if (config == null) {
            throw new NotFoundException("配置不存在");
        }
        return config;
    }

    private void applyRequest(SystemConfig config, SystemConfigRequest request) {
        if (request == null) {
            return;
        }
        if (request.value() != null) {
            config.setConfigValue(request.value());
        }
        if (request.valueType() != null) {
            config.setValueType(request.valueType());
        }
        if (request.description() != null) {
            config.setDescription(request.description());
        }
        if (request.isPublic() != null) {
            config.setPublic(request.isPublic());
        }
    }

    private SystemConfigResponse toResponse(SystemConfig config) {
        return new SystemConfigResponse(
            config.getId(),
            config.getConfigKey(),
            config.getConfigValue(),
            config.getValueType(),
            config.getDescription(),
            config.isPublic(),
            config.getCreatedAt(),
            config.getUpdatedAt()
        );
    }
}
