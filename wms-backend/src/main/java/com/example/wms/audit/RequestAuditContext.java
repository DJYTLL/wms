package com.example.wms.audit;

// 请求级审计上下文：为审计日志补充 requestId、IP、UA 与耗时
public final class RequestAuditContext {
    private static final ThreadLocal<RequestAuditContext> HOLDER = new ThreadLocal<>();

    private String requestId;
    private String clientIp;
    private String userAgent;
    private String method;
    private String path;
    private Long authTenantId;
    private String authTenantCode;
    private Boolean crossTenant;
    private long startNanos;

    public static void set(RequestAuditContext context) {
        HOLDER.set(context);
    }

    public static RequestAuditContext get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Long getAuthTenantId() {
        return authTenantId;
    }

    public void setAuthTenantId(Long authTenantId) {
        this.authTenantId = authTenantId;
    }

    public String getAuthTenantCode() {
        return authTenantCode;
    }

    public void setAuthTenantCode(String authTenantCode) {
        this.authTenantCode = authTenantCode;
    }

    public Boolean getCrossTenant() {
        return crossTenant;
    }

    public void setCrossTenant(Boolean crossTenant) {
        this.crossTenant = crossTenant;
    }

    public long getStartNanos() {
        return startNanos;
    }

    public void setStartNanos(long startNanos) {
        this.startNanos = startNanos;
    }
}
