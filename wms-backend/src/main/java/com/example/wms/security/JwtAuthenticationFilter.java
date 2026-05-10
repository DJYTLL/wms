package com.example.wms.security;

import com.example.wms.audit.RequestAuditContext;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import com.example.wms.service.UserAccountService;
import com.example.wms.tenant.TenantContext;

import java.io.IOException;

// 每次请求解析 JWT 并写入安全上下文
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenService jwtTokenService;
    private final UserAccountService userAccountService;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService, UserAccountService userAccountService) {
        this.jwtTokenService = jwtTokenService;
        this.userAccountService = userAccountService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        boolean tenantBound = false;
        boolean authTenantBound = false;
        Long targetTenantId = null;
        try {
            // 从 Authorization 头提取 Bearer Token
            String header = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
                String token = header.substring(7);
                try {
                    // 解析 Token 并构建认证信息
                    Claims claims = jwtTokenService.parseToken(token);
                    String username = claims.getSubject();
                    Number tenantValue = claims.get("tid", Number.class);
                    if (tenantValue != null) {
                        targetTenantId = tenantValue.longValue();
                        TenantContext.setTenantId(targetTenantId);
                        tenantBound = true;
                    }
                    Number userTenantValue = claims.get("utid", Number.class);
                    Long authTenantId = userTenantValue == null
                        ? targetTenantId
                        : Long.valueOf(userTenantValue.longValue());
                    RequestAuditContext auditContext = RequestAuditContext.get();
                    if (auditContext != null) {
                        auditContext.setAuthTenantId(authTenantId);
                        auditContext.setAuthTenantCode(claims.get("utcode", String.class));
                        auditContext.setCrossTenant(authTenantId != null
                            && targetTenantId != null
                            && !authTenantId.equals(targetTenantId));
                    }
                    if (StringUtils.hasText(username) && SecurityContextHolder.getContext().getAuthentication() == null) {
                        if (authTenantId != null && (targetTenantId == null || !authTenantId.equals(targetTenantId))) {
                            TenantContext.setTenantId(authTenantId);
                            authTenantBound = true;
                        }
                        // 校验权限版本，变更后强制重新认证
                        Number tokenVersionValue = claims.get("av", Number.class);
                        long tokenVersion = tokenVersionValue == null ? 0L : tokenVersionValue.longValue();
                        long currentVersion = userAccountService.loadAuthVersion(username);
                        if (tokenVersion != currentVersion) {
                            if (authTenantBound) {
                                if (targetTenantId != null) {
                                    TenantContext.setTenantId(targetTenantId);
                                } else {
                                    TenantContext.clear();
                                }
                            }
                            filterChain.doFilter(request, response);
                            return;
                        }

                        UserDetails userDetails = userAccountService.loadUserByUsername(username);
                        if (authTenantId != null
                            && targetTenantId != null
                            && !authTenantId.equals(targetTenantId)
                            && userDetails.getAuthorities().stream().noneMatch(authority ->
                                authority != null && "ROLE_super_admin".equalsIgnoreCase(authority.getAuthority()))) {
                            if (authTenantBound) {
                                if (targetTenantId != null) {
                                    TenantContext.setTenantId(targetTenantId);
                                } else {
                                    TenantContext.clear();
                                }
                            }
                            filterChain.doFilter(request, response);
                            return;
                        }
                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                        );
                        // 绑定请求细节
                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                        if (authTenantBound) {
                            if (targetTenantId != null) {
                                TenantContext.setTenantId(targetTenantId);
                            } else {
                                TenantContext.clear();
                            }
                            authTenantBound = false;
                        }
                    }
                } catch (Exception ex) {
                    // Token 无效，继续链路以便受保护接口返回 401
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            if (tenantBound || authTenantBound) {
                TenantContext.clear();
            }
        }
    }
}
