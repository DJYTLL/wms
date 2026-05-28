package com.example.wms.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// 审计日志注解：用于声明动作与实体信息
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {
    // 动作标识（如 USER_CREATE）
    String action();

    // 实体类型（如 user/role/permission）
    String entityType();

    // 实体 ID 表达式（例如 {arg0} 或 {result.id}）
    String entityId() default "";

    // 详情表达式（例如 username={arg0.username}）
    String detail() default "";
}
