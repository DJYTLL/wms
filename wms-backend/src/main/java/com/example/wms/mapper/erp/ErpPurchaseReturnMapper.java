package com.example.wms.mapper.erp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.erp.ErpPurchaseReturn;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

// 采购退货单 Mapper
@Mapper
public interface ErpPurchaseReturnMapper extends BaseMapper<ErpPurchaseReturn> {
    @Select("SELECT * FROM erp_purchase_return WHERE tenant_id = #{tenantId} AND order_no = #{orderNo}")
    ErpPurchaseReturn findByOrderNo(@Param("tenantId") Long tenantId, @Param("orderNo") String orderNo);
}
