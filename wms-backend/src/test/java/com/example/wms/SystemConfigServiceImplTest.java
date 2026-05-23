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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemConfigServiceImplTest {
    @Mock private SystemConfigMapper systemConfigMapper;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createAllowsSameKeyInDifferentTenants() {
        TenantContext.setTenantId(1L);
        when(systemConfigMapper.findByKey(1L, "erp.order.no.sale.prefix")).thenReturn(null);

        SystemConfigServiceImpl service = new SystemConfigServiceImpl(systemConfigMapper);
        service.create("erp.order.no.sale.prefix", new SystemConfigRequest("A-SO", "string", "销售单号前缀", false));

        ArgumentCaptor<SystemConfig> captor = ArgumentCaptor.forClass(SystemConfig.class);
        verify(systemConfigMapper).insert(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(1L);
        assertThat(captor.getValue().getConfigKey()).isEqualTo("erp.order.no.sale.prefix");
    }

    @Test
    void listAllExcludesTenantManagedPageSizeConfig() {
        TenantContext.setTenantId(1L);
        when(systemConfigMapper.findAll(1L)).thenReturn(List.of(
            config("default.page.size", "20", true),
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
