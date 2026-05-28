package com.example.wms.service.erp;

import com.example.wms.dto.erp.ErpProductPriceItemRequest;
import com.example.wms.entity.erp.ErpProductPrice;

import java.math.BigDecimal;
import java.util.List;

// 商品价格服务（ERP进销存）
public interface ErpProductPriceService {
    // 按商品列出价格
    List<ErpProductPrice> listByProduct(Long productId);

    // 保存商品价格
    void saveForProduct(Long productId, List<ErpProductPriceItemRequest> items);

    // 解析商品价格
    BigDecimal resolvePrice(Long productId, Long customerCategoryId);
}
