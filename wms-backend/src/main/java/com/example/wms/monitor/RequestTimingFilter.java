package com.example.wms.monitor;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.example.wms.audit.RequestAuditContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// 请求耗时统计与慢请求告警
@Component
public class RequestTimingFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(RequestTimingFilter.class);
    private final RequestSqlTraceRecorder requestSqlTraceRecorder;

    @Value("${wms.monitor.slow-request-ms:1000}")
    private long slowRequestMs;

    public RequestTimingFilter(RequestSqlTraceRecorder requestSqlTraceRecorder) {
        this.requestSqlTraceRecorder = requestSqlTraceRecorder;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        long start = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long costMs = (System.nanoTime() - start) / 1_000_000;
            response.setHeader("X-Response-Time", costMs + "ms");
            String path = request.getRequestURI();
            String method = request.getMethod();
            int status = response.getStatus();
            if (costMs >= slowRequestMs) {
                logger.warn("Slow request: method={} path={} status={} costMs={}", method, path, status, costMs);
            } else {
                logger.info("Request: method={} path={} status={} costMs={}", method, path, status, costMs);
            }
            try {
                requestSqlTraceRecorder.record(RequestAuditContext.get(), status, costMs, java.time.Instant.now());
            } catch (Exception ex) {
                logger.warn("Failed to persist SQL request trace: method={} path={} status={}", method, path, status, ex);
            }
        }
    }
}
