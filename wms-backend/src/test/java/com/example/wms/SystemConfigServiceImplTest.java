package com.example.wms;

import com.example.wms.dto.SystemConfigRequest;
import com.example.wms.dto.SystemConfigResponse;
import com.example.wms.entity.SystemConfig;
import com.example.wms.mapper.SystemConfigMapper;
import com.example.wms.service.impl.SystemConfigServiceImpl;
import com.example.wms.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemConfigServiceImplTest {
    @Mock private SystemConfigMapper systemConfigMapper;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createRejectsTenantManagedErpKey() {
        TenantContext.setTenantId(1L);

        SystemConfigServiceImpl service = new SystemConfigServiceImpl(systemConfigMapper);
        assertThatThrownBy(() -> service.create(
            "erp.order.no.sale.prefix",
            new SystemConfigRequest("A-SO", "string", "销售单号前缀", false)
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessage("ERP编码规则和单号规则请在租户设置中维护");
    }

    @Test
    void createRejectsUnsupportedPlatformKey() {
        TenantContext.setTenantId(1L);

        SystemConfigServiceImpl service = new SystemConfigServiceImpl(systemConfigMapper);
        assertThatThrownBy(() -> service.create(
            "feature.toggle.experimental",
            new SystemConfigRequest("true", "bool", "实验开关", false)
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessage("该配置项不属于平台配置，请在对应设置域维护");
    }

    @Test
    void createAllowsSqlTimingPlatformKey() {
        TenantContext.setTenantId(1L);

        SystemConfigServiceImpl service = new SystemConfigServiceImpl(systemConfigMapper);
        when(systemConfigMapper.findByKey(1L, "wms.monitor.sql-timing-enabled")).thenReturn(null);

        SystemConfigResponse response = service.create(
            "wms.monitor.sql-timing-enabled",
            new SystemConfigRequest("true", "bool", "SQL耗时采集开关", false)
        );

        assertThat(response.key()).isEqualTo("wms.monitor.sql-timing-enabled");
        assertThat(response.value()).isEqualTo("true");
        assertThat(response.valueType()).isEqualTo("bool");
    }

    @Test
    void listAllExcludesTenantManagedPageSizeAndErpConfigs() {
        TenantContext.setTenantId(1L);
        when(systemConfigMapper.findAll(1L)).thenReturn(List.of(
            config("default.page.size", "20", true),
            config("erp.order.no.sale.prefix", "SO", false),
            config("audit.retention.days", "180", false)
        ));

        SystemConfigServiceImpl service = new SystemConfigServiceImpl(systemConfigMapper);
        List<SystemConfigResponse> result = service.listAll();

        assertThat(result).extracting(SystemConfigResponse::key)
            .containsExactly("audit.retention.days");
    }

    @Test
    void updateRejectsTenantManagedPageSizeConfig() {
        TenantContext.setTenantId(1L);

        SystemConfigServiceImpl service = new SystemConfigServiceImpl(systemConfigMapper);

        assertThatThrownBy(() -> service.update(
            "default.page.size",
            new SystemConfigRequest("50", "int", "默认分页大小", false)
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessage("默认分页大小请在租户设置中维护");
    }

    @Test
    void updateRejectsTenantManagedErpKey() {
        TenantContext.setTenantId(1L);

        SystemConfigServiceImpl service = new SystemConfigServiceImpl(systemConfigMapper);

        assertThatThrownBy(() -> service.update(
            "erp.product.code.prefix",
            new SystemConfigRequest("PR", "string", "商品编码前缀", false)
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessage("ERP编码规则和单号规则请在租户设置中维护");
    }

    @Test
    void updateRejectsUnsupportedPlatformKey() {
        TenantContext.setTenantId(1L);

        SystemConfigServiceImpl service = new SystemConfigServiceImpl(systemConfigMapper);

        assertThatThrownBy(() -> service.update(
            "feature.toggle.experimental",
            new SystemConfigRequest("true", "bool", "实验开关", false)
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessage("该配置项不属于平台配置，请在对应设置域维护");
    }

    private SystemConfig config(String key, String value, boolean isPublic) {
        SystemConfig config = new SystemConfig();
        config.setTenantId(1L);
        config.setConfigKey(key);
        config.setConfigValue(value);
        config.setValueType("int");
        config.setDescription(key);
        config.setPublic(isPublic);
        return config;
    }
}
