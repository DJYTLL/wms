package com.example.wms.controller;

import com.example.wms.dto.ApiResponse;
import com.example.wms.dto.EffectiveListPreferencesResponse;
import com.example.wms.dto.MyListPreferencesResponse;
import com.example.wms.dto.MyListPreferencesUpdateRequest;
import com.example.wms.service.MyPreferenceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 当前用户偏好接口
@RestController
@RequestMapping("/api/my/preferences")
public class MyPreferenceController {
    private final MyPreferenceService myPreferenceService;

    public MyPreferenceController(MyPreferenceService myPreferenceService) {
        this.myPreferenceService = myPreferenceService;
    }

    @GetMapping("/list")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<MyListPreferencesResponse>> getListPreferences() {
        return ResponseEntity.ok(ApiResponse.ok(myPreferenceService.getListPreferences()));
    }

    @PutMapping("/list")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<MyListPreferencesResponse>> updateListPreferences(
        @RequestBody MyListPreferencesUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(myPreferenceService.updateListPreferences(request)));
    }

    @GetMapping("/list/effective")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<EffectiveListPreferencesResponse>> getEffectiveListPreferences() {
        return ResponseEntity.ok(ApiResponse.ok(myPreferenceService.getEffectiveListPreferences()));
    }
}
