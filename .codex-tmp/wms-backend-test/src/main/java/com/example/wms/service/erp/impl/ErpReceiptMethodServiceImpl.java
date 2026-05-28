package com.example.wms.service.erp.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.aop.AuditLog;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpPaymentMethodCreateRequest;
import com.example.wms.dto.erp.ErpPaymentMethodUpdateRequest;
import com.example.wms.entity.erp.ErpCustomer;
import com.example.wms.entity.erp.ErpReceipt;
import com.example.wms.entity.erp.ErpReceiptMethod;
import com.example.wms.entity.erp.ErpSaleOrder;
import com.example.wms.entity.erp.ErpSaleReturn;
import com.example.wms.mapper.erp.ErpCustomerMapper;
import com.example.wms.mapper.erp.ErpReceiptMapper;
import com.example.wms.mapper.erp.ErpReceiptMethodMapper;
import com.example.wms.mapper.erp.ErpSaleOrderMapper;
import com.example.wms.mapper.erp.ErpSaleReturnMapper;
import com.example.wms.service.erp.ErpReceiptMethodService;
import com.example.wms.service.erp.support.ErpMasterDataCodeGenerator;
import com.example.wms.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

// 收款方式服务实现（ERP进销存）
@Service
public class ErpReceiptMethodServiceImpl implements ErpReceiptMethodService {
    private static final String RECEIPT_METHOD_CODE_TYPE = "RECEIPT_METHOD";

    private final ErpReceiptMethodMapper erpReceiptMethodMapper;
    private final ErpCustomerMapper erpCustomerMapper;
    private final ErpSaleOrderMapper erpSaleOrderMapper;
    private final ErpSaleReturnMapper erpSaleReturnMapper;
    private final ErpReceiptMapper erpReceiptMapper;
    private final ErpMasterDataCodeGenerator codeGenerator;

    public ErpReceiptMethodServiceImpl(ErpReceiptMethodMapper erpReceiptMethodMapper,
                                       ErpCustomerMapper erpCustomerMapper,
                                       ErpSaleOrderMapper erpSaleOrderMapper,
                                       ErpSaleReturnMapper erpSaleReturnMapper,
                                       ErpReceiptMapper erpReceiptMapper,
                                       ErpMasterDataCodeGenerator codeGenerator) {
        this.erpReceiptMethodMapper = erpReceiptMethodMapper;
        this.erpCustomerMapper = erpCustomerMapper;
        this.erpSaleOrderMapper = erpSaleOrderMapper;
        this.erpSaleReturnMapper = erpSaleReturnMapper;
        this.erpReceiptMapper = erpReceiptMapper;
        this.codeGenerator = codeGenerator;
    }

    @Override
    public List<ErpReceiptMethod> listAll(String keyword, Boolean enabled) {
        QueryWrapper<ErpReceiptMethod> wrapper = baseWrapper(keyword, enabled);
        wrapper.orderByDesc("is_default").orderByAsc("sort_no", "id");
        return erpReceiptMethodMapper.selectList(wrapper);
    }

