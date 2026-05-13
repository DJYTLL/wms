package com.example.wms.mapper.erp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.erp.ErpStockCount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

// 库存盘点单 Mapper（ERP进销存）
@Mapper
public interface ErpStockCountMapper extends BaseMapper<ErpStockCount> {
    @Select("""
        SELECT *
        FROM erp_stock_count
        WHERE tenant_id = #{tenantId}
          AND id = #{id}
          AND deleted_at IS NULL
        FOR UPDATE
        """)
    ErpStockCount findByIdForUpdate(@Param("tenantId") Long tenantId, @Param("id") Long id);
}
