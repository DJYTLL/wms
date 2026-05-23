package com.example.wms.service.erp.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpAccountsReceivableDetail;
import com.example.wms.dto.erp.ErpAccountsReceivableView;
import com.example.wms.dto.erp.ErpReceiptView;
import com.example.wms.entity.erp.ErpAccountsReceivable;
import com.example.wms.entity.erp.ErpCustomer;
import com.example.wms.entity.erp.ErpReceipt;
import com.example.wms.entity.erp.ErpReceiptReceivable;
import com.example.wms.mapper.erp.ErpAccountsReceivableMapper;
import com.example.wms.mapper.erp.ErpCustomerMapper;
import com.example.wms.mapper.erp.ErpReceiptMapper;
import com.example.wms.mapper.erp.ErpReceiptReceivableMapper;
import com.example.wms.service.erp.ErpAccountsReceivableService;
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

// ERP应收单服务实现
@Service
public class ErpAccountsReceivableServiceImpl implements ErpAccountsReceivableService {
    private final ErpAccountsReceivableMapper erpAccountsReceivableMapper;
    private final ErpCustomerMapper erpCustomerMapper;
    private final ErpReceiptMapper erpReceiptMapper;
    private final ErpReceiptReceivableMapper erpReceiptReceivableMapper;

    public ErpAccountsReceivableServiceImpl(ErpAccountsReceivableMapper erpAccountsReceivableMapper,
                                            ErpCustomerMapper erpCustomerMapper,
                                            ErpReceiptMapper erpReceiptMapper,
                                            ErpReceiptReceivableMapper erpReceiptReceivableMapper) {
        this.erpAccountsReceivableMapper = erpAccountsReceivableMapper;
        this.erpCustomerMapper = erpCustomerMapper;
        this.erpReceiptMapper = erpReceiptMapper;
        this.erpReceiptReceivableMapper = erpReceiptReceivableMapper;
    }

    @Override
    public List<ErpAccountsReceivableView> listAll(String keyword, String status, Long customerId, Instant startAt, Instant endAt) {
        QueryWrapper<ErpAccountsReceivable> wrapper = baseWrapper(keyword, status, customerId, startAt, endAt);
        wrapper.orderByDesc("updated_at");
        List<ErpAccountsReceivable> items = erpAccountsReceivableMapper.selectList(wrapper);
        return mapViews(items);
    }

    @Override
    public PageResponse<ErpAccountsReceivableView> page(long page, long size, String keyword, String status, Long customerId, Instant startAt, Instant endAt) {
        Page<ErpAccountsReceivable> pageReq = Page.of(page, size);
        QueryWrapper<ErpAccountsReceivable> wrapper = baseWrapper(keyword, status, customerId, startAt, endAt);
        wrapper.orderByDesc("updated_at");
        Page<ErpAccountsReceivable> result = erpAccountsReceivableMapper.selectPage(pageReq, wrapper);
        List<ErpAccountsReceivableView> views = mapViews(result.getRecords());
        return new PageResponse<>(result.getTotal(), result.getCurrent(), result.getSize(), views);
    }

    @Override
    public ErpAccountsReceivableDetail getDetail(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        ErpAccountsReceivable receivable = erpAccountsReceivableMapper.selectOne(new QueryWrapper<ErpAccountsReceivable>()
            .eq("tenant_id", tenantId)
            .eq("id", id));
        if (receivable == null) {
            throw new IllegalArgumentException("应收单不存在");
        }
        Map<Long, String> customerNameMap = loadCustomerNameMap(receivable.getCustomerId() == null ? Set.of() : Set.of(receivable.getCustomerId()));
        String customerName = customerNameMap.getOrDefault(receivable.getCustomerId(), "-");
        List<ErpReceiptView> receipts = loadReceipts(tenantId, receivable.getId(), customerName);
        return new ErpAccountsReceivableDetail(receivable, customerName, receipts);
    }

