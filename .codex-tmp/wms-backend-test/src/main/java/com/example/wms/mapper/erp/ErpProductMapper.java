package com.example.wms.mapper.erp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.erp.ErpProduct;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

// 商品 Mapper（ERP进销存）
@Mapper
public interface ErpProductMapper extends BaseMapper<ErpProduct> {
    // 按编码查询
    @Select("SELECT * FROM erp_product WHERE tenant_id = #{tenantId} AND code = #{code} AND deleted_at IS NULL")
    ErpProduct findByCode(@Param("tenantId") Long tenantId, @Param("code") String code);

    @Select("SELECT * FROM erp_product WHERE tenant_id = #{tenantId} AND id = #{productId} AND deleted_at IS NULL FOR UPDATE")
    ErpProduct findByIdForUpdate(@Param("tenantId") Long tenantId, @Param("productId") Long productId);
}
