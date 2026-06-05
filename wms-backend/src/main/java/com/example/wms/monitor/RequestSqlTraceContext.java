package com.example.wms.monitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RequestSqlTraceContext {
    private final List<RequestSqlTraceEntry> entries = new ArrayList<>();
    private long totalCostMs;
    private int nextSequenceNo = 1;

    public List<RequestSqlTraceEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    public long getTotalCostMs() {
        return totalCostMs;
    }

    public int getEntryCount() {
        return entries.size();
    }

    public int nextSequenceNo() {
        return nextSequenceNo++;
    }

    public void append(RequestSqlTraceEntry entry) {
        entries.add(entry);
        totalCostMs += entry.getCostMs();
    }
}
