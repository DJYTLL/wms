package com.example.wms.audit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

// 请求上下文过滤器：在每个请求开始时注入审计上下文
public class RequestAuditContextFilter extends OncePerRequestFilter {
    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        RequestAuditContext context = new RequestAuditContext();
        context.setRequestId(resolveRequestId(request));
        context.setClientIp(resolveClientIp(request));
        context.setUserAgent(request.getHeader("User-Agent"));
        context.setMethod(request.getMethod());
        context.setPath(request.getRequestURI());
        context.setStartNanos(System.nanoTime());
        RequestAuditContext.set(context);
        response.setHeader(REQUEST_ID_HEADER, context.getRequestId());
        try {
            filterChain.doFilter(request, response);
        } finally {
            RequestAuditContext.clear();
        }
    }

    private String resolveRequestId(HttpServletRequest request) {
        String fromHeader = request.getHeader(REQUEST_ID_HEADER);
        if (StringUtils.hasText(fromHeader)) {
            return fromHeader.trim();
        }
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xff)) {
            String first = xff.split(",")[0].trim();
            if (StringUtils.hasText(first)) {
                return first;
            }
        }
        return request.getRemoteAddr();
    }
}