    @Override
    public PageResponse<ErpReceiptMethod> page(long page, long size, String keyword, Boolean enabled) {
        Page<ErpReceiptMethod> pageReq = Page.of(page, size);
        QueryWrapper<ErpReceiptMethod> wrapper = baseWrapper(keyword, enabled);
        wrapper.orderByDesc("is_default").orderByAsc("sort_no", "id");
        Page<ErpReceiptMethod> result = erpReceiptMethodMapper.selectPage(pageReq, wrapper);
        return new PageResponse<>(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override
    public ErpReceiptMethod getById(Long id) {
        ErpReceiptMethod method = erpReceiptMethodMapper.selectOne(new QueryWrapper<ErpReceiptMethod>()
            .eq("tenant_id", TenantContext.requireTenantId())
            .eq("id", id));
        if (method == null) {
            throw new IllegalArgumentException("收款方式不存在");
        }
        return method;
    }

    @Override
    public String nextCode() {
        return codeGenerator.nextCode(
            RECEIPT_METHOD_CODE_TYPE,
            "erp.receipt-method.code.prefix",
            "RM",
            "erp.receipt-method.code.date-format",
            "erp.receipt-method.code.seq-length"
        );
    }

    @Override
    @AuditLog(action = "ERP_RECEIPT_METHOD_CREATE", entityType = "erp_receipt_method", entityId = "{result.id}", detail = "code={arg0.code}")
@Transactional
    public ErpReceiptMethod create(ErpPaymentMethodCreateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        ErpReceiptMethod existing = erpReceiptMethodMapper.findByCode(tenantId, request.code());
        if (existing != null) {
            throw new IllegalArgumentException("收款方式编码已存在");
        }
        ErpReceiptMethod method = new ErpReceiptMethod();
        method.setTenantId(tenantId);
        applyRequest(method, request);
        method.setEnabled(request.enabled() == null || request.enabled());
        applyDefaultFlag(tenantId, method, request.isDefault());
        method.setCreatedAt(Instant.now());
        method.setUpdatedAt(Instant.now());
        erpReceiptMethodMapper.insert(method);
        return method;
    }

    @Override
    @AuditLog(action = "ERP_RECEIPT_METHOD_UPDATE", entityType = "erp_receipt_method", entityId = "{arg0}", detail = "code={arg1.code}")
@Transactional
    public ErpReceiptMethod update(Long id, ErpPaymentMethodUpdateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        ErpReceiptMethod method = erpReceiptMethodMapper.selectOne(new QueryWrapper<ErpReceiptMethod>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (method == null) {
            throw new IllegalArgumentException("收款方式不存在");
        }
        ErpReceiptMethod existing = erpReceiptMethodMapper.findByCode(tenantId, request.code());
        if (existing != null && !existing.getId().equals(id)) {
            throw new IllegalArgumentException("收款方式编码已存在");
        }
        applyRequest(method, request);
        if (request.enabled() != null) {
            method.setEnabled(request.enabled());
        }
        applyDefaultFlag(tenantId, method, request.isDefault());
        method.setUpdatedAt(Instant.now());
        erpReceiptMethodMapper.updateById(method);
        return method;
    }

    @Override
    @AuditLog(action = "ERP_RECEIPT_METHOD_DELETE", entityType = "erp_receipt_method", entityId = "{arg0}")
@Transactional
    public void delete(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        ErpReceiptMethod method = erpReceiptMethodMapper.selectOne(new QueryWrapper<ErpReceiptMethod>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (method == null) {
            throw new IllegalArgumentException("收款方式不存在");
        }
        ensureReceiptMethodNotReferenced(tenantId, method.getCode());
        erpReceiptMethodMapper.deleteById(id);
    }

    private QueryWrapper<ErpReceiptMethod> baseWrapper(String keyword, Boolean enabled) {
        QueryWrapper<ErpReceiptMethod> wrapper = new QueryWrapper<ErpReceiptMethod>()
            .eq("tenant_id", TenantContext.requireTenantId());
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(q -> q.like("code", keyword).or().like("name", keyword));
        }
        if (enabled != null) {
            wrapper.eq("is_enabled", enabled);
        }
        return wrapper;
    }

    private void applyRequest(ErpReceiptMethod method, ErpPaymentMethodCreateRequest request) {
        method.setCode(request.code());
        method.setName(request.name());
        method.setSortNo(request.sortNo() == null ? 0 : request.sortNo());
        method.setRemark(request.remark());
    }

    private void applyRequest(ErpReceiptMethod method, ErpPaymentMethodUpdateRequest request) {
        method.setCode(request.code());
        method.setName(request.name());
        method.setSortNo(request.sortNo() == null ? 0 : request.sortNo());
        method.setRemark(request.remark());
    }

    private void applyDefaultFlag(Long tenantId, ErpReceiptMethod method, Boolean isDefault) {
        if (isDefault == null) {
            return;
        }
        if (Boolean.TRUE.equals(isDefault)) {
            erpReceiptMethodMapper.update(null, new UpdateWrapper<ErpReceiptMethod>()
                .eq("tenant_id", tenantId)
                .set("is_default", false));
            method.setIsDefault(true);
        } else {
            method.setIsDefault(false);
        }
    }

    private void ensureReceiptMethodNotReferenced(Long tenantId, String code) {
        if (erpCustomerMapper.selectCount(new QueryWrapper<ErpCustomer>()
            .eq("tenant_id", tenantId)
            .eq("default_receipt_method_code", code)) > 0) {
            throw new IllegalArgumentException("收款方式已被客户引用，不能删除");
        }
        if (erpSaleOrderMapper.selectCount(new QueryWrapper<ErpSaleOrder>()
            .eq("tenant_id", tenantId)
            .eq("receipt_method_code", code)) > 0) {
            throw new IllegalArgumentException("收款方式已被销售单引用，不能删除");
        }
        if (erpSaleReturnMapper.selectCount(new QueryWrapper<ErpSaleReturn>()
            .eq("tenant_id", tenantId)
            .eq("receipt_method_code", code)) > 0) {
            throw new IllegalArgumentException("收款方式已被销售退货单引用，不能删除");
        }
        if (erpReceiptMapper.selectCount(new QueryWrapper<ErpReceipt>()
            .eq("tenant_id", tenantId)
            .eq("receipt_method_code", code)) > 0) {
            throw new IllegalArgumentException("收款方式已被收款单引用，不能删除");
        }
    }
}
