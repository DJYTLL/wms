package com.example.wms.service.impl;

import com.example.wms.dto.AuthPayload;
import com.example.wms.dto.TokenPairResponse;
import com.example.wms.entity.RefreshToken;
import com.example.wms.entity.UserAccount;
import com.example.wms.mapper.RefreshTokenMapper;
import com.example.wms.mapper.TenantMapper;
import com.example.wms.mapper.UserAccountMapper;
import com.example.wms.security.JwtTokenService;
import com.example.wms.service.RefreshTokenService;
import com.example.wms.service.UserAccountService;
import com.example.wms.tenant.TenantContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

// 刷新令牌服务实现
@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {
    private final RefreshTokenMapper refreshTokenMapper;
    private final UserAccountMapper userAccountMapper;
    private final UserAccountService userAccountService;
    private final JwtTokenService jwtTokenService;
    private final TenantMapper tenantMapper;
    private final long refreshExpirationDays;

    public RefreshTokenServiceImpl(RefreshTokenMapper refreshTokenMapper,
                                   UserAccountMapper userAccountMapper,
                                   UserAccountService userAccountService,
                                   JwtTokenService jwtTokenService,
                                   TenantMapper tenantMapper,
                                   @Value("${jwt.refresh-expiration-days:7}") long refreshExpirationDays) {
        this.refreshTokenMapper = refreshTokenMapper;
        this.userAccountMapper = userAccountMapper;
        this.userAccountService = userAccountService;
        this.jwtTokenService = jwtTokenService;
        this.tenantMapper = tenantMapper;
        this.refreshExpirationDays = refreshExpirationDays;
    }

    @Override
    public TokenPairResponse issueTokens(UserAccount user, AuthPayload payload) {
        String accessToken = jwtTokenService.generateToken(payload);
        Long audienceTenantId = payload.tenantId() == null ? user.getTenantId() : payload.tenantId();
        String refreshToken = createRefreshToken(user.getId(), user.getTenantId(), audienceTenantId);
        return new TokenPairResponse(accessToken, refreshToken);
    }

    @Override
    public TokenPairResponse refresh(String refreshToken) {
        String tokenHash = hashToken(refreshToken);
        RefreshToken record = refreshTokenMapper.findByTokenHash(tokenHash);
        if (record == null || record.getRevokedAt() != null) {
            throw new IllegalArgumentException("刷新令牌无效");
        }
        if (record.getExpiresAt() != null && record.getExpiresAt().isBefore(Instant.now())) {
            refreshTokenMapper.revokeById(record.getId());
            throw new IllegalArgumentException("刷新令牌已过期");
        }

        try {
            TenantContext.setTenantId(record.getTenantId());
            UserAccount user = userAccountMapper.findActiveById(record.getTenantId(), record.getUserId());
            if (user == null) {
                refreshTokenMapper.revokeById(record.getId());
                throw new IllegalArgumentException("用户不存在");
            }

            // 轮换刷新令牌
            refreshTokenMapper.revokeById(record.getId());
            AuthPayload payload = userAccountService.loadAuthPayload(user.getUsername());
            Long audienceTenantId = record.getAudienceTenantId();
            AuthPayload audiencePayload = resolveAudiencePayload(payload, audienceTenantId);
            String accessToken = jwtTokenService.generateToken(audiencePayload);
            String newRefreshToken = createRefreshToken(user.getId(), user.getTenantId(), audiencePayload.tenantId());
            return new TokenPairResponse(accessToken, newRefreshToken);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    public void revoke(String refreshToken) {
        String tokenHash = hashToken(refreshToken);
        RefreshToken record = refreshTokenMapper.findByTokenHash(tokenHash);
        if (record != null && record.getRevokedAt() == null) {
            refreshTokenMapper.revokeById(record.getId());
        }
    }

    @Override
    public void revokeByUserId(Long userId) {
        Long tenantId = TenantContext.requireTenantId();
        refreshTokenMapper.revokeByUserId(tenantId, userId);
    }

    // 创建并持久化刷新令牌
    private String createRefreshToken(Long userId, Long tenantId, Long audienceTenantId) {
        String raw = UUID.randomUUID().toString().replace("-", "");
        RefreshToken token = new RefreshToken();
        token.setTenantId(tenantId);
        token.setUserId(userId);
        token.setAudienceTenantId(audienceTenantId);
        token.setTokenHash(hashToken(raw));
        token.setExpiresAt(Instant.now().plus(refreshExpirationDays, ChronoUnit.DAYS));
        refreshTokenMapper.insert(token);
        return raw;
    }

    private AuthPayload resolveAudiencePayload(AuthPayload payload, Long audienceTenantId) {
        if (audienceTenantId == null || audienceTenantId.equals(payload.tenantId())) {
            return payload;
        }
        if (!hasRole(payload, "super_admin")) {
            throw new IllegalArgumentException("当前账号已无跨租户权限");
        }
        String tenantCode = null;
        var tenant = tenantMapper.selectById(audienceTenantId);
        if (tenant != null) {
            tenantCode = tenant.getCode();
        }
        return new AuthPayload(payload.user(),
            payload.permissions(),
            payload.authVersion(),
            audienceTenantId,
            tenantCode,
            payload.userTenantId(),
            payload.userTenantCode());
    }

    private boolean hasRole(AuthPayload payload, String roleCode) {
        if (payload == null || payload.user() == null || payload.user().roles() == null) {
            return false;
        }
        return payload.user().roles().stream()
            .filter(code -> code != null && !code.isBlank())
            .map(code -> code.trim().toLowerCase(Locale.ROOT))
            .anyMatch(roleCode.toLowerCase(Locale.ROOT)::equals);
    }

    // 对令牌进行哈希
    private String hashToken(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception ex) {
            throw new IllegalStateException("无法生成令牌哈希", ex);
        }
    }
}
