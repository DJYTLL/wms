package com.example.wms.mapper.erp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.erp.ErpPurchaseOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

// 采购单 Mapper（ERP进销存）
@Mapper
public interface ErpPurchaseOrderMapper extends BaseMapper<ErpPurchaseOrder> {
    // 按单号查询
    @Select("SELECT * FROM erp_purchase_order WHERE tenant_id = #{tenantId} AND order_no = #{orderNo} AND deleted_at IS NULL")
    ErpPurchaseOrder findByOrderNo(@Param("tenantId") Long tenantId, @Param("orderNo") String orderNo);
}
