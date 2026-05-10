package com.example.wms.service.erp.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpAccountsPayableDetail;
import com.example.wms.dto.erp.ErpAccountsPayableView;
import com.example.wms.dto.erp.ErpPaymentView;
import com.example.wms.entity.erp.ErpAccountsPayable;
import com.example.wms.entity.erp.ErpPayment;
import com.example.wms.entity.erp.ErpPaymentPayable;
import com.example.wms.entity.erp.ErpSupplier;
import com.example.wms.mapper.erp.ErpAccountsPayableMapper;
import com.example.wms.mapper.erp.ErpPaymentMapper;
import com.example.wms.mapper.erp.ErpPaymentPayableMapper;
import com.example.wms.mapper.erp.ErpSupplierMapper;
import com.example.wms.service.erp.ErpAccountsPayableService;
import com.example.wms.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

// ERP应付单服务实现
@Service
public class ErpAccountsPayableServiceImpl implements ErpAccountsPayableService {
    private final ErpAccountsPayableMapper erpAccountsPayableMapper;
    private final ErpSupplierMapper erpSupplierMapper;
    private final ErpPaymentMapper erpPaymentMapper;
    private final ErpPaymentPayableMapper erpPaymentPayableMapper;

    public ErpAccountsPayableServiceImpl(ErpAccountsPayableMapper erpAccountsPayableMapper,
                                         ErpSupplierMapper erpSupplierMapper,
                                         ErpPaymentMapper erpPaymentMapper,
                                         ErpPaymentPayableMapper erpPaymentPayableMapper) {
        this.erpAccountsPayableMapper = erpAccountsPayableMapper;
        this.erpSupplierMapper = erpSupplierMapper;
        this.erpPaymentMapper = erpPaymentMapper;
        this.erpPaymentPayableMapper = erpPaymentPayableMapper;
    }

    @Override
    public List<ErpAccountsPayableView> listAll(String keyword, String status, Long supplierId, Instant startAt, Instant endAt) {
        QueryWrapper<ErpAccountsPayable> wrapper = baseWrapper(keyword, status, supplierId, startAt, endAt);
        wrapper.orderByDesc("updated_at");
        List<ErpAccountsPayable> items = erpAccountsPayableMapper.selectList(wrapper);
        return mapViews(items);
    }

    @Override
    public PageResponse<ErpAccountsPayableView> page(long page, long size, String keyword, String status, Long supplierId, Instant startAt, Instant endAt) {
        Page<ErpAccountsPayable> pageReq = Page.of(page, size);
        QueryWrapper<ErpAccountsPayable> wrapper = baseWrapper(keyword, status, supplierId, startAt, endAt);
        wrapper.orderByDesc("updated_at");
        Page<ErpAccountsPayable> result = erpAccountsPayableMapper.selectPage(pageReq, wrapper);
        List<ErpAccountsPayableView> views = mapViews(result.getRecords());
        return new PageResponse<>(result.getTotal(), result.getCurrent(), result.getSize(), views);
    }

    @Override
    public ErpAccountsPayableDetail getDetail(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        ErpAccountsPayable payable = erpAccountsPayableMapper.selectOne(new QueryWrapper<ErpAccountsPayable>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (payable == null) {
            throw new IllegalArgumentException("应付单不存在");
        }
        String supplierName = "-";
        if (payable.getSupplierId() != null) {
            ErpSupplier supplier = erpSupplierMapper.selectById(payable.getSupplierId());
            if (supplier != null) {
                supplierName = supplier.getName();
            }
        }
        List<ErpPaymentView> payments = loadPayments(tenantId, payable.getId(), supplierName);
        return new ErpAccountsPayableDetail(payable, supplierName, payments);
    }

    private QueryWrapper<ErpAccountsPayable> baseWrapper(String keyword, String status, Long supplierId, Instant startAt, Instant endAt) {
        QueryWrapper<ErpAccountsPayable> wrapper = new QueryWrapper<ErpAccountsPayable>()
            .eq("tenant_id", TenantContext.requireTenantId());
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like("order_no", keyword.trim());
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq("status", status.trim());
        }
        if (supplierId != null) {
            wrapper.eq("supplier_id", supplierId);
        }
        if (startAt != null) {
            wrapper.ge("created_at", startAt);
        }
        if (endAt != null) {
            wrapper.le("created_at", endAt);
        }
        return wrapper;
    }

