package com.example.wms.mapper.erp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.erp.ErpProductFitment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

// 商品适配车型关系 Mapper（ERP进销存）
@Mapper
public interface ErpProductFitmentMapper extends BaseMapper<ErpProductFitment> {
    @Select("SELECT * FROM erp_product_fitment WHERE tenant_id = #{tenantId} AND product_id = #{productId} AND model_id = #{modelId} AND deleted_at IS NULL")
    ErpProductFitment findByKey(@Param("tenantId") Long tenantId,
                                @Param("productId") Long productId,
                                @Param("modelId") Long modelId);
}
