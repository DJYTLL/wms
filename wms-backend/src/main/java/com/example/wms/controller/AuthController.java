package com.example.wms.controller;

import com.example.wms.dto.ApiResponse;
import com.example.wms.dto.AuthPayload;
import com.example.wms.dto.RefreshTokenRequest;
import com.example.wms.dto.TokenPairResponse;
import com.example.wms.entity.Tenant;
import com.example.wms.entity.UserAccount;
import com.example.wms.mapper.TenantMapper;
import com.example.wms.mapper.UserAccountMapper;
import com.example.wms.service.RefreshTokenService;
import com.example.wms.service.UserAccountService;
import com.example.wms.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 认证相关接口
@RestController
@RequestMapping("/api")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final UserAccountService userAccountService;
    private final RefreshTokenService refreshTokenService;
    private final TenantMapper tenantMapper;
    private final UserAccountMapper userAccountMapper;

    public AuthController(AuthenticationManager authenticationManager,
                          UserAccountService userAccountService,
                          RefreshTokenService refreshTokenService,
                          TenantMapper tenantMapper,
                          UserAccountMapper userAccountMapper) {
        this.authenticationManager = authenticationManager;
        this.userAccountService = userAccountService;
        this.refreshTokenService = refreshTokenService;
        this.tenantMapper = tenantMapper;
        this.userAccountMapper = userAccountMapper;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenPairResponse>> login(@Valid @RequestBody LoginRequest request,
                                                                HttpServletRequest httpRequest,
                                                                HttpServletResponse httpResponse) {
        Tenant tenant = tenantMapper.findByCode(request.tenantCode());
        if (tenant == null || tenant.getDeletedAt() != null || !tenant.isEnabled()) {
            return ResponseEntity.status(401).body(ApiResponse.error(401, "租户不存在或已停用"));
        }
        try {
            TenantContext.setTenantId(tenant.getId());
            UserAccount user = userAccountMapper.findActiveByUsername(tenant.getId(), request.username());
            if (user == null) {
                return ResponseEntity.status(401).body(ApiResponse.error(401, "用户名或密码错误"));
            }
            // 校验用户名密码
            Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );
            if (!auth.isAuthenticated()) {
                return ResponseEntity.status(401).body(ApiResponse.error(401, "用户名或密码错误"));
            }
            // 登录成功后更新最近登录时间
            userAccountMapper.updateLastLoginAt(tenant.getId(), user.getId());
            // 生成 JWT 并返回
            AuthPayload payload = userAccountService.loadAuthPayload(request.username());
            TokenPairResponse tokens = refreshTokenService.issueTokens(
                userAccountService.loadUserAccount(request.username()),
                payload
            );
            writeRefreshTokenCookie(httpResponse, tokens.refreshToken(), httpRequest.isSecure());
            return ResponseEntity.ok(new ApiResponse<>(200, "登录成功", toClientTokenPair(tokens)));
        } finally {
            TenantContext.clear();
        }
    }

    // 刷新令牌
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenPairResponse>> refresh(@RequestBody(required = false) RefreshTokenRequest request,
                                                                  HttpServletRequest httpRequest,
                                                                  HttpServletResponse httpResponse) {
        String refreshToken = resolveRefreshToken(request, httpRequest);
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(401).body(ApiResponse.error(401, "刷新令牌缺失"));
        }
        TokenPairResponse tokens = refreshTokenService.refresh(refreshToken);
        writeRefreshTokenCookie(httpResponse, tokens.refreshToken(), httpRequest.isSecure());
        return ResponseEntity.ok(ApiResponse.ok(toClientTokenPair(tokens)));
    }

    // 登出并撤销刷新令牌
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestBody(required = false) RefreshTokenRequest request,
                                                    HttpServletRequest httpRequest,
                                                    HttpServletResponse httpResponse) {
        String refreshToken = resolveRefreshToken(request, httpRequest);
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenService.revoke(refreshToken);
        }
        clearRefreshTokenCookie(httpResponse, httpRequest.isSecure());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // 登录请求体
    public record LoginRequest(@NotBlank String username,
                               @NotBlank String tenantCode,
                               @NotBlank String password) {
    }

    private String resolveRefreshToken(RefreshTokenRequest request, HttpServletRequest httpRequest) {
        if (request != null && request.refreshToken() != null && !request.refreshToken().isBlank()) {
            return request.refreshToken();
        }
        if (httpRequest.getCookies() == null) {
            return null;
        }
        for (var cookie : httpRequest.getCookies()) {
            if ("refreshToken".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private void writeRefreshTokenCookie(HttpServletResponse response, String refreshToken, boolean secure) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
            .httpOnly(true)
            .secure(secure)
            .path("/api")
            .sameSite("Strict")
            .maxAge(java.time.Duration.ofDays(7))
            .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshTokenCookie(HttpServletResponse response, boolean secure) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
            .httpOnly(true)
            .secure(secure)
            .path("/api")
            .sameSite("Strict")
            .maxAge(0)
            .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private TokenPairResponse toClientTokenPair(TokenPairResponse tokens) {
        return new TokenPairResponse(tokens.token(), null, tokens.authPayload());
    }

}

