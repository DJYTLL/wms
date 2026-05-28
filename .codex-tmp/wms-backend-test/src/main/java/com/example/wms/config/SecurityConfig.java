package com.example.wms.config;

import com.example.wms.audit.RequestAuditContextFilter;
import com.example.wms.security.JwtAuthenticationFilter;
import com.example.wms.security.JwtTokenService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.example.wms.service.UserAccountService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

// Spring Security 配置
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public RoleHierarchy superAdminRoleHierarchy() {
        String hierarchy = PermissionSeedProvider.permissionSeeds().stream()
            .map(seed -> "ROLE_super_admin > PERM_" + seed.code())
            .distinct()
            .collect(Collectors.joining("\n"));
        return RoleHierarchyImpl.fromHierarchy(hierarchy);
    }

    @Bean
    public static MethodSecurityExpressionHandler methodSecurityExpressionHandler(RoleHierarchy roleHierarchy) {
        DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
        handler.setRoleHierarchy(roleHierarchy);
        return handler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtTokenService jwtTokenService,
                                                   UserAccountService userAccountService) throws Exception {
        // 基于 JWT 的认证过滤器
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtTokenService, userAccountService);
        // 请求级审计上下文过滤器
        RequestAuditContextFilter auditContextFilter = new RequestAuditContextFilter();

        http
            // 关闭 CSRF，使用无状态 API
            .csrf(csrf -> csrf.disable())
            // 不创建会话，依赖 JWT
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // 放行登录、刷新与健康检查
                .requestMatchers(
                    "/api/login",
                    "/api/refresh",
                    "/api/logout",
                    "/api/system-configs/public",
                    "/actuator/health",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                // 未认证：返回 401
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(401);
                    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"code\":401,\"message\":\"未认证\",\"data\":null}");
                })
                // 无权限：返回 403
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(403);
                    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"code\":403,\"message\":\"无权限\",\"data\":null}");
                })
            )
            // 禁用 Basic 认证，避免浏览器弹窗
            .httpBasic(httpBasic -> httpBasic.disable());

        // 先注入请求审计上下文，再执行 JWT 认证
        http.addFilterBefore(auditContextFilter, UsernamePasswordAuthenticationFilter.class);
        // 在用户名密码过滤器前插入 JWT 过滤器
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(UserAccountService userAccountService,
                                                       PasswordEncoder passwordEncoder) {
        // 使用用户服务与密码编码器构建认证管理器
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userAccountService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt 密码编码
        return new BCryptPasswordEncoder();
    }
}

