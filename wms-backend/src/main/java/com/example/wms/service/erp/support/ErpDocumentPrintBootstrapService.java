package com.example.wms.service.erp.support;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.wms.dto.erp.ErpPurchaseOrderPrintBootstrapData;
import com.example.wms.dto.erp.ErpPurchaseReturnPrintBootstrapData;
import com.example.wms.dto.erp.ErpSaleOrderPrintBootstrapData;
import com.example.wms.dto.erp.ErpSaleReturnPrintBootstrapData;
import com.example.wms.entity.erp.ErpCustomer;
import com.example.wms.entity.erp.ErpDeliveryMethod;
import com.example.wms.entity.erp.ErpLocation;
import com.example.wms.entity.erp.ErpPaymentMethod;
import com.example.wms.entity.erp.ErpSettlementMethod;
import com.example.wms.entity.erp.ErpSupplier;
import com.example.wms.entity.erp.ErpWarehouse;
import com.example.wms.mapper.erp.ErpCustomerMapper;
import com.example.wms.mapper.erp.ErpDeliveryMethodMapper;
import com.example.wms.mapper.erp.ErpLocationMapper;
import com.example.wms.mapper.erp.ErpPaymentMethodMapper;
import com.example.wms.mapper.erp.ErpSettlementMethodMapper;
import com.example.wms.mapper.erp.ErpSupplierMapper;
import com.example.wms.mapper.erp.ErpWarehouseMapper;
import com.example.wms.service.erp.ErpPurchaseOrderService;
import com.example.wms.service.erp.ErpPurchaseReturnService;
import com.example.wms.service.erp.ErpSaleOrderService;
import com.example.wms.service.erp.ErpSaleReturnService;
import com.example.wms.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ErpDocumentPrintBootstrapService {
    private final ErpSaleOrderService erpSaleOrderService;
    private final ErpPurchaseOrderService erpPurchaseOrderService;
    private final ErpSaleReturnService erpSaleReturnService;
    private final ErpPurchaseReturnService erpPurchaseReturnService;
    private final ErpCustomerMapper erpCustomerMapper;
    private final ErpSupplierMapper erpSupplierMapper;
    private final ErpWarehouseMapper erpWarehouseMapper;
    private final ErpLocationMapper erpLocationMapper;
    private final ErpSettlementMethodMapper erpSettlementMethodMapper;
    private final ErpDeliveryMethodMapper erpDeliveryMethodMapper;
    private final ErpPaymentMethodMapper erpPaymentMethodMapper;

    public ErpDocumentPrintBootstrapService(ErpSaleOrderService erpSaleOrderService,
                                            ErpPurchaseOrderService erpPurchaseOrderService,
                                            ErpSaleReturnService erpSaleReturnService,
                                            ErpPurchaseReturnService erpPurchaseReturnService,
                                            ErpCustomerMapper erpCustomerMapper,
                                            ErpSupplierMapper erpSupplierMapper,
                                            ErpWarehouseMapper erpWarehouseMapper,
                                            ErpLocationMapper erpLocationMapper,
                                            ErpSettlementMethodMapper erpSettlementMethodMapper,
                                            ErpDeliveryMethodMapper erpDeliveryMethodMapper,
                                            ErpPaymentMethodMapper erpPaymentMethodMapper) {
        this.erpSaleOrderService = erpSaleOrderService;
        this.erpPurchaseOrderService = erpPurchaseOrderService;
        this.erpSaleReturnService = erpSaleReturnService;
        this.erpPurchaseReturnService = erpPurchaseReturnService;
        this.erpCustomerMapper = erpCustomerMapper;
        this.erpSupplierMapper = erpSupplierMapper;
        this.erpWarehouseMapper = erpWarehouseMapper;
        this.erpLocationMapper = erpLocationMapper;
        this.erpSettlementMethodMapper = erpSettlementMethodMapper;
        this.erpDeliveryMethodMapper = erpDeliveryMethodMapper;
        this.erpPaymentMethodMapper = erpPaymentMethodMapper;
    }

    public ErpSaleOrderPrintBootstrapData getSaleOrderBootstrap(Long id, boolean approved) {
        Long tenantId = TenantContext.requireTenantId();
        return new ErpSaleOrderPrintBootstrapData(
            approved ? erpSaleOrderService.getApprovedDetail(id) : erpSaleOrderService.getDraftDetail(id),
            loadCustomers(tenantId),
            loadWarehouses(tenantId),
            loadLocations(tenantId),
            loadSettlementMethods(tenantId),
            loadDeliveryMethods(tenantId)
        );
    }

    public ErpPurchaseOrderPrintBootstrapData getPurchaseOrderBootstrap(Long id, boolean approved) {
        Long tenantId = TenantContext.requireTenantId();
        return new ErpPurchaseOrderPrintBootstrapData(
            approved ? erpPurchaseOrderService.getApprovedDetail(id) : erpPurchaseOrderService.getDraftDetail(id),
            loadSuppliers(tenantId),
            loadWarehouses(tenantId),
            loadLocations(tenantId),
            loadPaymentMethods(tenantId)
        );
    }

    public ErpSaleReturnPrintBootstrapData getSaleReturnBootstrap(Long id, boolean approved) {
        Long tenantId = TenantContext.requireTenantId();
        return new ErpSaleReturnPrintBootstrapData(
            approved ? erpSaleReturnService.getApprovedDetail(id) : erpSaleReturnService.getDraftDetail(id),
            loadCustomers(tenantId),
            loadWarehouses(tenantId),
            loadLocations(tenantId)
        );
    }

    public ErpPurchaseReturnPrintBootstrapData getPurchaseReturnBootstrap(Long id, boolean approved) {
        Long tenantId = TenantContext.requireTenantId();
        return new ErpPurchaseReturnPrintBootstrapData(
            approved ? erpPurchaseReturnService.getApprovedDetail(id) : erpPurchaseReturnService.getDraftDetail(id),
            loadSuppliers(tenantId),
            loadWarehouses(tenantId),
            loadLocations(tenantId)
        );
    }

    private List<ErpCustomer> loadCustomers(Long tenantId) {
        return erpCustomerMapper.selectList(new QueryWrapper<ErpCustomer>()
            .eq("tenant_id", tenantId)
            .orderByAsc("id"));
    }

    private List<ErpSupplier> loadSuppliers(Long tenantId) {
        return erpSupplierMapper.selectList(new QueryWrapper<ErpSupplier>()
            .eq("tenant_id", tenantId)
            .orderByAsc("id"));
    }

    private List<ErpWarehouse> loadWarehouses(Long tenantId) {
        return erpWarehouseMapper.selectList(new QueryWrapper<ErpWarehouse>()
            .eq("tenant_id", tenantId)
            .orderByAsc("id"));
    }

    private List<ErpLocation> loadLocations(Long tenantId) {
        return erpLocationMapper.selectList(new QueryWrapper<ErpLocation>()
            .eq("tenant_id", tenantId)
            .orderByAsc("id"));
    }

    private List<ErpSettlementMethod> loadSettlementMethods(Long tenantId) {
        return erpSettlementMethodMapper.selectList(new QueryWrapper<ErpSettlementMethod>()
            .eq("tenant_id", tenantId)
            .orderByDesc("is_default")
            .orderByAsc("sort_no", "id"));
    }

    private List<ErpDeliveryMethod> loadDeliveryMethods(Long tenantId) {
        return erpDeliveryMethodMapper.selectList(new QueryWrapper<ErpDeliveryMethod>()
            .eq("tenant_id", tenantId)
            .orderByDesc("is_default")
            .orderByAsc("sort_no", "id"));
    }

    private List<ErpPaymentMethod> loadPaymentMethods(Long tenantId) {
        return erpPaymentMethodMapper.selectList(new QueryWrapper<ErpPaymentMethod>()
            .eq("tenant_id", tenantId)
            .orderByDesc("is_default")
            .orderByAsc("sort_no", "id"));
    }
}
