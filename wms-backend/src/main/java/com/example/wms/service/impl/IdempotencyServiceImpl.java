package com.example.wms.service.impl;

import com.example.wms.entity.IdempotencyRecord;
import com.example.wms.exception.DuplicateRequestException;
import com.example.wms.mapper.IdempotencyMapper;
import com.example.wms.service.IdempotencyService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

// 幂等服务实现
@Service
public class IdempotencyServiceImpl implements IdempotencyService {
    private final IdempotencyMapper idempotencyMapper;
    private final long ttlSeconds;

    public IdempotencyServiceImpl(IdempotencyMapper idempotencyMapper,
                                  @Value("${wms.idempotency.ttl-seconds:300}") long ttlSeconds) {
        this.idempotencyMapper = idempotencyMapper;
        this.ttlSeconds = ttlSeconds;
    }

    @Override
    public void checkAndStore(String key, String method, String path, Long tenantId, String username) {
        if (key == null || key.isBlank()) {
            return;
        }
        IdempotencyRecord existing = idempotencyMapper.findValid(key);
        if (existing != null) {
            throw new DuplicateRequestException("重复请求，请勿重复提交");
        }
        idempotencyMapper.deleteExpired(key);
        IdempotencyRecord record = new IdempotencyRecord();
        record.setIdempotencyKey(key);
        record.setMethod(method);
        record.setPath(path);
        record.setTenantId(tenantId);
        record.setUsername(username);
        record.setExpiresAt(Instant.now().plusSeconds(ttlSeconds));
        idempotencyMapper.insert(record);
    }
}
