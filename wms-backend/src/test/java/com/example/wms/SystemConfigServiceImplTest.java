package com.example.wms;

import com.example.wms.dto.SystemConfigRequest;
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

import static org.assertj.core.api.Assertions.assertThat;
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
}
