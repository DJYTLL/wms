package com.example.wms.mapper.erp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.dto.erp.ErpStockOccupancyView;
import com.example.wms.entity.erp.ErpPurchaseReturn;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

// 采购退货单 Mapper
@Mapper
public interface ErpPurchaseReturnMapper extends BaseMapper<ErpPurchaseReturn> {
    @Select("SELECT * FROM erp_purchase_return WHERE tenant_id = #{tenantId} AND order_no = #{orderNo} AND deleted_at IS NULL")
    ErpPurchaseReturn findByOrderNo(@Param("tenantId") Long tenantId, @Param("orderNo") String orderNo);

    @Select("""
        SELECT *
        FROM erp_purchase_return
        WHERE tenant_id = #{tenantId}
          AND purchase_order_id = #{purchaseOrderId}
          AND status = 'APPROVED'
          AND deleted_at IS NULL
        """)
    List<ErpPurchaseReturn> findApprovedByPurchaseOrderId(@Param("tenantId") Long tenantId,
                                                          @Param("purchaseOrderId") Long purchaseOrderId);

    @Select("""
        SELECT 'PURCHASE_RETURN' AS docType,
               r.order_no AS docNo,
               r.id AS docId,
               SUM(i.qty) AS qty,
               r.order_at AS orderAt,
               'erp-purchase-returns-draft-edit' AS routeName
        FROM erp_purchase_return r
        JOIN erp_purchase_return_item i
          ON i.return_id = r.id
         AND i.tenant_id = r.tenant_id
         AND i.deleted_at IS NULL
        WHERE r.tenant_id = #{tenantId}
          AND r.deleted_at IS NULL
          AND r.status = 'DRAFT'
          AND r.inventory_reserved = TRUE
          AND i.product_id = #{productId}
          AND i.warehouse_id IS NOT DISTINCT FROM #{warehouseId}
          AND i.location_id IS NOT DISTINCT FROM #{locationId}
        GROUP BY r.id, r.order_no, r.order_at
        HAVING SUM(i.qty) > 0
        ORDER BY r.order_at DESC NULLS LAST, r.id DESC
        """)
    List<ErpStockOccupancyView> findStockOccupancy(@Param("tenantId") Long tenantId,
                                                   @Param("productId") Long productId,
                                                   @Param("warehouseId") Long warehouseId,
                                                   @Param("locationId") Long locationId);
}
