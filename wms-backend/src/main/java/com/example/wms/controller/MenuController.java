package com.example.wms.controller;

import com.example.wms.dto.ApiResponse;
import com.example.wms.dto.MenuCreateRequest;
import com.example.wms.dto.MenuManageResponse;
import com.example.wms.dto.MenuResponse;
import com.example.wms.dto.MenuUpdateRequest;
import com.example.wms.service.MenuService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

// 菜单查询接口
@RestController
@RequestMapping("/api/menus")
public class MenuController {
    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    // 获取当前用户可见菜单
    @GetMapping
    public ResponseEntity<ApiResponse<List<MenuResponse>>> listVisibleMenus() {
        return ResponseEntity.ok(ApiResponse.ok(menuService.listVisibleMenus()));
    }

    // 获取全部菜单（超级管理员）
    @GetMapping("/all")
    @PreAuthorize("hasRole('super_admin')")
    public ResponseEntity<ApiResponse<List<MenuManageResponse>>> listAllMenus() {
        return ResponseEntity.ok(ApiResponse.ok(menuService.listAllMenus()));
    }

    // 新增菜单（超级管理员）
    @PostMapping
    @PreAuthorize("hasRole('super_admin')")
    public ResponseEntity<ApiResponse<MenuManageResponse>> create(@Valid @RequestBody MenuCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(menuService.createMenu(request)));
    }

    // 更新菜单（超级管理员）
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('super_admin')")
    public ResponseEntity<ApiResponse<MenuManageResponse>> update(@PathVariable Long id,
                                                                  @Valid @RequestBody MenuUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(menuService.updateMenu(id, request)));
    }

    // 删除菜单（超级管理员）
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('super_admin')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        menuService.deleteMenu(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
