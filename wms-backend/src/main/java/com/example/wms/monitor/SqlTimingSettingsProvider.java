package com.example.wms.monitor;

import com.example.wms.tenant.TenantContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SqlTimingSettingsProvider {
    private static final String SQL_TIMING_ENABLED_KEY = "wms.monitor.sql-timing-enabled";
    private static final String SQL_TIMING_LOG_PARAMS_KEY = "wms.monitor.sql-timing-log-params";

    private final JdbcTemplate jdbcTemplate;
    private final boolean fallbackSqlTimingEnabled;
    private final boolean fallbackSqlTimingLogParams;

    public SqlTimingSettingsProvider(JdbcTemplate jdbcTemplate,
                                     @Value("${wms.monitor.sql-timing-enabled:false}") boolean fallbackSqlTimingEnabled,
                                     @Value("${wms.monitor.sql-timing-log-params:false}") boolean fallbackSqlTimingLogParams) {
        this.jdbcTemplate = jdbcTemplate;
        this.fallbackSqlTimingEnabled = fallbackSqlTimingEnabled;
        this.fallbackSqlTimingLogParams = fallbackSqlTimingLogParams;
    }

    public boolean isSqlTimingEnabled() {
        return readBoolean(SQL_TIMING_ENABLED_KEY, fallbackSqlTimingEnabled);
    }

    public boolean isSqlTimingLogParamsEnabled() {
        return readBoolean(SQL_TIMING_LOG_PARAMS_KEY, fallbackSqlTimingLogParams);
    }

    private boolean readBoolean(String key, boolean fallbackValue) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            return fallbackValue;
        }
        try {
            String value = jdbcTemplate.query(
                "SELECT config_value FROM app_system_config WHERE tenant_id = ? AND config_key = ?",
                ps -> {
                    ps.setLong(1, tenantId);
                    ps.setString(2, key);
                },
                rs -> rs.next() ? rs.getString(1) : null
            );
            if (value == null || value.isBlank()) {
                return fallbackValue;
            }
            return Boolean.parseBoolean(value.trim());
        } catch (Exception ex) {
            return fallbackValue;
        }
    }
}
