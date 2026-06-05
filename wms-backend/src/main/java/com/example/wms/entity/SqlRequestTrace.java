package com.example.wms.entity;

import java.time.Instant;

public class SqlRequestTrace {
    private Long id;
    private Long tenantId;
    private String requestId;
    private String requestPath;
    private String requestMethod;
    private Integer responseStatus;
    private Long requestCostMs;
    private Long sqlTotalCostMs;
    private Integer sqlCount;
    private String username;
    private Long userId;
    private Instant startedAt;
    private Instant finishedAt;
    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getRequestPath() {
        return requestPath;
    }

    public void setRequestPath(String requestPath) {
        this.requestPath = requestPath;
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
    }

    public Integer getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(Integer responseStatus) {
        this.responseStatus = responseStatus;
    }

    public Long getRequestCostMs() {
        return requestCostMs;
    }

    public void setRequestCostMs(Long requestCostMs) {
        this.requestCostMs = requestCostMs;
    }

    public Long getSqlTotalCostMs() {
        return sqlTotalCostMs;
    }

    public void setSqlTotalCostMs(Long sqlTotalCostMs) {
        this.sqlTotalCostMs = sqlTotalCostMs;
    }

    public Integer getSqlCount() {
        return sqlCount;
    }

    public void setSqlCount(Integer sqlCount) {
        this.sqlCount = sqlCount;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
