package com.example.wms;

import com.example.wms.config.MybatisPlusConfig;
import com.example.wms.monitor.SlowQueryInterceptor;
import com.example.wms.monitor.SqlTimingSettingsProvider;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MybatisPlusConfigTest {

    @Test
    void exposesSlowQueryInterceptorBean() {
        MybatisPlusConfig config = new MybatisPlusConfig();

        SlowQueryInterceptor interceptor = config.slowQueryInterceptor(mock(SqlTimingSettingsProvider.class));

        assertThat(interceptor).isNotNull();
    }
}
