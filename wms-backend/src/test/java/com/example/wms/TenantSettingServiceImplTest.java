package com.example.wms;

import com.example.wms.dto.TenantDisplaySettingsResponse;
import com.example.wms.dto.TenantDisplaySettingsUpdateRequest;
import com.example.wms.entity.SystemConfig;
import com.example.wms.mapper.SystemConfigMapper;
import com.example.wms.service.impl.TenantSettingServiceImpl;
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
class TenantSettingServiceImplTest {
    @Mock private SystemConfigMapper systemConfigMapper;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void getDisplaySettingsFallsBackToBuiltinPageSize() {
        TenantContext.setTenantId(9L);
        when(systemConfigMapper.findByKey(9L, "default.page.size")).thenReturn(null);

        TenantSettingServiceImpl service = new TenantSettingServiceImpl(systemConfigMapper);
        TenantDisplaySettingsResponse result = service.getDisplaySettings();

        assertThat(result.defaultPageSize()).isEqualTo(20);
    }

    @Test
    void updateDisplaySettingsCreatesTenantScopedConfig() {
        TenantContext.setTenantId(9L);
        when(systemConfigMapper.findByKey(9L, "default.page.size")).thenReturn(null);

        TenantSettingServiceImpl service = new TenantSettingServiceImpl(systemConfigMapper);
        service.updateDisplaySettings(new TenantDisplaySettingsUpdateRequest(50));

        ArgumentCaptor<SystemConfig> captor = ArgumentCaptor.forClass(SystemConfig.class);
        verify(systemConfigMapper).insert(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(9L);
        assertThat(captor.getValue().getConfigKey()).isEqualTo("default.page.size");
        assertThat(captor.getValue().getConfigValue()).isEqualTo("50");
        assertThat(captor.getValue().isPublic()).isFalse();
    }
}
