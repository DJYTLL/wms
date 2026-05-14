package com.example.wms.service.erp.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.aop.AuditLog;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpSettlementMethodCreateRequest;
import com.example.wms.dto.erp.ErpSettlementMethodUpdateRequest;
import com.example.wms.entity.erp.ErpAccountsPayable;
import com.example.wms.entity.erp.ErpAccountsReceivable;
import com.example.wms.entity.erp.ErpCustomer;
import com.example.wms.entity.erp.ErpPayment;
import com.example.wms.entity.erp.ErpPurchaseReturn;
import com.example.wms.entity.erp.ErpReceipt;
import com.example.wms.entity.erp.ErpSaleOrder;
import com.example.wms.entity.erp.ErpSaleReturn;
import com.example.wms.entity.erp.ErpSettlementMethod;
import com.example.wms.entity.erp.ErpSupplier;
import com.example.wms.mapper.erp.ErpAccountsPayableMapper;
import com.example.wms.mapper.erp.ErpAccountsReceivableMapper;
import com.example.wms.mapper.erp.ErpCustomerMapper;
import com.example.wms.mapper.erp.ErpPaymentMapper;
import com.example.wms.mapper.erp.ErpPurchaseReturnMapper;
import com.example.wms.mapper.erp.ErpReceiptMapper;
import com.example.wms.mapper.erp.ErpSaleOrderMapper;
import com.example.wms.mapper.erp.ErpSaleReturnMapper;
import com.example.wms.mapper.erp.ErpSettlementMethodMapper;
import com.example.wms.mapper.erp.ErpSupplierMapper;
import com.example.wms.service.erp.ErpSettlementMethodService;
import com.example.wms.service.erp.support.ErpMasterDataCodeGenerator;
import com.example.wms.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

// 结算方式服务实现（ERP进销存）
@Service
public class ErpSettlementMethodServiceImpl implements ErpSettlementMethodService {
    private static final String SETTLEMENT_METHOD_CODE_TYPE = "SETTLEMENT_METHOD";

    private final ErpSettlementMethodMapper erpSettlementMethodMapper;
    private final ErpCustomerMapper erpCustomerMapper;
    private final ErpSupplierMapper erpSupplierMapper;
    private final ErpSaleOrderMapper erpSaleOrderMapper;
    private final ErpSaleReturnMapper erpSaleReturnMapper;
    private final ErpPurchaseReturnMapper erpPurchaseReturnMapper;
    private final ErpReceiptMapper erpReceiptMapper;
    private final ErpPaymentMapper erpPaymentMapper;
    private final ErpAccountsReceivableMapper erpAccountsReceivableMapper;
    private final ErpAccountsPayableMapper erpAccountsPayableMapper;
    private final ErpMasterDataCodeGenerator codeGenerator;

    public ErpSettlementMethodServiceImpl(ErpSettlementMethodMapper erpSettlementMethodMapper,
                                          ErpCustomerMapper erpCustomerMapper,
                                          ErpSupplierMapper erpSupplierMapper,
                                          ErpSaleOrderMapper erpSaleOrderMapper,
                                          ErpSaleReturnMapper erpSaleReturnMapper,
                                          ErpPurchaseReturnMapper erpPurchaseReturnMapper,
                                          ErpReceiptMapper erpReceiptMapper,
                                          ErpPaymentMapper erpPaymentMapper,
                                          ErpAccountsReceivableMapper erpAccountsReceivableMapper,
                                          ErpAccountsPayableMapper erpAccountsPayableMapper,
                                          ErpMasterDataCodeGenerator codeGenerator) {
        this.erpSettlementMethodMapper = erpSettlementMethodMapper;
        this.erpCustomerMapper = erpCustomerMapper;
        this.erpSupplierMapper = erpSupplierMapper;
        this.erpSaleOrderMapper = erpSaleOrderMapper;
        this.erpSaleReturnMapper = erpSaleReturnMapper;
        this.erpPurchaseReturnMapper = erpPurchaseReturnMapper;
        this.erpReceiptMapper = erpReceiptMapper;
        this.erpPaymentMapper = erpPaymentMapper;
        this.erpAccountsReceivableMapper = erpAccountsReceivableMapper;
        this.erpAccountsPayableMapper = erpAccountsPayableMapper;
        this.codeGenerator = codeGenerator;
    }

