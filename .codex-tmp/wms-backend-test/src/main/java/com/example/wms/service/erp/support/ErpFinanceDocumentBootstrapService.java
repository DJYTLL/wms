package com.example.wms.service.erp.support;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.wms.dto.erp.ErpPaymentBootstrapData;
import com.example.wms.dto.erp.ErpReceiptBootstrapData;
import com.example.wms.entity.erp.ErpCustomer;
import com.example.wms.entity.erp.ErpPaymentMethod;
import com.example.wms.entity.erp.ErpReceiptMethod;
import com.example.wms.entity.erp.ErpSettlementMethod;
import com.example.wms.entity.erp.ErpSupplier;
import com.example.wms.mapper.erp.ErpCustomerMapper;
import com.example.wms.mapper.erp.ErpPaymentMethodMapper;
import com.example.wms.mapper.erp.ErpReceiptMethodMapper;
import com.example.wms.mapper.erp.ErpSettlementMethodMapper;
import com.example.wms.mapper.erp.ErpSupplierMapper;
import com.example.wms.service.erp.ErpPaymentService;
import com.example.wms.service.erp.ErpReceiptService;
import com.example.wms.tenant.TenantContext;
import org.springframework.stereotype.Service;

@Service
public class ErpFinanceDocumentBootstrapService {
    private final ErpReceiptService erpReceiptService;
    private final ErpPaymentService erpPaymentService;
    private final ErpCustomerMapper erpCustomerMapper;
    private final ErpSupplierMapper erpSupplierMapper;
    private final ErpSettlementMethodMapper erpSettlementMethodMapper;
    private final ErpReceiptMethodMapper erpReceiptMethodMapper;
    private final ErpPaymentMethodMapper erpPaymentMethodMapper;

    public ErpFinanceDocumentBootstrapService(ErpReceiptService erpReceiptService,
                                              ErpPaymentService erpPaymentService,
                                              ErpCustomerMapper erpCustomerMapper,
                                              ErpSupplierMapper erpSupplierMapper,
                                              ErpSettlementMethodMapper erpSettlementMethodMapper,
                                              ErpReceiptMethodMapper erpReceiptMethodMapper,
                                              ErpPaymentMethodMapper erpPaymentMethodMapper) {
        this.erpReceiptService = erpReceiptService;
        this.erpPaymentService = erpPaymentService;
        this.erpCustomerMapper = erpCustomerMapper;
        this.erpSupplierMapper = erpSupplierMapper;
        this.erpSettlementMethodMapper = erpSettlementMethodMapper;
        this.erpReceiptMethodMapper = erpReceiptMethodMapper;
        this.erpPaymentMethodMapper = erpPaymentMethodMapper;
    }

    public ErpReceiptBootstrapData getReceiptBootstrapData() {
        Long tenantId = TenantContext.requireTenantId();
        return new ErpReceiptBootstrapData(
            erpReceiptService.nextReceiptNo(),
            erpCustomerMapper.selectList(new QueryWrapper<ErpCustomer>()
                .eq("tenant_id", tenantId)
                .orderByAsc("id")),
            erpSettlementMethodMapper.selectList(new QueryWrapper<ErpSettlementMethod>()
                .eq("tenant_id", tenantId)
                .orderByDesc("is_default")
                .orderByAsc("sort_no", "id")),
            erpReceiptMethodMapper.selectList(new QueryWrapper<ErpReceiptMethod>()
                .eq("tenant_id", tenantId)
                .orderByDesc("is_default")
                .orderByAsc("sort_no", "id"))
        );
    }

    public ErpPaymentBootstrapData getPaymentBootstrapData() {
        Long tenantId = TenantContext.requireTenantId();
        return new ErpPaymentBootstrapData(
            erpPaymentService.nextPaymentNo(),
            erpSupplierMapper.selectList(new QueryWrapper<ErpSupplier>()
                .eq("tenant_id", tenantId)
                .orderByAsc("id")),
            erpSettlementMethodMapper.selectList(new QueryWrapper<ErpSettlementMethod>()
                .eq("tenant_id", tenantId)
                .orderByDesc("is_default")
                .orderByAsc("sort_no", "id")),
            erpPaymentMethodMapper.selectList(new QueryWrapper<ErpPaymentMethod>()
                .eq("tenant_id", tenantId)
                .orderByDesc("is_default")
                .orderByAsc("sort_no", "id"))
        );
    }
}
