package com.example.wms.service;

// 幂等服务
public interface IdempotencyService {
    void checkAndStore(String key, String method, String path, Long tenantId, String username);
}
