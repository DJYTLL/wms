package com.example.wms.mapper.monitor;

import com.example.wms.dto.monitor.SqlRequestTraceRow;
import com.example.wms.entity.SqlRequestTrace;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.Instant;
import java.util.List;

@Mapper
public interface SqlRequestTraceMapper {
    @Insert("""
        INSERT INTO app_sql_request_trace (
            tenant_id, request_id, request_path, request_method, response_status,
            request_cost_ms, sql_total_cost_ms, sql_count, username, user_id,
            started_at, finished_at, created_at
        ) VALUES (
            #{tenantId}, #{requestId}, #{requestPath}, #{requestMethod}, #{responseStatus},
            #{requestCostMs}, #{sqlTotalCostMs}, #{sqlCount}, #{username}, #{userId},
            #{startedAt}, #{finishedAt}, NOW()
        )
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SqlRequestTrace trace);

    @Select("""
        <script>
        SELECT request_id AS requestId,
               request_path AS requestPath,
               request_method AS requestMethod,
               response_status AS responseStatus,
               request_cost_ms AS requestCostMs,
               sql_total_cost_ms AS sqlTotalCostMs,
               sql_count AS sqlCount,
               username,
               started_at AS startedAt,
               finished_at AS finishedAt
        FROM app_sql_request_trace
        WHERE tenant_id = #{tenantId}
        <if test="requestPath != null and requestPath != ''">
          AND request_path ILIKE CONCAT('%', #{requestPath}, '%')
        </if>
        <if test="requestMethod != null and requestMethod != ''">
          AND request_method = #{requestMethod}
        </if>
        <if test="responseStatus != null">
          AND response_status = #{responseStatus}
        </if>
        <if test="minRequestCostMs != null">
          AND request_cost_ms &gt;= #{minRequestCostMs}
        </if>
        <if test="minSqlCostMs != null">
          AND sql_total_cost_ms &gt;= #{minSqlCostMs}
        </if>
        <if test="startAt != null">
          AND started_at &gt;= #{startAt}
        </if>
        <if test="endAt != null">
          AND started_at &lt;= #{endAt}
        </if>
        ORDER BY started_at DESC, id DESC
        LIMIT #{size} OFFSET #{offset}
        </script>
        """)
    List<SqlRequestTraceRow> page(@Param("tenantId") Long tenantId,
                                  @Param("requestPath") String requestPath,
                                  @Param("requestMethod") String requestMethod,
                                  @Param("responseStatus") Integer responseStatus,
                                  @Param("minRequestCostMs") Long minRequestCostMs,
                                  @Param("minSqlCostMs") Long minSqlCostMs,
                                  @Param("startAt") Instant startAt,
                                  @Param("endAt") Instant endAt,
                                  @Param("size") long size,
                                  @Param("offset") long offset);

    @Select("""
        <script>
        SELECT COUNT(1)
        FROM app_sql_request_trace
        WHERE tenant_id = #{tenantId}
        <if test="requestPath != null and requestPath != ''">
          AND request_path ILIKE CONCAT('%', #{requestPath}, '%')
        </if>
        <if test="requestMethod != null and requestMethod != ''">
          AND request_method = #{requestMethod}
        </if>
        <if test="responseStatus != null">
          AND response_status = #{responseStatus}
        </if>
        <if test="minRequestCostMs != null">
          AND request_cost_ms &gt;= #{minRequestCostMs}
        </if>
        <if test="minSqlCostMs != null">
          AND sql_total_cost_ms &gt;= #{minSqlCostMs}
        </if>
        <if test="startAt != null">
          AND started_at &gt;= #{startAt}
        </if>
        <if test="endAt != null">
          AND started_at &lt;= #{endAt}
        </if>
        </script>
        """)
    long count(@Param("tenantId") Long tenantId,
               @Param("requestPath") String requestPath,
               @Param("requestMethod") String requestMethod,
               @Param("responseStatus") Integer responseStatus,
               @Param("minRequestCostMs") Long minRequestCostMs,
               @Param("minSqlCostMs") Long minSqlCostMs,
               @Param("startAt") Instant startAt,
               @Param("endAt") Instant endAt);
}
