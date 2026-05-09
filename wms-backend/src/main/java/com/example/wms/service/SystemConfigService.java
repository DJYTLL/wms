package com.example.wms.service;

import com.example.wms.dto.SystemConfigRequest;
import com.example.wms.dto.SystemConfigResponse;

import java.util.List;

// 系统配置服务
public interface SystemConfigService {
    List<SystemConfigResponse> listAll();

    List<SystemConfigResponse> listPublic();

    SystemConfigResponse getByKey(String key);

    SystemConfigResponse create(String key, SystemConfigRequest request);

    SystemConfigResponse update(String key, SystemConfigRequest request);
}
