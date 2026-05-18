package com.example.wms.controller;

import com.example.wms.dto.ApiResponse;
import com.example.wms.dto.UserTableSettingRequest;
import com.example.wms.dto.UserTableSettingResponse;
import com.example.wms.service.UserTableSettingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 当前登录用户的表格个性化配置接口
@RestController
@RequestMapping("/api/user-table-settings")
public class UserTableSettingController {
    private final UserTableSettingService userTableSettingService;

    public UserTableSettingController(UserTableSettingService userTableSettingService) {
        this.userTableSettingService = userTableSettingService;
    }

    @GetMapping("/{pageKey}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserTableSettingResponse>> get(@PathVariable String pageKey) {
        return ResponseEntity.ok(ApiResponse.ok(userTableSettingService.getByPageKey(pageKey)));
    }

    @PutMapping("/{pageKey}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserTableSettingResponse>> update(@PathVariable String pageKey,
                                                                       @RequestBody UserTableSettingRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(userTableSettingService.update(pageKey, request)));
    }
}
