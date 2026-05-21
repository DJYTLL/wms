package com.example.wms.service;

import com.example.wms.dto.PermissionCreateRequest;
import com.example.wms.dto.PermissionDiagnosticResponse;
import com.example.wms.dto.PermissionUpdateRequest;
import com.example.wms.dto.PageResponse;
import com.example.wms.entity.Permission;

import java.util.List;

// 权限服务接口
public interface PermissionService {
    // 查询权限列表
    List<Permission> listAll();

    // 分页查询权限列表
    PageResponse<Permission> page(long page, long size, String keyword, Boolean enabled);

    // 按 ID 查询
    Permission getById(Long id);

    // 新增权限
    Permission create(PermissionCreateRequest request);

    // 更新权限
    Permission update(Long id, PermissionUpdateRequest request);

    // 删除权限
    void delete(Long id);

    // 查询列权限列表
    List<Permission> listColumnPermissions();

    // 查询权限诊断信息
    List<PermissionDiagnosticResponse> listDiagnostics();
}
