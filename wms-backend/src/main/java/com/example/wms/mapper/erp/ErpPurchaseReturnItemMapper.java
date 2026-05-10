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
    @Select("SELECT * FROM erp_purchase_return_item WHERE tenant_id = #{tenantId} AND return_id = #{returnId} AND deleted_at IS NULL ORDER BY sort_no ASC, id ASC")
    List<ErpPurchaseReturnItem> findByReturnId(@Param("tenantId") Long tenantId, @Param("returnId") Long returnId);
}
