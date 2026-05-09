package com.example.wms.aop;

import com.example.wms.exception.DuplicateRequestException;
import com.example.wms.exception.NotFoundException;
import com.example.wms.service.AuditLogService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import jakarta.validation.ConstraintViolationException;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// 审计日志切面：基于注解自动记录
@Aspect
@Component
public class AuditLogAspect {
    private final AuditLogService auditLogService;
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([^}]+)}");

    public AuditLogAspect(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Around("@annotation(auditLog)")
    public Object recordAudit(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {
        long start = System.nanoTime();
        Object result = null;
        String status = "SUCCESS";
        int httpStatus = 200;
        Throwable error = null;
        String errorCode = null;
        String errorMessage = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable ex) {
            status = "FAIL";
            httpStatus = mapHttpStatus(ex);
            errorCode = String.valueOf(httpStatus);
            errorMessage = truncate(ex.getMessage(), 400);
            error = ex;
            throw ex;
        } finally {
            long durationMs = (System.nanoTime() - start) / 1_000_000L;
            Object[] args = joinPoint.getArgs();
            String entityId = resolveValue(auditLog.entityId(), args, result);
            String detail = resolveValue(auditLog.detail(), args, result);
            if (error != null) {
                String suffix = "error=" + error.getClass().getSimpleName() + ":" + error.getMessage();
                detail = (detail == null || detail.isBlank()) ? suffix : (detail + " | " + suffix);
            }
            auditLogService.record(
                auditLog.action(),
                auditLog.entityType(),
                entityId,
                detail,
                status,
                httpStatus,
                durationMs,
                errorCode,
                errorMessage
            );
        }
    }

    private int mapHttpStatus(Throwable ex) {
        if (ex instanceof NotFoundException) {
            return 404;
        }
        if (ex instanceof AccessDeniedException) {
            return 403;
        }
        if (ex instanceof AuthenticationException) {
            return 401;
        }
        if (ex instanceof DuplicateRequestException) {
            return 409;
        }
        if (ex instanceof IllegalArgumentException
            || ex instanceof MethodArgumentNotValidException
            || ex instanceof BindException
            || ex instanceof ConstraintViolationException
            || ex instanceof HttpMessageNotReadableException) {
            return 400;
        }
        return 500;
    }

    private String truncate(String value, int maxLen) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= maxLen) {
            return trimmed;
        }
        return trimmed.substring(0, maxLen);
    }

    // 解析模板表达式（支持 {arg0}、{arg0.username}、{result.id}）
    private String resolveValue(String template, Object[] args, Object result) {
        if (template == null || template.isBlank()) {
            return null;
        }
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            Object value = resolveKey(key, args, result);
            matcher.appendReplacement(buffer, value == null ? "" : Matcher.quoteReplacement(value.toString()));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private Object resolveKey(String key, Object[] args, Object result) {
        if (key.startsWith("arg")) {
            int index = parseIndex(key.substring(3));
            if (index >= 0 && args != null && index < args.length) {
                return resolveProperty(args[index], propertyOf(key));
            }
        }
        if (key.startsWith("result")) {
            return resolveProperty(result, propertyOf(key));
        }
        return null;
    }

    private int parseIndex(String value) {
        String number = value.split("\\.")[0];
        try {
            return Integer.parseInt(number);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private String propertyOf(String key) {
        int dot = key.indexOf('.');
        return dot >= 0 ? key.substring(dot + 1) : null;
    }

    private Object resolveProperty(Object target, String property) {
        if (target == null) {
            return null;
        }
        if (property == null || property.isBlank()) {
            return target;
        }
        try {
            Method getter = target.getClass().getMethod(property);
            return getter.invoke(target);
        } catch (Exception ignored) {
            // ignore
        }
        try {
            String getterName = "get" + Character.toUpperCase(property.charAt(0)) + property.substring(1);
            Method getter = target.getClass().getMethod(getterName);
            return getter.invoke(target);
        } catch (Exception ignored) {
            // ignore
        }
        try {
            Field field = target.getClass().getDeclaredField(property);
            field.setAccessible(true);
            return field.get(target);
        } catch (Exception ignored) {
            return null;
        }
    }
}
