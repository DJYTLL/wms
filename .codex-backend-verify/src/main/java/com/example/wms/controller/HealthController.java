package com.example.wms.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 健康检查接口
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public ResponseEntity<String> health(Authentication authentication) {
        // 返回当前用户或匿名标识
        String name = authentication != null ? authentication.getName() : "anonymous";
        return ResponseEntity.ok("ok:" + name);
    }
}