    @Override
    public List<ErpSettlementMethod> listAll(String keyword, Boolean enabled) {
        QueryWrapper<ErpSettlementMethod> wrapper = baseWrapper(keyword, enabled);
        wrapper.orderByDesc("is_default").orderByAsc("sort_no", "id");
        return erpSettlementMethodMapper.selectList(wrapper);
    }

    @Override
    public PageResponse<ErpSettlementMethod> page(long page, long size, String keyword, Boolean enabled) {
        Page<ErpSettlementMethod> pageReq = Page.of(page, size);
        QueryWrapper<ErpSettlementMethod> wrapper = baseWrapper(keyword, enabled);
        wrapper.orderByDesc("is_default").orderByAsc("sort_no", "id");
        Page<ErpSettlementMethod> result = erpSettlementMethodMapper.selectPage(pageReq, wrapper);
        return new PageResponse<>(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override
    public ErpSettlementMethod getById(Long id) {
        ErpSettlementMethod method = erpSettlementMethodMapper.selectOne(new QueryWrapper<ErpSettlementMethod>()
            .eq("tenant_id", TenantContext.requireTenantId())
            .eq("id", id));
        if (method == null) {
            throw new IllegalArgumentException("结算方式不存在");
        }
        return method;
    }

    @Override
    public String nextCode() {
        return codeGenerator.nextCode(
            SETTLEMENT_METHOD_CODE_TYPE,
            "erp.settlement-method.code.prefix",
            "SM",
            "erp.settlement-method.code.date-format",
            "erp.settlement-method.code.seq-length"
        );
    }

    @Override
    @AuditLog(action = "ERP_SETTLEMENT_METHOD_CREATE", entityType = "erp_settlement_method", entityId = "{result.id}", detail = "code={arg0.code}")
    public ErpSettlementMethod create(ErpSettlementMethodCreateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        ErpSettlementMethod existing = erpSettlementMethodMapper.findByCode(tenantId, request.code());
        if (existing != null) {
            throw new IllegalArgumentException("结算方式编码已存在");
        }
        ErpSettlementMethod method = new ErpSettlementMethod();
        method.setTenantId(tenantId);
        applyRequest(method, request);
        method.setEnabled(request.enabled() == null || request.enabled());
        applyDefaultFlag(tenantId, method, request.isDefault());
        method.setCreatedAt(Instant.now());
        method.setUpdatedAt(Instant.now());
        erpSettlementMethodMapper.insert(method);
        return method;
    }

    @Override
    @AuditLog(action = "ERP_SETTLEMENT_METHOD_UPDATE", entityType = "erp_settlement_method", entityId = "{arg0}", detail = "code={arg1.code}")
    public ErpSettlementMethod update(Long id, ErpSettlementMethodUpdateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        ErpSettlementMethod method = erpSettlementMethodMapper.selectOne(new QueryWrapper<ErpSettlementMethod>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (method == null) {
            throw new IllegalArgumentException("结算方式不存在");
        }
        ErpSettlementMethod existing = erpSettlementMethodMapper.findByCode(tenantId, request.code());
        if (existing != null && !existing.getId().equals(id)) {
            throw new IllegalArgumentException("结算方式编码已存在");
        }
        applyRequest(method, request);
        if (request.enabled() != null) {
            method.setEnabled(request.enabled());
        }
        applyDefaultFlag(tenantId, method, request.isDefault());
        method.setUpdatedAt(Instant.now());
        erpSettlementMethodMapper.updateById(method);
        return method;
    }

    @Override
    @AuditLog(action = "ERP_SETTLEMENT_METHOD_DELETE", entityType = "erp_settlement_method", entityId = "{arg0}")
    public void delete(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        ErpSettlementMethod method = erpSettlementMethodMapper.selectOne(new QueryWrapper<ErpSettlementMethod>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (method == null) {
            throw new IllegalArgumentException("结算方式不存在");
        }
        ensureSettlementMethodNotReferenced(tenantId, method.getCode());
        erpSettlementMethodMapper.deleteById(id);
    }

    private void ensureSettlementMethodNotReferenced(Long tenantId, String code) {
        if (erpCustomerMapper.selectCount(new QueryWrapper<ErpCustomer>()
            .eq("tenant_id", tenantId)
            .eq("default_settlement_method_code", code)) > 0) {
            throw new IllegalArgumentException("结算方式已被客户引用，不能删除");
        }
        if (erpSupplierMapper.selectCount(new QueryWrapper<ErpSupplier>()
            .eq("tenant_id", tenantId)
            .eq("default_settlement_method_code", code)) > 0) {
            throw new IllegalArgumentException("结算方式已被供应商引用，不能删除");
        }
        if (erpSaleOrderMapper.selectCount(new QueryWrapper<ErpSaleOrder>()
            .eq("tenant_id", tenantId)
            .eq("settlement_method", code)) > 0) {
            throw new IllegalArgumentException("结算方式已被销售单引用，不能删除");
        }
        if (erpSaleReturnMapper.selectCount(new QueryWrapper<ErpSaleReturn>()
            .eq("tenant_id", tenantId)
            .eq("settlement_method", code)) > 0) {
            throw new IllegalArgumentException("结算方式已被销售退货单引用，不能删除");
        }
        if (erpPurchaseReturnMapper.selectCount(new QueryWrapper<ErpPurchaseReturn>()
            .eq("tenant_id", tenantId)
            .eq("settlement_method", code)) > 0) {
            throw new IllegalArgumentException("结算方式已被采购退货单引用，不能删除");
        }
        if (erpReceiptMapper.selectCount(new QueryWrapper<ErpReceipt>()
            .eq("tenant_id", tenantId)
            .eq("settlement_method", code)) > 0) {
            throw new IllegalArgumentException("结算方式已被收款单引用，不能删除");
        }
        if (erpPaymentMapper.selectCount(new QueryWrapper<ErpPayment>()
            .eq("tenant_id", tenantId)
            .eq("settlement_method", code)) > 0) {
            throw new IllegalArgumentException("结算方式已被付款单引用，不能删除");
        }
        if (erpAccountsReceivableMapper.selectCount(new QueryWrapper<ErpAccountsReceivable>()
            .eq("tenant_id", tenantId)
            .eq("settlement_method", code)) > 0) {
            throw new IllegalArgumentException("结算方式已被应收单引用，不能删除");
        }
        if (erpAccountsPayableMapper.selectCount(new QueryWrapper<ErpAccountsPayable>()
            .eq("tenant_id", tenantId)
            .eq("settlement_method", code)) > 0) {
            throw new IllegalArgumentException("结算方式已被应付单引用，不能删除");
        }
    }

    private QueryWrapper<ErpSettlementMethod> baseWrapper(String keyword, Boolean enabled) {
        QueryWrapper<ErpSettlementMethod> wrapper = new QueryWrapper<ErpSettlementMethod>()
            .eq("tenant_id", TenantContext.requireTenantId());
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(q -> q.like("code", keyword)
                .or()
                .like("name", keyword));
        }
        if (enabled != null) {
            wrapper.eq("is_enabled", enabled);
        }
        return wrapper;
    }

    private void applyRequest(ErpSettlementMethod method, ErpSettlementMethodCreateRequest request) {
        method.setCode(request.code());
        method.setName(request.name());
        method.setSortNo(request.sortNo() == null ? 0 : request.sortNo());
        method.setRemark(request.remark());
    }

    private void applyRequest(ErpSettlementMethod method, ErpSettlementMethodUpdateRequest request) {
        method.setCode(request.code());
        method.setName(request.name());
        method.setSortNo(request.sortNo() == null ? 0 : request.sortNo());
        method.setRemark(request.remark());
    }

    private void applyDefaultFlag(Long tenantId, ErpSettlementMethod method, Boolean isDefault) {
        if (isDefault == null) {
            return;
        }
        if (Boolean.TRUE.equals(isDefault)) {
            erpSettlementMethodMapper.update(null, new UpdateWrapper<ErpSettlementMethod>()
                .eq("tenant_id", tenantId)
                .set("is_default", false));
            method.setIsDefault(true);
        } else {
            method.setIsDefault(false);
        }
    }
}
