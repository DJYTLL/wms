package com.example.wms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.wms.dto.TenantBusinessSettingItemResponse;
import com.example.wms.dto.TenantBusinessSettingsResponse;
import com.example.wms.dto.TenantBusinessSettingsUpdateRequest;
import com.example.wms.dto.TenantDisplaySettingsResponse;
import com.example.wms.dto.TenantDisplaySettingsUpdateRequest;
import com.example.wms.entity.SystemConfig;
import com.example.wms.mapper.SystemConfigMapper;
import com.example.wms.service.TenantSettingService;
import com.example.wms.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// 租户设置服务实现
@Service
public class TenantSettingServiceImpl implements TenantSettingService {
    public static final String DEFAULT_PAGE_SIZE_KEY = "default.page.size";
    public static final int FALLBACK_PAGE_SIZE = 20;
    private static final int MIN_PAGE_SIZE = 5;
    private static final int MAX_PAGE_SIZE = 200;
    private static final int MAX_PREFIX_LENGTH = 8;
    private static final int MAX_SEQUENCE_LENGTH = 8;
    private static final Set<String> ALLOWED_DATE_FORMATS = Set.of(
        "yyyyMMdd",
        "yyMMdd",
        "yyyyMM",
        "yyyy-MM-dd",
        "yyyyMMddHHmmss"
    );
    private static final Set<String> INT_KEYS = Set.of(
        DEFAULT_PAGE_SIZE_KEY,
        "erp.order.no.seq-length",
        "erp.category.code.seq-length",
        "erp.customer-category.code.seq-length",
        "erp.customer.code.seq-length",
        "erp.delivery-method.code.seq-length",
        "erp.location.code.seq-length",
        "erp.payment-method.code.seq-length",
        "erp.print-template.code.seq-length",
        "erp.product.code.seq-length",
        "erp.receipt-method.code.seq-length",
        "erp.settlement-method.code.seq-length",
        "erp.supplier.code.seq-length",
        "erp.unit.code.seq-length",
        "erp.vehicle-brand.code.seq-length",
        "erp.vehicle-model.code.seq-length",
        "erp.vehicle-series.code.seq-length",
        "erp.warehouse.code.seq-length"
    );
    private static final List<SettingDefinition> CODE_RULE_DEFINITIONS = List.of(
        definition("erp.category.code.prefix", "商品分类编码前缀", "string", "CA", "商品分类编码前缀"),
        definition("erp.category.code.date-format", "商品分类日期格式", "string", "yyyyMMdd", "商品分类编码日期格式"),
        definition("erp.category.code.seq-length", "商品分类序列长度", "int", "4", "商品分类编码序列长度"),
        definition("erp.customer-category.code.prefix", "客户类别编码前缀", "string", "CC", "客户类别编码前缀"),
        definition("erp.customer-category.code.date-format", "客户类别日期格式", "string", "yyyyMMdd", "客户类别编码日期格式"),
        definition("erp.customer-category.code.seq-length", "客户类别序列长度", "int", "4", "客户类别编码序列长度"),
        definition("erp.customer.code.prefix", "客户编码前缀", "string", "CU", "客户编码前缀"),
        definition("erp.customer.code.date-format", "客户日期格式", "string", "yyyyMMdd", "客户编码日期格式"),
        definition("erp.customer.code.seq-length", "客户序列长度", "int", "4", "客户编码序列长度"),
        definition("erp.delivery-method.code.prefix", "送货方式编码前缀", "string", "DM", "送货方式编码前缀"),
        definition("erp.delivery-method.code.date-format", "送货方式日期格式", "string", "yyyyMMdd", "送货方式编码日期格式"),
        definition("erp.delivery-method.code.seq-length", "送货方式序列长度", "int", "4", "送货方式编码序列长度"),
        definition("erp.location.code.prefix", "库位编码前缀", "string", "LO", "库位编码前缀"),
        definition("erp.location.code.date-format", "库位日期格式", "string", "yyyyMMdd", "库位编码日期格式"),
        definition("erp.location.code.seq-length", "库位序列长度", "int", "4", "库位编码序列长度"),
        definition("erp.payment-method.code.prefix", "付款方式编码前缀", "string", "PM", "付款方式编码前缀"),
        definition("erp.payment-method.code.date-format", "付款方式日期格式", "string", "yyyyMMdd", "付款方式编码日期格式"),
        definition("erp.payment-method.code.seq-length", "付款方式序列长度", "int", "4", "付款方式编码序列长度"),
        definition("erp.print-template.code.prefix", "打印模板编码前缀", "string", "PT", "打印模板编码前缀"),
        definition("erp.print-template.code.date-format", "打印模板日期格式", "string", "yyyyMMdd", "打印模板编码日期格式"),
        definition("erp.print-template.code.seq-length", "打印模板序列长度", "int", "4", "打印模板编码序列长度"),
        definition("erp.product.code.prefix", "商品编码前缀", "string", "PR", "商品编码前缀"),
        definition("erp.product.code.date-format", "商品日期格式", "string", "yyyyMMdd", "商品编码日期格式"),
        definition("erp.product.code.seq-length", "商品序列长度", "int", "4", "商品编码序列长度"),
        definition("erp.receipt-method.code.prefix", "收款方式编码前缀", "string", "RM", "收款方式编码前缀"),
        definition("erp.receipt-method.code.date-format", "收款方式日期格式", "string", "yyyyMMdd", "收款方式编码日期格式"),
        definition("erp.receipt-method.code.seq-length", "收款方式序列长度", "int", "4", "收款方式编码序列长度"),
        definition("erp.settlement-method.code.prefix", "结算方式编码前缀", "string", "SM", "结算方式编码前缀"),
        definition("erp.settlement-method.code.date-format", "结算方式日期格式", "string", "yyyyMMdd", "结算方式编码日期格式"),
        definition("erp.settlement-method.code.seq-length", "结算方式序列长度", "int", "4", "结算方式编码序列长度"),
        definition("erp.supplier.code.prefix", "供应商编码前缀", "string", "SU", "供应商编码前缀"),
        definition("erp.supplier.code.date-format", "供应商日期格式", "string", "yyyyMMdd", "供应商编码日期格式"),
        definition("erp.supplier.code.seq-length", "供应商序列长度", "int", "4", "供应商编码序列长度"),
        definition("erp.unit.code.prefix", "单位编码前缀", "string", "UN", "单位编码前缀"),
        definition("erp.unit.code.date-format", "单位日期格式", "string", "yyyyMMdd", "单位编码日期格式"),
        definition("erp.unit.code.seq-length", "单位序列长度", "int", "4", "单位编码序列长度"),
        definition("erp.vehicle-brand.code.prefix", "车型品牌编码前缀", "string", "VB", "车型品牌编码前缀"),
        definition("erp.vehicle-brand.code.date-format", "车型品牌日期格式", "string", "yyyyMMdd", "车型品牌编码日期格式"),
        definition("erp.vehicle-brand.code.seq-length", "车型品牌序列长度", "int", "4", "车型品牌编码序列长度"),
        definition("erp.vehicle-series.code.prefix", "车系编码前缀", "string", "VS", "车系编码前缀"),
        definition("erp.vehicle-series.code.date-format", "车系日期格式", "string", "yyyyMMdd", "车系编码日期格式"),
        definition("erp.vehicle-series.code.seq-length", "车系序列长度", "int", "4", "车系编码序列长度"),
        definition("erp.vehicle-model.code.prefix", "车型编码前缀", "string", "VM", "车型编码前缀"),
        definition("erp.vehicle-model.code.date-format", "车型日期格式", "string", "yyyyMMdd", "车型编码日期格式"),
        definition("erp.vehicle-model.code.seq-length", "车型序列长度", "int", "4", "车型编码序列长度"),
        definition("erp.warehouse.code.prefix", "仓库编码前缀", "string", "WH", "仓库编码前缀"),
        definition("erp.warehouse.code.date-format", "仓库日期格式", "string", "yyyyMMdd", "仓库编码日期格式"),
        definition("erp.warehouse.code.seq-length", "仓库序列长度", "int", "4", "仓库编码序列长度")
    );
    private static final List<SettingDefinition> ORDER_RULE_DEFINITIONS = List.of(
        definition("erp.order.no.date-format", "单号日期格式", "string", "yyyyMMdd", "统一单号日期格式"),
        definition("erp.order.no.seq-length", "单号序列长度", "int", "4", "统一单号序列长度"),
        definition("erp.order.no.purchase.prefix", "采购单前缀", "string", "PO", "采购单号前缀"),
        definition("erp.order.no.purchase-return.prefix", "采购退货前缀", "string", "PR", "采购退货单号前缀"),
        definition("erp.order.no.sale.prefix", "销售单前缀", "string", "SO", "销售单号前缀"),
        definition("erp.order.no.sale-return.prefix", "销售退货前缀", "string", "SR", "销售退货单号前缀"),
        definition("erp.order.no.receipt.prefix", "收款单前缀", "string", "RC", "收款单号前缀"),
        definition("erp.order.no.payment.prefix", "付款单前缀", "string", "PY", "付款单号前缀"),
        definition("erp.order.no.ar-return.prefix", "应收退回前缀", "string", "AR", "应收退回单号前缀"),
        definition("erp.order.no.ap-return.prefix", "应付退回前缀", "string", "AP", "应付退回单号前缀"),
        definition("erp.order.no.stock-count.prefix", "库存调整前缀", "string", "SC", "库存调整单号前缀"),
        definition("erp.order.no.stock-init.prefix", "初始库存前缀", "string", "SI", "初始库存单号前缀"),
        definition("erp.order.no.stock-transfer.prefix", "库存移库前缀", "string", "ST", "库存移库单号前缀"),
        definition("erp.order.no.assembly.prefix", "组装单前缀", "string", "AO", "组装单号前缀")
    );

