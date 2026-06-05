package com.example.wms.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.example.wms.monitor.SlowQueryInterceptor;
import com.example.wms.monitor.SqlTimingSettingsProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// MyBatis-Plus 配置
@Configuration
public class MybatisPlusConfig {
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        // 启用分页拦截器（PostgreSQL）
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        return interceptor;
    }

    @Bean
    public SlowQueryInterceptor slowQueryInterceptor(SqlTimingSettingsProvider sqlTimingSettingsProvider) {
        return new SlowQueryInterceptor(sqlTimingSettingsProvider);
    }
}
