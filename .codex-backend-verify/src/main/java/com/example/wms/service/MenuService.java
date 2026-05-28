package com.example.wms.service;

import com.example.wms.dto.MenuResponse;
import com.example.wms.dto.MenuManageResponse;
import com.example.wms.dto.MenuCreateRequest;
import com.example.wms.dto.MenuUpdateRequest;
import com.example.wms.dto.TenantMenuResponse;
import com.example.wms.dto.TenantMenuUpdateRequest;

import java.util.List;

// 菜单服务
public interface MenuService {
    // 获取当前用户可见菜单
    List<MenuResponse> listVisibleMenus();

    // 获取指定租户菜单（含启用状态）
    List<TenantMenuResponse> listTenantMenus(Long tenantId);

    // 更新指定租户可见菜单
    void updateTenantMenus(Long tenantId, TenantMenuUpdateRequest request);

    // 获取全部菜单（管理用）
    List<MenuManageResponse> listAllMenus();

    // 新增菜单
    MenuManageResponse createMenu(MenuCreateRequest request);

    // 更新菜单
    MenuManageResponse updateMenu(Long id, MenuUpdateRequest request);

    // 删除菜单
    void deleteMenu(Long id);
}
