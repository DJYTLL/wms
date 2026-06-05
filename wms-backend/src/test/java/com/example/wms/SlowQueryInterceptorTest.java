package com.example.wms;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.example.wms.audit.RequestAuditContext;
import com.example.wms.monitor.RequestSqlTraceContext;
import com.example.wms.monitor.SqlTimingSettingsProvider;
import com.example.wms.monitor.SlowQueryInterceptor;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.BatchResult;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.LocalCacheScope;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.transaction.Transaction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SlowQueryInterceptorTest {
    private final Logger logger = (Logger) LoggerFactory.getLogger(SlowQueryInterceptor.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    @AfterEach
    void tearDown() {
        RequestAuditContext.clear();
        logger.detachAppender(appender);
        appender.stop();
    }

    @Test
    void disabledTimingDoesNotLogFastSql() throws Throwable {
        SqlTimingSettingsProvider settingsProvider = mock(SqlTimingSettingsProvider.class);
        when(settingsProvider.isSqlTimingEnabled()).thenReturn(false);
        when(settingsProvider.isSqlTimingLogParamsEnabled()).thenReturn(true);
        SlowQueryInterceptor interceptor = new SlowQueryInterceptor(settingsProvider);
        ReflectionTestUtils.setField(interceptor, "sqlTimingEnabled", false);
        ReflectionTestUtils.setField(interceptor, "sqlTimingLogParams", true);
        ReflectionTestUtils.setField(interceptor, "slowQueryMs", 500L);

        attachAppender();

        interceptor.intercept(queryInvocation("com.example.Mapper.select", Map.of("id", 86), 0L));

        assertTrue(appender.list.isEmpty());
    }

    @Test
    void enabledTimingLogsEverySqlWithParams() throws Throwable {
        SqlTimingSettingsProvider settingsProvider = mock(SqlTimingSettingsProvider.class);
        when(settingsProvider.isSqlTimingEnabled()).thenReturn(true);
        when(settingsProvider.isSqlTimingLogParamsEnabled()).thenReturn(true);
        SlowQueryInterceptor interceptor = new SlowQueryInterceptor(settingsProvider);
        ReflectionTestUtils.setField(interceptor, "sqlTimingEnabled", true);
        ReflectionTestUtils.setField(interceptor, "sqlTimingLogParams", true);
        ReflectionTestUtils.setField(interceptor, "slowQueryMs", 500L);

        attachAppender();

        interceptor.intercept(queryInvocation("com.example.Mapper.select", Map.of("id", 86, "status", "APPROVED"), 0L));

        assertEquals(1, appender.list.size());
        ILoggingEvent event = appender.list.get(0);
        assertEquals(Level.INFO, event.getLevel());
        assertTrue(event.getFormattedMessage().contains("SQL timing: type=query"));
        assertTrue(event.getFormattedMessage().contains("mapperId=com.example.Mapper.select"));
        assertTrue(event.getFormattedMessage().contains("params={id=86, status=APPROVED}"));
    }

    @Test
    void slowSqlLogsInfoAndWarn() throws Throwable {
        SqlTimingSettingsProvider settingsProvider = mock(SqlTimingSettingsProvider.class);
        when(settingsProvider.isSqlTimingEnabled()).thenReturn(true);
        when(settingsProvider.isSqlTimingLogParamsEnabled()).thenReturn(false);
        SlowQueryInterceptor interceptor = new SlowQueryInterceptor(settingsProvider);
        ReflectionTestUtils.setField(interceptor, "sqlTimingEnabled", true);
        ReflectionTestUtils.setField(interceptor, "sqlTimingLogParams", false);
        ReflectionTestUtils.setField(interceptor, "slowQueryMs", 0L);

        attachAppender();

        interceptor.intercept(updateInvocation("com.example.Mapper.update", Map.of("id", 86), 0L));

        assertEquals(2, appender.list.size());
        assertEquals(Level.INFO, appender.list.get(0).getLevel());
        assertEquals(Level.WARN, appender.list.get(1).getLevel());
        assertTrue(appender.list.get(1).getFormattedMessage().contains("Slow query: type=update"));
        assertTrue(appender.list.get(1).getFormattedMessage().contains("params=[disabled]"));
    }

    @Test
    void disabledParamLoggingMasksParameters() throws Throwable {
        SqlTimingSettingsProvider settingsProvider = mock(SqlTimingSettingsProvider.class);
        when(settingsProvider.isSqlTimingEnabled()).thenReturn(true);
        when(settingsProvider.isSqlTimingLogParamsEnabled()).thenReturn(false);
        SlowQueryInterceptor interceptor = new SlowQueryInterceptor(settingsProvider);
        ReflectionTestUtils.setField(interceptor, "sqlTimingEnabled", true);
        ReflectionTestUtils.setField(interceptor, "sqlTimingLogParams", false);
        ReflectionTestUtils.setField(interceptor, "slowQueryMs", 500L);

        attachAppender();

        interceptor.intercept(queryInvocation("com.example.Mapper.select", Map.of("id", 86), 0L));

        assertEquals(1, appender.list.size());
        assertTrue(appender.list.get(0).getFormattedMessage().contains("params=[disabled]"));
        assertFalse(appender.list.get(0).getFormattedMessage().contains("id=86"));
    }

    @Test
    void enabledTimingAppendsSqlTraceIntoRequestAuditContext() throws Throwable {
        SqlTimingSettingsProvider settingsProvider = mock(SqlTimingSettingsProvider.class);
        when(settingsProvider.isSqlTimingEnabled()).thenReturn(true);
        when(settingsProvider.isSqlTimingLogParamsEnabled()).thenReturn(true);
        SlowQueryInterceptor interceptor = new SlowQueryInterceptor(settingsProvider);
        ReflectionTestUtils.setField(interceptor, "sqlTimingEnabled", true);
        ReflectionTestUtils.setField(interceptor, "sqlTimingLogParams", true);
        ReflectionTestUtils.setField(interceptor, "slowQueryMs", 500L);

        RequestAuditContext context = new RequestAuditContext();
        context.setRequestId("req-86");
        RequestAuditContext.set(context);

        interceptor.intercept(queryInvocation("com.example.Mapper.select", Map.of("id", 86, "status", "APPROVED"), 0L));

        RequestSqlTraceContext traceContext = RequestAuditContext.get().getSqlTraceContext();
        assertEquals(1, traceContext.getEntries().size());
        assertEquals(1, traceContext.getEntryCount());
        assertTrue(traceContext.getTotalCostMs() >= 0);
        assertEquals(1, traceContext.getEntries().get(0).getSequenceNo());
        assertEquals("req-86", traceContext.getEntries().get(0).getRequestId());
        assertEquals("com.example.Mapper.select", traceContext.getEntries().get(0).getMapperId());
        assertEquals("query", traceContext.getEntries().get(0).getSqlType());
        assertTrue(traceContext.getEntries().get(0).getSqlText().contains("SELECT * FROM erp_sale_order WHERE id = ?"));
        assertEquals("{id=86, status=APPROVED}", traceContext.getEntries().get(0).getParamsSummary());
    }

    private void attachAppender() {
        appender.start();
        logger.addAppender(appender);
    }

    private Invocation queryInvocation(String statementId, Object parameter, long sleepMs) throws Exception {
        MappedStatement statement = mappedStatement(statementId, SqlCommandType.SELECT);
        Method method = Executor.class.getMethod(
            "query",
            MappedStatement.class,
            Object.class,
            RowBounds.class,
            ResultHandler.class
        );
        return new Invocation(new DummyExecutor(), method, new Object[] {
            statement, parameter, RowBounds.DEFAULT, null
        });
    }

    private Invocation updateInvocation(String statementId, Object parameter, long sleepMs) throws Exception {
        MappedStatement statement = mappedStatement(statementId, SqlCommandType.UPDATE);
        Method method = Executor.class.getMethod(
            "update",
            MappedStatement.class, Object.class
        );
        return new Invocation(new DummyExecutor(), method, new Object[] {statement, parameter});
    }

    private MappedStatement mappedStatement(String id, SqlCommandType sqlCommandType) {
        Configuration configuration = new Configuration();
        configuration.setLocalCacheScope(LocalCacheScope.STATEMENT);
        SqlSource sqlSource = parameterObject -> new BoundSql(
            configuration,
            "SELECT * FROM erp_sale_order WHERE id = ?",
            List.of(new ParameterMapping.Builder(configuration, "id", Object.class).build()),
            parameterObject
        );
        return new MappedStatement.Builder(configuration, id, sqlSource, sqlCommandType).build();
    }

    public static class DummyExecutor implements Executor {
        @Override
        public int update(MappedStatement ms, Object parameter) {
            return 1;
        }

        @Override
        public <E> List<E> query(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler) {
            return List.of();
        }

        @Override
        public <E> List<E> query(MappedStatement ms,
                                 Object parameter,
                                 RowBounds rowBounds,
                                 ResultHandler resultHandler,
                                 CacheKey cacheKey,
                                 BoundSql boundSql) {
            return List.of();
        }

        @Override
        public <E> Cursor<E> queryCursor(MappedStatement ms, Object parameter, RowBounds rowBounds) {
            return null;
        }

        @Override
        public List<BatchResult> flushStatements() {
            return Collections.emptyList();
        }

        @Override
        public void commit(boolean required) {
        }

        @Override
        public void rollback(boolean required) {
        }

        @Override
        public CacheKey createCacheKey(MappedStatement ms, Object parameterObject, RowBounds rowBounds, BoundSql boundSql) {
            return new CacheKey();
        }

        @Override
        public boolean isCached(MappedStatement ms, CacheKey key) {
            return false;
        }

        @Override
        public void clearLocalCache() {
        }

        @Override
        public void deferLoad(MappedStatement ms, org.apache.ibatis.reflection.MetaObject resultObject,
                              String property, CacheKey key, Class<?> targetType) {
        }

        @Override
        public Transaction getTransaction() {
            return new Transaction() {
                @Override
                public Connection getConnection() {
                    return null;
                }

                @Override
                public void commit() {
                }

                @Override
                public void rollback() {
                }

                @Override
                public void close() {
                }

                @Override
                public Integer getTimeout() {
                    return null;
                }
            };
        }

        @Override
        public void close(boolean forceRollback) {
        }

        @Override
        public boolean isClosed() {
            return false;
        }

        @Override
        public void setExecutorWrapper(Executor executor) {
        }
    }
}