    private final SystemConfigMapper systemConfigMapper;

    public TenantSettingServiceImpl(SystemConfigMapper systemConfigMapper) {
        this.systemConfigMapper = systemConfigMapper;
    }

    @Override
    public TenantDisplaySettingsResponse getDisplaySettings() {
        Long tenantId = TenantContext.requireTenantId();
        SystemConfig config = systemConfigMapper.findByKey(tenantId, DEFAULT_PAGE_SIZE_KEY);
        Integer pageSize = parsePageSize(config == null ? null : config.getConfigValue());
        return new TenantDisplaySettingsResponse(
            pageSize == null ? FALLBACK_PAGE_SIZE : pageSize,
            config == null ? null : null,
            config == null ? null : config.getUpdatedAt()
        );
    }

    @Override
    public TenantDisplaySettingsResponse updateDisplaySettings(TenantDisplaySettingsUpdateRequest request) {
        Integer pageSize = sanitizePageSize(request == null ? null : request.defaultPageSize());
        if (pageSize == null) {
            throw new IllegalArgumentException("默认分页大小必须为 5 到 200 的整数");
        }
        Long tenantId = TenantContext.requireTenantId();
        SystemConfig config = systemConfigMapper.findByKey(tenantId, DEFAULT_PAGE_SIZE_KEY);
        if (config == null) {
            config = new SystemConfig();
            config.setTenantId(tenantId);
            config.setConfigKey(DEFAULT_PAGE_SIZE_KEY);
            config.setCreatedAt(Instant.now());
        }
        config.setConfigValue(String.valueOf(pageSize));
        config.setValueType("int");
        config.setDescription("默认分页大小");
        config.setPublic(false);
        config.setUpdatedAt(Instant.now());
        if (config.getId() == null) {
            systemConfigMapper.insert(config);
        } else {
            systemConfigMapper.update(
                config,
                new QueryWrapper<SystemConfig>().eq("tenant_id", tenantId).eq("config_key", DEFAULT_PAGE_SIZE_KEY)
            );
        }
        return new TenantDisplaySettingsResponse(pageSize, null, config.getUpdatedAt());
    }