    private List<ErpAccountsPayableView> mapViews(List<ErpAccountsPayable> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        Set<Long> supplierIds = items.stream()
            .map(ErpAccountsPayable::getSupplierId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Map<Long, String> supplierNameMap = new HashMap<>();
        if (!supplierIds.isEmpty()) {
            List<ErpSupplier> suppliers = erpSupplierMapper.selectBatchIds(supplierIds);
            for (ErpSupplier supplier : suppliers) {
                supplierNameMap.put(supplier.getId(), supplier.getName());
            }
        }
        return items.stream()
            .map(item -> new ErpAccountsPayableView(
                item.getId(),
                item.getOrderNo(),
                item.getSupplierId(),
                supplierNameMap.getOrDefault(item.getSupplierId(), "-"),
                item.getTotalAmount(),
                item.getPaidAmount(),
                item.getDiscountAmount(),
                item.getUnpaidAmount(),
                item.getStatus(),
                item.getCreatedAt()
            ))
            .toList();
    }

    private List<ErpPaymentView> loadPayments(Long tenantId, Long payableId, String supplierName) {
        try {
            if (payableId == null) {
                return List.of();
            }
            List<ErpPaymentPayable> allocations;
            try {
                allocations = erpPaymentPayableMapper.findByPayableId(tenantId, payableId);
            } catch (Exception ex) {
                allocations = List.of();
            }
            if (allocations != null && !allocations.isEmpty()) {
                List<Long> paymentIds = allocations.stream()
                    .map(ErpPaymentPayable::getPaymentId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
                if (!paymentIds.isEmpty()) {
                    List<ErpPayment> payments = erpPaymentMapper.selectBatchIds(paymentIds);
                    Map<Long, ErpPayment> paymentMap = payments == null ? new HashMap<>() : payments.stream()
                        .filter(item -> Objects.equals(item.getTenantId(), tenantId))
                        .collect(Collectors.toMap(ErpPayment::getId, item -> item, (a, b) -> a));
                    return allocations.stream()
                        .map(allocation -> {
                            ErpPayment payment = paymentMap.get(allocation.getPaymentId());
                            if (payment == null) {
                                return null;
                            }
                            return new ErpPaymentView(
                                payment.getId(),
                                payment.getPaymentNo(),
                                payment.getSupplierId(),
                                supplierName == null ? "-" : supplierName,
                                allocation.getPayableId(),
                                allocation.getAllocatedAmount(),
                                allocation.getAllocatedDiscount(),
                                payment.getStatus(),
                                payment.getCreatedAt(),
                                payment.getRemark()
                            );
                        })
                        .filter(Objects::nonNull)
                        .sorted((a, b) -> {
                            Instant atA = a.createdAt();
                            Instant atB = b.createdAt();
                            if (atA == null && atB == null) return 0;
                            if (atA == null) return 1;
                            if (atB == null) return -1;
                            return atB.compareTo(atA);
                        })
                        .toList();
                }
            }

            List<ErpPayment> payments = erpPaymentMapper.selectList(new QueryWrapper<ErpPayment>()
                .eq("tenant_id", tenantId)
                .eq("payable_id", payableId)
                .orderByDesc("created_at"));
            if (payments == null || payments.isEmpty()) {
                return List.of();
            }
            return payments.stream()
                .map(item -> new ErpPaymentView(
                    item.getId(),
                    item.getPaymentNo(),
                    item.getSupplierId(),
                    supplierName == null ? "-" : supplierName,
                    item.getPayableId(),
                    item.getAmount(),
                    item.getDiscountAmount(),
                    item.getStatus(),
                    item.getCreatedAt(),
                    item.getRemark()
                ))
                .toList();
        } catch (Exception ex) {
            return List.of();
        }
    }
}
