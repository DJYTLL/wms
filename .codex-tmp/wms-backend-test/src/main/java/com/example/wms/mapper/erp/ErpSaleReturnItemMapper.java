package com.example.wms.mapper.erp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.erp.ErpSaleReturnItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

// 销售退货明细 Mapper
@Mapper
public interface ErpSaleReturnItemMapper extends BaseMapper<ErpSaleReturnItem> {
    @Select("""
        SELECT item.*,
               sale_order.order_no AS sourceSaleOrderNo,
               sale_item.sort_no AS sourceSaleOrderItemSortNo
        FROM erp_sale_return_item item
        LEFT JOIN erp_sale_order sale_order
          ON sale_order.id = COALESCE(item.source_sale_order_id, (
               SELECT owner_item.order_id
               FROM erp_sale_order_item owner_item
               WHERE owner_item.tenant_id = item.tenant_id
                 AND owner_item.id = item.source_sale_order_item_id
                 AND owner_item.deleted_at IS NULL
               LIMIT 1
          ))
         AND sale_order.tenant_id = item.tenant_id
         AND sale_order.deleted_at IS NULL
        LEFT JOIN erp_sale_order_item sale_item
          ON sale_item.id = item.source_sale_order_item_id
         AND sale_item.tenant_id = item.tenant_id
         AND sale_item.deleted_at IS NULL
        WHERE item.tenant_id = #{tenantId}
          AND item.return_id = #{returnId}
          AND item.deleted_at IS NULL
        ORDER BY item.sort_no ASC, item.id ASC
        """)
    List<ErpSaleReturnItem> findByReturnId(@Param("tenantId") Long tenantId, @Param("returnId") Long returnId);
}
