package com.example.wms.mapper;

import com.example.wms.entity.AuditLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

// 审计日志 Mapper
@Mapper
public interface AuditLogMapper {
    // 写入审计日志
        @Insert("""
        INSERT INTO app_audit_log (
            tenant_id, auth_tenant_id, auth_tenant_code, is_cross_tenant, actor_username, action, entity_type, entity_id, detail, delete_reason,
            status, request_id, client_ip, user_agent, method, path, http_status, error_code, error_message, duration_ms, created_at
        )
        VALUES (
            #{tenantId}, #{authTenantId}, #{authTenantCode}, #{crossTenant}, #{actorUsername}, #{action}, #{entityType}, #{entityId}, #{detail}, #{deleteReason},
            #{status}, #{requestId}, #{clientIp}, #{userAgent}, #{method}, #{path}, #{httpStatus}, #{errorCode}, #{errorMessage}, #{durationMs}, NOW()
        )
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AuditLog log);

    // 分页查询审计日志
    @Select("""
        <script>
        SELECT a.id,
               a.tenant_id,
               t.code AS tenant_code,
               a.auth_tenant_id,
               a.auth_tenant_code,
               a.is_cross_tenant AS cross_tenant,
               a.actor_username,
               a.action,
               a.entity_type,
               a.entity_id,
               a.detail,
               a.delete_reason,
               a.status,
               a.request_id,
               a.client_ip,
               a.user_agent,
               a.method,
               a.path,
               a.http_status,
               a.error_code,
               a.error_message,
               a.duration_ms,
               a.created_at
        FROM app_audit_log a
        LEFT JOIN app_tenant t ON a.tenant_id = t.id
        WHERE 1 = 1
        <if test="tenantId != null">
          AND a.tenant_id = #{tenantId}
        </if>
        <if test="keyword != null and keyword != ''">
          AND (
            a.actor_username ILIKE CONCAT('%', #{keyword}, '%')
            OR a.action ILIKE CONCAT('%', #{keyword}, '%')
            OR a.entity_type ILIKE CONCAT('%', #{keyword}, '%')
            OR a.entity_id ILIKE CONCAT('%', #{keyword}, '%')
            OR a.detail ILIKE CONCAT('%', #{keyword}, '%')
            OR a.delete_reason ILIKE CONCAT('%', #{keyword}, '%')
          )
        </if>
        <if test="action != null and action != ''">
          AND a.action = #{action}
        </if>
        <if test="entityType != null and entityType != ''">
          AND a.entity_type = #{entityType}
        </if>
        <if test="actorUsername != null and actorUsername != ''">
          AND a.actor_username = #{actorUsername}
        </if>
        <if test="status != null and status != ''">
          AND a.status = #{status}
        </if>
        <if test="requestId != null and requestId != ''">
          AND a.request_id = #{requestId}
        </if>
        <if test="method != null and method != ''">
          AND a.method = #{method}
        </if>
        <if test="path != null and path != ''">
          AND a.path ILIKE CONCAT('%', #{path}, '%')
        </if>
        <if test="deleteReason != null and deleteReason != ''">
          AND a.delete_reason ILIKE CONCAT('%', #{deleteReason}, '%')
        </if>
        <if test="errorCode != null and errorCode != ''">
          AND a.error_code = #{errorCode}
        </if>
        <if test="errorMessage != null and errorMessage != ''">
          AND a.error_message ILIKE CONCAT('%', #{errorMessage}, '%')
        </if>
        <if test="httpStatus != null">
          AND a.http_status = #{httpStatus}
        </if>
        <if test="startTime != null">
          AND a.created_at &gt;= #{startTime}
        </if>
        <if test="endTime != null">
          AND a.created_at &lt;= #{endTime}
        </if>
        ORDER BY a.created_at DESC
        LIMIT #{size} OFFSET #{offset}
        </script>
        """)
    java.util.List<AuditLog> page(@Param("tenantId") Long tenantId,
                                  @Param("keyword") String keyword,
                                  @Param("action") String action,
                                  @Param("entityType") String entityType,
                                  @Param("actorUsername") String actorUsername,
                                  @Param("status") String status,
                                  @Param("requestId") String requestId,
                                  @Param("method") String method,
                                  @Param("path") String path,
                                  @Param("deleteReason") String deleteReason,
                                  @Param("errorCode") String errorCode,
                                  @Param("errorMessage") String errorMessage,
                                  @Param("httpStatus") Integer httpStatus,
                                  @Param("startTime") java.time.Instant startTime,
                                  @Param("endTime") java.time.Instant endTime,
                                  @Param("size") long size,
                                  @Param("offset") long offset);

    // 统计审计日志数量
    @Select("""
        <script>
        SELECT COUNT(1)
        FROM app_audit_log a
        WHERE 1 = 1
        <if test="tenantId != null">
          AND a.tenant_id = #{tenantId}
        </if>
        <if test="keyword != null and keyword != ''">
          AND (
            a.actor_username ILIKE CONCAT('%', #{keyword}, '%')
            OR a.action ILIKE CONCAT('%', #{keyword}, '%')
            OR a.entity_type ILIKE CONCAT('%', #{keyword}, '%')
            OR a.entity_id ILIKE CONCAT('%', #{keyword}, '%')
            OR a.detail ILIKE CONCAT('%', #{keyword}, '%')
            OR a.delete_reason ILIKE CONCAT('%', #{keyword}, '%')
          )
        </if>
        <if test="action != null and action != ''">
          AND a.action = #{action}
        </if>
        <if test="entityType != null and entityType != ''">
          AND a.entity_type = #{entityType}
        </if>
        <if test="actorUsername != null and actorUsername != ''">
          AND a.actor_username = #{actorUsername}
        </if>
        <if test="status != null and status != ''">
          AND a.status = #{status}
        </if>
        <if test="requestId != null and requestId != ''">
          AND a.request_id = #{requestId}
        </if>
        <if test="method != null and method != ''">
          AND a.method = #{method}
        </if>
        <if test="path != null and path != ''">
          AND a.path ILIKE CONCAT('%', #{path}, '%')
        </if>
        <if test="deleteReason != null and deleteReason != ''">
          AND a.delete_reason ILIKE CONCAT('%', #{deleteReason}, '%')
        </if>
        <if test="errorCode != null and errorCode != ''">
          AND a.error_code = #{errorCode}
        </if>
        <if test="errorMessage != null and errorMessage != ''">
          AND a.error_message ILIKE CONCAT('%', #{errorMessage}, '%')
        </if>
        <if test="httpStatus != null">
          AND a.http_status = #{httpStatus}
        </if>
        <if test="startTime != null">
          AND a.created_at &gt;= #{startTime}
        </if>
        <if test="endTime != null">
          AND a.created_at &lt;= #{endTime}
        </if>
        </script>
        """)
    long count(@Param("tenantId") Long tenantId,
               @Param("keyword") String keyword,
               @Param("action") String action,
               @Param("entityType") String entityType,
               @Param("actorUsername") String actorUsername,
               @Param("status") String status,
               @Param("requestId") String requestId,
               @Param("method") String method,
               @Param("path") String path,
               @Param("deleteReason") String deleteReason,
               @Param("errorCode") String errorCode,
               @Param("errorMessage") String errorMessage,
               @Param("httpStatus") Integer httpStatus,
               @Param("startTime") java.time.Instant startTime,
               @Param("endTime") java.time.Instant endTime);

    // 导出审计日志
    @Select("""
        <script>
        SELECT a.id,
               a.tenant_id,
               t.code AS tenant_code,
               a.auth_tenant_id,
               a.auth_tenant_code,
               a.is_cross_tenant AS cross_tenant,
               a.actor_username,
               a.action,
               a.entity_type,
               a.entity_id,
               a.detail,
               a.delete_reason,
               a.status,
               a.request_id,
               a.client_ip,
               a.user_agent,
               a.method,
               a.path,
               a.http_status,
               a.error_code,
               a.error_message,
               a.duration_ms,
               a.created_at
        FROM app_audit_log a
        LEFT JOIN app_tenant t ON a.tenant_id = t.id
        WHERE 1 = 1
        <if test="tenantId != null">
          AND a.tenant_id = #{tenantId}
        </if>
        <if test="keyword != null and keyword != ''">
          AND (
            a.actor_username ILIKE CONCAT('%', #{keyword}, '%')
            OR a.action ILIKE CONCAT('%', #{keyword}, '%')
            OR a.entity_type ILIKE CONCAT('%', #{keyword}, '%')
            OR a.entity_id ILIKE CONCAT('%', #{keyword}, '%')
            OR a.detail ILIKE CONCAT('%', #{keyword}, '%')
            OR a.delete_reason ILIKE CONCAT('%', #{keyword}, '%')
          )
        </if>
        <if test="action != null and action != ''">
          AND a.action = #{action}
        </if>
        <if test="entityType != null and entityType != ''">
          AND a.entity_type = #{entityType}
        </if>
        <if test="actorUsername != null and actorUsername != ''">
          AND a.actor_username = #{actorUsername}
        </if>
        <if test="status != null and status != ''">
          AND a.status = #{status}
        </if>
        <if test="requestId != null and requestId != ''">
          AND a.request_id = #{requestId}
        </if>
        <if test="method != null and method != ''">
          AND a.method = #{method}
        </if>
        <if test="path != null and path != ''">
          AND a.path ILIKE CONCAT('%', #{path}, '%')
        </if>
        <if test="deleteReason != null and deleteReason != ''">
          AND a.delete_reason ILIKE CONCAT('%', #{deleteReason}, '%')
        </if>
        <if test="errorCode != null and errorCode != ''">
          AND a.error_code = #{errorCode}
        </if>
        <if test="errorMessage != null and errorMessage != ''">
          AND a.error_message ILIKE CONCAT('%', #{errorMessage}, '%')
        </if>
        <if test="httpStatus != null">
          AND a.http_status = #{httpStatus}
        </if>
        <if test="startTime != null">
          AND a.created_at &gt;= #{startTime}
        </if>
        <if test="endTime != null">
          AND a.created_at &lt;= #{endTime}
        </if>
        ORDER BY a.created_at DESC
        LIMIT #{limit}
        </script>
        """)
    java.util.List<AuditLog> export(@Param("tenantId") Long tenantId,
                                    @Param("keyword") String keyword,
                                    @Param("action") String action,
                                    @Param("entityType") String entityType,
                                    @Param("actorUsername") String actorUsername,
                                    @Param("status") String status,
                                    @Param("requestId") String requestId,
                                    @Param("method") String method,
                                    @Param("path") String path,
                                    @Param("deleteReason") String deleteReason,
                                    @Param("errorCode") String errorCode,
                                    @Param("errorMessage") String errorMessage,
                                    @Param("httpStatus") Integer httpStatus,
                                    @Param("startTime") java.time.Instant startTime,
                                    @Param("endTime") java.time.Instant endTime,
                                    @Param("limit") long limit);
}
