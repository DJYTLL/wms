package com.example.wms.config;

import com.example.wms.monitor.IdempotencyInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// Web MVC 配置
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final IdempotencyInterceptor idempotencyInterceptor;

    public WebMvcConfig(@NonNull IdempotencyInterceptor idempotencyInterceptor) {
        this.idempotencyInterceptor = idempotencyInterceptor;
    }

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(idempotencyInterceptor)
            .addPathPatterns("/api/**");
    }
}
