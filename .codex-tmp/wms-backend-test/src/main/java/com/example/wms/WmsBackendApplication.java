package com.example.wms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;

// Spring Boot 应用入口
@SpringBootApplication
// 扫描 MyBatis Mapper 接口
@MapperScan("com.example.wms.mapper")
public class WmsBackendApplication {
    public static void main(String[] args) {
        // 启动 Spring Boot 容器
        SpringApplication.run(WmsBackendApplication.class, args);
    }
}

