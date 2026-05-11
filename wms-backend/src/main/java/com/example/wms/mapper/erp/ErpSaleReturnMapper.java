package com.example.wms.mapper.erp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.erp.ErpSaleReturn;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

// 销售退货单 Mapper
@Mapper
public interface ErpSaleReturnMapper extends BaseMapper<ErpSaleReturn> {
    @Select("SELECT * FROM erp_sale_return WHERE tenant_id = #{tenantId} AND order_no = #{orderNo} AND deleted_at IS NULL")
    ErpSaleReturn findByOrderNo(@Param("tenantId") Long tenantId, @Param("orderNo") String orderNo);

    @Select("""
        SELECT COUNT(1)
        FROM erp_sale_return
        WHERE tenant_id = #{tenantId}
          AND sale_order_id = #{saleOrderId}
          AND status = 'APPROVED'
          AND deleted_at IS NULL
        """)
    long countApprovedBySaleOrderId(@Param("tenantId") Long tenantId, @Param("saleOrderId") Long saleOrderId);
}
