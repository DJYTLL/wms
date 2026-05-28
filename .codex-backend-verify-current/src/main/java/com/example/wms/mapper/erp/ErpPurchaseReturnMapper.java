package com.example.wms.mapper.erp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
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
}
