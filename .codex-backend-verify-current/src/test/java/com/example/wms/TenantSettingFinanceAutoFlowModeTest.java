package com.example.wms;

import com.example.wms.entity.SystemConfig;
import com.example.wms.mapper.SystemConfigMapper;
import com.example.wms.service.erp.support.FinanceAutoFlowMode;
import com.example.wms.service.impl.TenantSettingServiceImpl;
import com.example.wms.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantSettingFinanceAutoFlowModeTest {
    @Mock
    private SystemConfigMapper systemConfigMapper;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void getFinanceAutoFlowModeFallsBackToApprovedPaymentWhenUnset() {
        TenantContext.setTenantId(9L);
        when(systemConfigMapper.findByKey(9L, "erp.finance.auto-flow.mode")).thenReturn(null);

        TenantSettingServiceImpl service = new TenantSettingServiceImpl(systemConfigMapper);
        FinanceAutoFlowMode mode = service.getFinanceAutoFlowMode();

        assertThat(mode).isEqualTo(FinanceAutoFlowMode.AR_AP_WITH_APPROVED_PAYMENT);
    }

    @Test
    void getFinanceAutoFlowModeReadsTenantScopedValue() {
        TenantContext.setTenantId(9L);
        SystemConfig config = new SystemConfig();
        config.setConfigKey("erp.finance.auto-flow.mode");
        config.setConfigValue("AR_AP_ONLY");
        when(systemConfigMapper.findByKey(9L, "erp.finance.auto-flow.mode")).thenReturn(config);

        TenantSettingServiceImpl service = new TenantSettingServiceImpl(systemConfigMapper);
        FinanceAutoFlowMode mode = service.getFinanceAutoFlowMode();

        assertThat(mode).isEqualTo(FinanceAutoFlowMode.AR_AP_ONLY);
    }

    @Test
    void getFinanceAutoFlowModeRejectsUnknownValue() {
        TenantContext.setTenantId(9L);
        SystemConfig config = new SystemConfig();
        config.setConfigKey("erp.finance.auto-flow.mode");
        config.setConfigValue("INVALID_MODE");
        when(systemConfigMapper.findByKey(9L, "erp.finance.auto-flow.mode")).thenReturn(config);

        TenantSettingServiceImpl service = new TenantSettingServiceImpl(systemConfigMapper);

        assertThatThrownBy(service::getFinanceAutoFlowMode)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("erp.finance.auto-flow.mode");
    }
}
