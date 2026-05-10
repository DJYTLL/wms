package com.example.wms.mapper.erp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.erp.ErpSaleOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

// 销售单 Mapper（ERP进销存）
@Mapper
public interface ErpSaleOrderMapper extends BaseMapper<ErpSaleOrder> {
    // 按单号查询
    @Select("SELECT * FROM erp_sale_order WHERE tenant_id = #{tenantId} AND order_no = #{orderNo} AND deleted_at IS NULL")
    ErpSaleOrder findByOrderNo(@Param("tenantId") Long tenantId, @Param("orderNo") String orderNo);
}
