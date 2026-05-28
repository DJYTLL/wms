package com.example.wms.controller;

import com.example.wms.dto.ApiResponse;
import com.example.wms.dto.AuthorizationContextResponse;
import com.example.wms.service.AuthorizationContextService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class CurrentAuthorizationController {
    private final AuthorizationContextService authorizationContextService;

    public CurrentAuthorizationController(AuthorizationContextService authorizationContextService) {
        this.authorizationContextService = authorizationContextService;
    }

    @GetMapping("/authorizations")
    public ResponseEntity<ApiResponse<AuthorizationContextResponse>> getAuthorizations() {
        return ResponseEntity.ok(ApiResponse.ok(authorizationContextService.getCurrent()));
    }
}
