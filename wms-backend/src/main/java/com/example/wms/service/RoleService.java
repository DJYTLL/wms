package com.example.wms.service;

import com.example.wms.dto.RoleCreateRequest;
import com.example.wms.dto.RoleUpdateRequest;
import com.example.wms.dto.PageResponse;
import com.example.wms.entity.Role;

import java.util.List;

// 角色服务接口
public interface RoleService {
    // 查询角色列表
    List<Role> listAll();

    // 分页查询角色列表
    PageResponse<Role> page(long page, long size, String keyword, Boolean enabled);

    // 按 ID 查询
    Role getById(Long id);

    // 新增角色
    Role create(RoleCreateRequest request);

    // 更新角色
    Role update(Long id, RoleUpdateRequest request);

    // 删除角色
    void delete(Long id);
}
