package com.example.wms.service.erp.support;

import com.example.wms.entity.SystemConfig;
import com.example.wms.mapper.SystemConfigMapper;
import com.example.wms.mapper.erp.ErpOrderSequenceMapper;
import com.example.wms.tenant.TenantContext;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

// ERP基础资料编码生成器
@Component
public class ErpMasterDataCodeGenerator {
    private final ErpOrderSequenceMapper erpOrderSequenceMapper;
    private final SystemConfigMapper systemConfigMapper;

    public ErpMasterDataCodeGenerator(ErpOrderSequenceMapper erpOrderSequenceMapper,
                                      SystemConfigMapper systemConfigMapper) {
        this.erpOrderSequenceMapper = erpOrderSequenceMapper;
        this.systemConfigMapper = systemConfigMapper;
    }

    public String nextCode(String sequenceType,
                           String prefixConfigKey,
                           String defaultPrefix,
                           String dateFormatConfigKey,
                           String seqLengthConfigKey) {
        Long tenantId = TenantContext.requireTenantId();
        String prefix = readConfig(tenantId, prefixConfigKey, defaultPrefix);
        String dateFormat = readConfig(tenantId, dateFormatConfigKey, "yyyyMMdd");
        int seqLength = readIntConfig(tenantId, seqLengthConfigKey, 4);
        String dateKey = LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern(dateFormat));
        erpOrderSequenceMapper.insertIgnore(tenantId, sequenceType, dateKey);
        Long seq = erpOrderSequenceMapper.incrementAndGet(tenantId, sequenceType, dateKey);
        String seqStr = String.format("%0" + seqLength + "d", seq == null ? 1 : seq);
        return prefix + dateKey + seqStr;
    }

    private String readConfig(Long tenantId, String key, String fallback) {
        SystemConfig config = systemConfigMapper.findByKey(tenantId, key);
        if (config == null || config.getConfigValue() == null || config.getConfigValue().isBlank()) {
            return fallback;
        }
        return config.getConfigValue().trim();
    }

    private int readIntConfig(Long tenantId, String key, int fallback) {
        String value = readConfig(tenantId, key, String.valueOf(fallback));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
}
