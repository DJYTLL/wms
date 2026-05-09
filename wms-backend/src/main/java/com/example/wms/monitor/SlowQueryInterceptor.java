package com.example.wms.monitor;

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

    @Value("${wms.monitor.slow-query-ms:500}")
    private long slowQueryMs;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        long start = System.nanoTime();
        try {
            return invocation.proceed();
        } finally {
            long costMs = (System.nanoTime() - start) / 1_000_000;
            if (costMs >= slowQueryMs) {
                MappedStatement statement = (MappedStatement) invocation.getArgs()[0];
                Object parameter = invocation.getArgs().length > 1 ? invocation.getArgs()[1] : null;
                BoundSql boundSql = statement.getBoundSql(parameter);
                String sql = boundSql.getSql().replaceAll("\\s+", " ").trim();
                logger.warn("Slow query: id={} costMs={} sql={}", statement.getId(), costMs, sql);
            }
        }
    }
}
