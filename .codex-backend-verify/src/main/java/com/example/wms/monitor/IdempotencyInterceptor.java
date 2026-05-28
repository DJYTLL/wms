package com.example.wms.monitor;

import com.example.wms.service.IdempotencyService;
import com.example.wms.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

// 幂等拦截器：防重复提交
@Component
public class IdempotencyInterceptor implements HandlerInterceptor {
    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";
    private final IdempotencyService idempotencyService;

    public IdempotencyInterceptor(IdempotencyService idempotencyService) {
        this.idempotencyService = idempotencyService;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        String method = request.getMethod();
        if ("GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method) || "OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }
        String path = request.getRequestURI();
        if (path != null && (path.startsWith("/api/login")
            || path.startsWith("/api/refresh")
            || path.startsWith("/api/logout")
            || path.startsWith("/api/system-configs"))) {
            return true;
        }
        String key = request.getHeader(IDEMPOTENCY_HEADER);
        if (key == null || key.isBlank()) {
            return true;
        }
        Long tenantId = TenantContext.getTenantId();
        String username = resolveUsername();
        idempotencyService.checkAndStore(key, method, path, tenantId, username);
        return true;
    }

    private String resolveUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return null;
        }
        return authentication.getName();
    }
}
