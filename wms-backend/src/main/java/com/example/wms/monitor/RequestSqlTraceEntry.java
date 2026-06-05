package com.example.wms.monitor;

import java.time.Instant;

public final class RequestSqlTraceEntry {
    private final int sequenceNo;
    private final String requestId;
    private final String mapperId;
    private final String sqlType;
    private final long costMs;
    private final String sqlText;
    private final String paramsSummary;
    private final Instant executedAt;

    public RequestSqlTraceEntry(int sequenceNo,
                                String requestId,
                                String mapperId,
                                String sqlType,
                                long costMs,
                                String sqlText,
                                String paramsSummary,
                                Instant executedAt) {
        this.sequenceNo = sequenceNo;
        this.requestId = requestId;
        this.mapperId = mapperId;
        this.sqlType = sqlType;
        this.costMs = costMs;
        this.sqlText = sqlText;
        this.paramsSummary = paramsSummary;
        this.executedAt = executedAt;
    }

    public int getSequenceNo() {
        return sequenceNo;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getMapperId() {
        return mapperId;
    }

    public String getSqlType() {
        return sqlType;
    }

    public long getCostMs() {
        return costMs;
    }

    public String getSqlText() {
        return sqlText;
    }

    public String getParamsSummary() {
        return paramsSummary;
    }

    public Instant getExecutedAt() {
        return executedAt;
    }
}
