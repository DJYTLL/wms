package com.example.wms.monitor;

import com.example.wms.audit.RequestAuditContext;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;

// 慢查询告警拦截器
@Component
@Intercepts({
    @Signature(type = Executor.class, method = "query",
        args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
    @Signature(type = Executor.class, method = "update",
        args = {MappedStatement.class, Object.class})
})
public class SlowQueryInterceptor implements Interceptor {
    private static final Logger logger = LoggerFactory.getLogger(SlowQueryInterceptor.class);
    private final SqlTimingSettingsProvider sqlTimingSettingsProvider;

    @Value("${wms.monitor.sql-timing-enabled:false}")
    private boolean sqlTimingEnabled;

    @Value("${wms.monitor.sql-timing-log-params:true}")
    private boolean sqlTimingLogParams;

    @Value("${wms.monitor.slow-query-ms:500}")
    private long slowQueryMs;

    public SlowQueryInterceptor(SqlTimingSettingsProvider sqlTimingSettingsProvider) {
        this.sqlTimingSettingsProvider = sqlTimingSettingsProvider;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        long start = System.nanoTime();
        try {
            return invocation.proceed();
        } finally {
            long costMs = (System.nanoTime() - start) / 1_000_000;
            MappedStatement statement = (MappedStatement) invocation.getArgs()[0];
            Object parameter = invocation.getArgs().length > 1 ? invocation.getArgs()[1] : null;
            BoundSql boundSql = statement.getBoundSql(parameter);
            String sql = SqlTimingFormatter.normalizeSql(boundSql);
            String sqlType = resolveSqlType(invocation);
            boolean timingEnabled = sqlTimingSettingsProvider.isSqlTimingEnabled();
            boolean logParamsEnabled = sqlTimingSettingsProvider.isSqlTimingLogParamsEnabled();
            String params = SqlTimingFormatter.summarizeParameter(parameter, logParamsEnabled);

            if (timingEnabled) {
                appendRequestTrace(statement.getId(), sqlType, costMs, sql, params);
                logger.info("SQL timing: type={} mapperId={} costMs={} params={} sql={}",
                    sqlType, statement.getId(), costMs, params, sql);
            }
            if (costMs >= slowQueryMs) {
                logger.warn("Slow query: type={} mapperId={} costMs={} params={} sql={}",
                    sqlType, statement.getId(), costMs, params, sql);
            }
        }
    }

    private String resolveSqlType(Invocation invocation) {
        return "update".equals(invocation.getMethod().getName()) ? "update" : "query";
    }

    private void appendRequestTrace(String mapperId, String sqlType, long costMs, String sql, String params) {
        RequestAuditContext auditContext = RequestAuditContext.get();
        if (auditContext == null) {
            return;
        }
        RequestSqlTraceContext sqlTraceContext = auditContext.getOrCreateSqlTraceContext();
        sqlTraceContext.append(new RequestSqlTraceEntry(
            sqlTraceContext.nextSequenceNo(),
            auditContext.getRequestId(),
            mapperId,
            sqlType,
            costMs,
            sql,
            params,
            Instant.now()
        ));
    }
}
