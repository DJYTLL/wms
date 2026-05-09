package com.example.wms.mapper.erp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.erp.ErpAssemblyOrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

// Assembly order item mapper
@Mapper
public interface ErpAssemblyOrderItemMapper extends BaseMapper<ErpAssemblyOrderItem> {
    @Select("SELECT * FROM erp_assembly_order_item WHERE tenant_id = #{tenantId} AND order_id = #{orderId} ORDER BY line_no ASC")
    List<ErpAssemblyOrderItem> findByOrderId(@Param("tenantId") Long tenantId, @Param("orderId") Long orderId);
}
