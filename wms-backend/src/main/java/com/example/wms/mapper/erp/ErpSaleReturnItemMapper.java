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
    @Select("SELECT * FROM erp_sale_return_item WHERE tenant_id = #{tenantId} AND return_id = #{returnId} ORDER BY sort_no ASC, id ASC")
    List<ErpSaleReturnItem> findByReturnId(@Param("tenantId") Long tenantId, @Param("returnId") Long returnId);
}
