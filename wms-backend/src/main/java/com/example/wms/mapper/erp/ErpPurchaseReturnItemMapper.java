package com.example.wms.mapper.erp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.erp.ErpPurchaseReturnItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

// 采购退货明细 Mapper
@Mapper
public interface ErpPurchaseReturnItemMapper extends BaseMapper<ErpPurchaseReturnItem> {
    @Select("""
        SELECT item.*,
               purchase_order.order_no AS sourcePurchaseOrderNo,
               purchase_item.sort_no AS sourcePurchaseOrderItemSortNo
        FROM erp_purchase_return_item item
        LEFT JOIN erp_purchase_order purchase_order
          ON purchase_order.id = COALESCE(item.source_purchase_order_id, (
               SELECT owner_item.order_id
               FROM erp_purchase_order_item owner_item
               WHERE owner_item.tenant_id = item.tenant_id
                 AND owner_item.id = item.source_purchase_order_item_id
                 AND owner_item.deleted_at IS NULL
               LIMIT 1
          ))
         AND purchase_order.tenant_id = item.tenant_id
         AND purchase_order.deleted_at IS NULL
        LEFT JOIN erp_purchase_order_item purchase_item
          ON purchase_item.id = item.source_purchase_order_item_id
         AND purchase_item.tenant_id = item.tenant_id
         AND purchase_item.deleted_at IS NULL
        WHERE item.tenant_id = #{tenantId}
          AND item.return_id = #{returnId}
          AND item.deleted_at IS NULL
        ORDER BY item.sort_no ASC, item.id ASC
        """)
    List<ErpPurchaseReturnItem> findByReturnId(@Param("tenantId") Long tenantId, @Param("returnId") Long returnId);
}
