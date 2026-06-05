package com.example.wms.mapper.erp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.erp.ErpProductStockPolicy;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ErpProductStockPolicyMapper extends BaseMapper<ErpProductStockPolicy> {
    @Select("""
        SELECT *
        FROM erp_product_stock_policy
        WHERE tenant_id = #{tenantId}
          AND product_id = #{productId}
          AND deleted_at IS NULL
        ORDER BY id
        """)
    List<ErpProductStockPolicy> listByProduct(@Param("tenantId") Long tenantId, @Param("productId") Long productId);

    @Update("""
        UPDATE erp_product_stock_policy
        SET deleted_at = NOW()
        WHERE tenant_id = #{tenantId}
          AND product_id = #{productId}
          AND deleted_at IS NULL
        """)
    int deleteByProduct(@Param("tenantId") Long tenantId, @Param("productId") Long productId);
}