    @Override
    public Integer getConfiguredDefaultPageSize() {
        Long tenantId = TenantContext.requireTenantId();
        SystemConfig config = systemConfigMapper.findByKey(tenantId, DEFAULT_PAGE_SIZE_KEY);
        return parsePageSize(config == null ? null : config.getConfigValue());
    }

    @Override
    public TenantBusinessSettingsResponse getBusinessSettings() {
        Long tenantId = TenantContext.requireTenantId();
        Map<String, SystemConfig> configMap = loadTenantConfigMap(tenantId);
        return new TenantBusinessSettingsResponse(
            toSettingItems(CODE_RULE_DEFINITIONS, configMap),
            toSettingItems(ORDER_RULE_DEFINITIONS, configMap)
        );
    }

    @Override
    public TenantBusinessSettingsResponse updateBusinessSettings(TenantBusinessSettingsUpdateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        Map<String, String> values = request == null || request.values() == null ? Map.of() : request.values();
        Map<String, SettingDefinition> definitionMap = buildDefinitionMap();
        values.forEach((key, value) -> {
            SettingDefinition definition = definitionMap.get(key);
            if (definition == null) {
                throw new IllegalArgumentException("不支持的租户业务配置项: " + key);
            }
            String sanitized = sanitizeConfigValue(definition, value);
            upsertTenantConfig(tenantId, definition, sanitized);
        });
        return getBusinessSettings();
    }

    public static Integer sanitizePageSize(Integer value) {
        if (value == null || value < MIN_PAGE_SIZE || value > MAX_PAGE_SIZE) {
            return null;
        }
        return value;
    }

