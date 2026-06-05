package com.example.wms.mapper.monitor;

import com.example.wms.dto.monitor.SqlTraceEntryRow;
import com.example.wms.entity.SqlTraceEntry;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SqlTraceEntryMapper {
    @Insert("""
        <script>
        INSERT INTO app_sql_trace_entry (
            tenant_id, request_trace_id, request_id, sequence_no, mapper_id,
            sql_type, cost_ms, sql_text, params_summary, executed_at, created_at
        ) VALUES
        <foreach collection="entries" item="entry" separator=",">
            (
                #{entry.tenantId}, #{entry.requestTraceId}, #{entry.requestId}, #{entry.sequenceNo}, #{entry.mapperId},
                #{entry.sqlType}, #{entry.costMs}, #{entry.sqlText}, #{entry.paramsSummary}, #{entry.executedAt}, NOW()
            )
        </foreach>
        </script>
        """)
    int batchInsert(@Param("entries") List<SqlTraceEntry> entries);

    @Select("""
        SELECT sequence_no AS sequenceNo,
               mapper_id AS mapperId,
               sql_type AS sqlType,
               cost_ms AS costMs,
               sql_text AS sqlText,
               params_summary AS paramsSummary,
               executed_at AS executedAt
        FROM app_sql_trace_entry
        WHERE tenant_id = #{tenantId}
          AND request_id = #{requestId}
        ORDER BY sequence_no ASC, id ASC
        """)
    List<SqlTraceEntryRow> findByRequestId(@Param("tenantId") Long tenantId,
                                           @Param("requestId") String requestId);
}