    private QueryWrapper<ErpAccountsReceivable> baseWrapper(String keyword, String status, Long customerId, Instant startAt, Instant endAt) {
        QueryWrapper<ErpAccountsReceivable> wrapper = new QueryWrapper<ErpAccountsReceivable>()
            .eq("tenant_id", TenantContext.requireTenantId());
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like("order_no", keyword.trim());
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq("status", status.trim());
        }
        if (customerId != null) {
            wrapper.eq("customer_id", customerId);
        }
        if (startAt != null) {
            wrapper.ge("created_at", startAt);
        }
        if (endAt != null) {
            wrapper.le("created_at", endAt);
        }
        return wrapper;
    }

    private List<ErpAccountsReceivableView> mapViews(List<ErpAccountsReceivable> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        Set<Long> customerIds = items.stream()
            .map(ErpAccountsReceivable::getCustomerId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Map<Long, String> customerNameMap = loadCustomerNameMap(customerIds);
        return items.stream()
            .map(item -> new ErpAccountsReceivableView(
                item.getId(),
                item.getOrderNo(),
                item.getCustomerId(),
                customerNameMap.getOrDefault(item.getCustomerId(), "-"),
                item.getTotalAmount(),
                item.getPaidAmount(),
                item.getUnpaidAmount(),
                item.getStatus(),
                item.getCreatedAt()
            ))
            .toList();
    }

    private List<ErpReceiptView> loadReceipts(Long tenantId, Long receivableId, String customerName) {
        try {
            if (receivableId == null) {
                return List.of();
            }
            List<ErpReceiptReceivable> allocations;
            try {
                allocations = erpReceiptReceivableMapper.findByReceivableId(tenantId, receivableId);
            } catch (Exception ex) {
                allocations = List.of();
            }
            if (allocations != null && !allocations.isEmpty()) {
                List<Long> receiptIds = allocations.stream()
                    .map(ErpReceiptReceivable::getReceiptId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
                if (!receiptIds.isEmpty()) {
                    List<ErpReceipt> receipts = erpReceiptMapper.selectBatchIds(receiptIds);
                    Map<Long, ErpReceipt> receiptMap = receipts == null ? new HashMap<>() : receipts.stream()
                        .filter(item -> Objects.equals(item.getTenantId(), tenantId))
                        .collect(Collectors.toMap(ErpReceipt::getId, item -> item, (a, b) -> a));
                    return allocations.stream()
                        .map(allocation -> {
                            ErpReceipt receipt = receiptMap.get(allocation.getReceiptId());
                            if (receipt == null) {
                                return null;
                            }
                            return new ErpReceiptView(
                                receipt.getId(),
                                receipt.getReceiptNo(),
                                receipt.getCustomerId(),
                                customerName == null ? "-" : customerName,
                                allocation.getReceivableId(),
                                allocation.getAllocatedAmount(),
                                allocation.getAllocatedDiscount(),
                                receipt.getStatus(),
                                receipt.getCreatedAt(),
                                receipt.getRemark()
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

            List<ErpReceipt> receipts = erpReceiptMapper.selectList(new QueryWrapper<ErpReceipt>()
                .eq("tenant_id", tenantId)
                .eq("receivable_id", receivableId)
                .ge("amount", BigDecimal.ZERO)
                .orderByDesc("created_at"));
            if (receipts == null || receipts.isEmpty()) {
                return List.of();
            }
            return receipts.stream()
                .map(item -> new ErpReceiptView(
                    item.getId(),
                    item.getReceiptNo(),
                    item.getCustomerId(),
                    customerName == null ? "-" : customerName,
                    item.getReceivableId(),
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

    private Map<Long, String> loadCustomerNameMap(Set<Long> customerIds) {
        Set<Long> effectiveIds = customerIds == null
            ? Set.of()
            : customerIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (effectiveIds.isEmpty()) {
            return Map.of();
        }
        List<ErpCustomer> customers = erpCustomerMapper.selectBatchIds(effectiveIds);
        Map<Long, String> customerNameMap = new HashMap<>();
        for (ErpCustomer customer : customers) {
            customerNameMap.put(customer.getId(), customer.getName());
        }
        return customerNameMap;
    }
}