    public static Integer parsePageSize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return sanitizePageSize(Integer.parseInt(value.trim()));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Map<String, SystemConfig> loadTenantConfigMap(Long tenantId) {
        Map<String, SystemConfig> configMap = new LinkedHashMap<>();
        for (SystemConfig config : systemConfigMapper.findAll(tenantId)) {
            configMap.put(config.getConfigKey(), config);
        }
        return configMap;
    }

    private List<TenantBusinessSettingItemResponse> toSettingItems(
        List<SettingDefinition> definitions,
        Map<String, SystemConfig> configMap
    ) {
        List<TenantBusinessSettingItemResponse> items = new ArrayList<>();
        for (SettingDefinition definition : definitions) {
            SystemConfig config = configMap.get(definition.key());
            items.add(new TenantBusinessSettingItemResponse(
                definition.key(),
                definition.label(),
                definition.valueType(),
                config == null || config.getConfigValue() == null || config.getConfigValue().isBlank()
                    ? definition.defaultValue()
                    : config.getConfigValue(),
                definition.defaultValue(),
                definition.description()
            ));
        }
        return items;
    }

    private Map<String, SettingDefinition> buildDefinitionMap() {
        Map<String, SettingDefinition> definitionMap = new LinkedHashMap<>();
        for (SettingDefinition definition : CODE_RULE_DEFINITIONS) {
            definitionMap.put(definition.key(), definition);
        }
        for (SettingDefinition definition : ORDER_RULE_DEFINITIONS) {
            definitionMap.put(definition.key(), definition);
        }
        return definitionMap;
    }

    private void upsertTenantConfig(Long tenantId, SettingDefinition definition, String value) {
        SystemConfig config = systemConfigMapper.findByKey(tenantId, definition.key());
        if (config == null) {
            config = new SystemConfig();
            config.setTenantId(tenantId);
            config.setConfigKey(definition.key());
            config.setCreatedAt(Instant.now());
        }
        config.setConfigValue(value);
        config.setValueType(definition.valueType());
        config.setDescription(definition.description());
        config.setPublic(false);
        config.setUpdatedAt(Instant.now());
        if (config.getId() == null) {
            systemConfigMapper.insert(config);
        } else {
            systemConfigMapper.update(
                config,
                new QueryWrapper<SystemConfig>().eq("tenant_id", tenantId).eq("config_key", definition.key())
            );
        }
    }

    private String sanitizeConfigValue(SettingDefinition definition, String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isBlank()) {
            throw new IllegalArgumentException(definition.label() + "不能为空");
        }
        if (definition.key().endsWith(".date-format")) {
            if (!ALLOWED_DATE_FORMATS.contains(trimmed)) {
                throw new IllegalArgumentException(definition.label() + "必须使用受支持的日期格式");
            }
            return trimmed;
        }
        if (definition.key().endsWith(".prefix")) {
            if (!trimmed.matches("^[A-Za-z0-9_-]+$")) {
                throw new IllegalArgumentException(definition.label() + "仅支持字母、数字、下划线和短横线");
            }
            if (trimmed.length() > MAX_PREFIX_LENGTH) {
                throw new IllegalArgumentException(definition.label() + "长度不能超过 " + MAX_PREFIX_LENGTH);
            }
            return trimmed.toUpperCase();
        }
        if ("int".equals(definition.valueType()) || INT_KEYS.contains(definition.key())) {
            try {
                int parsed = Integer.parseInt(trimmed);
                if (definition.key().endsWith(".seq-length")) {
                    if (parsed <= 0 || parsed > MAX_SEQUENCE_LENGTH) {
                        throw new IllegalArgumentException(definition.label() + "必须为 1 到 " + MAX_SEQUENCE_LENGTH + " 的整数");
                    }
                    return String.valueOf(parsed);
                }
                if (parsed <= 0 || parsed > 200) {
                    throw new IllegalArgumentException(definition.label() + "必须为 1 到 200 的整数");
                }
                return String.valueOf(parsed);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException(definition.label() + "必须为整数");
            }
        }
        if (trimmed.length() > 40) {
            throw new IllegalArgumentException(definition.label() + "长度不能超过 40");
        }
        return trimmed;
    }

    private static SettingDefinition definition(
        String key,
        String label,
        String valueType,
        String defaultValue,
        String description
    ) {
        return new SettingDefinition(key, label, valueType, defaultValue, description);
    }

    private record SettingDefinition(
        String key,
        String label,
        String valueType,
        String defaultValue,
        String description
    ) {
    }
}
