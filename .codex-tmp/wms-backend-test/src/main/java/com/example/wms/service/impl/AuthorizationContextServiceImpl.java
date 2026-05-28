package com.example.wms.service.impl;

import com.example.wms.dto.AuthPayload;
import com.example.wms.dto.AuthorizationContextResponse;
import com.example.wms.security.AuthenticatedUser;
import com.example.wms.service.AuthorizationContextService;
import com.example.wms.service.UserAccountService;
import com.example.wms.tenant.TenantContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationContextServiceImpl implements AuthorizationContextService {
    private final UserAccountService userAccountService;

    public AuthorizationContextServiceImpl(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    @Override
    public AuthorizationContextResponse getCurrent() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser principal)) {
            throw new IllegalStateException("当前登录态无效");
        }
        AuthPayload principalPayload = principal.getAuthPayload();
        Long audienceTenantId = principalPayload == null ? null : principalPayload.tenantId();
        Long authTenantId = principalPayload == null ? null : principalPayload.userTenantId();
        Long originalTenantId = TenantContext.getTenantId();
        boolean tenantOverridden = false;
        try {
            if (authTenantId != null && !authTenantId.equals(originalTenantId)) {
                TenantContext.setTenantId(authTenantId);
                tenantOverridden = true;
            }
            AuthPayload payload = userAccountService.loadAuthPayload(principal.getUsername(), audienceTenantId);
            return new AuthorizationContextResponse(
                payload.user(),
                payload.permissions(),
                payload.authVersion(),
                payload.tenantId(),
                payload.tenantCode(),
                payload.userTenantId(),
                payload.userTenantCode()
            );
        } finally {
            if (tenantOverridden) {
                if (originalTenantId != null) {
                    TenantContext.setTenantId(originalTenantId);
                } else {
                    TenantContext.clear();
                }
            }
        }
    }
}
