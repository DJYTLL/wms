package com.example.wms.mapper.erp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.erp.ErpStockBalance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

// 库存余额 Mapper（ERP进销存）
@Mapper
public interface ErpStockBalanceMapper extends BaseMapper<ErpStockBalance> {
    // 按库存维度查询（支持空库位）
    @Select("SELECT * FROM erp_stock_balance WHERE tenant_id = #{tenantId} AND product_id = #{productId} AND warehouse_id IS NOT DISTINCT FROM #{warehouseId} AND location_id IS NOT DISTINCT FROM #{locationId}")
    ErpStockBalance findByKey(@Param("tenantId") Long tenantId,
                              @Param("productId") Long productId,
                              @Param("warehouseId") Long warehouseId,
                              @Param("locationId") Long locationId);

    @Select("SELECT COALESCE(SUM(qty_on_hand), 0) FROM erp_stock_balance WHERE tenant_id = #{tenantId} AND product_id = #{productId}")
    BigDecimal sumQtyByProduct(@Param("tenantId") Long tenantId,
                               @Param("productId") Long productId);
}
