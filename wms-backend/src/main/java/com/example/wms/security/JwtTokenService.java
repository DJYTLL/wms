package com.example.wms.security;

import com.example.wms.dto.AuthPayload;
import com.example.wms.dto.UserClaim;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

// JWT 生成与解析服务
@Service
public class JwtTokenService {
    private final SecretKey secretKey;
    private final String issuer;
    private final long expirationMinutes;

    public JwtTokenService(@Value("${jwt.secret}") String secret,
                           @Value("${jwt.issuer}") String issuer,
                           @Value("${jwt.expiration-minutes}") long expirationMinutes) {
        // 使用配置的密钥与参数初始化
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
        this.expirationMinutes = expirationMinutes;
    }

    public String generateToken(AuthPayload payload) {
        // 生成包含用户对象、权限列表与过期时间的 JWT
        Instant now = Instant.now();
        Map<String, Object> userClaim = buildUserClaim(payload.user());
        Long userTenantId = payload.userTenantId() == null ? payload.tenantId() : payload.userTenantId();
        return Jwts.builder()
            .setSubject(payload.user().username())
            .setIssuer(issuer)
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(now.plus(expirationMinutes, ChronoUnit.MINUTES)))
            .claim("user", userClaim)
            .claim("uid", userClaim.get("id"))
            .claim("perms", payload.permissions())
            .claim("av", payload.authVersion())
            .claim("tid", payload.tenantId())
            .claim("tcode", payload.tenantCode())
            .claim("utid", userTenantId)
            .claim("utcode", payload.userTenantCode())
            .signWith(secretKey, SignatureAlgorithm.HS256)
            .compact();
    }

    public Claims parseToken(String token) {
        // 解析并验证 JWT
        return Jwts.parserBuilder()
            .setSigningKey(secretKey)
            .build()
            .parseClaimsJws(token)
            .getBody();
    }

    // 组装 user 载荷对象
    private Map<String, Object> buildUserClaim(UserClaim user) {
        Map<String, Object> claim = new HashMap<>();
        claim.put("id", user.id());
        claim.put("username", user.username());
        claim.put("role", user.role());
        claim.put("avatar", user.avatar());
        claim.put("roles", user.roles());
        return claim;
    }
}
