package com.example.wms.mapper.erp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.erp.ErpProductPrice;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

// 商品价格 Mapper（ERP进销存）
@Mapper
public interface ErpProductPriceMapper extends BaseMapper<ErpProductPrice> {
    // 按商品列出价格
    @Select("SELECT * FROM erp_product_price WHERE tenant_id = #{tenantId} AND product_id = #{productId} ORDER BY id")
    List<ErpProductPrice> listByProduct(@Param("tenantId") Long tenantId, @Param("productId") Long productId);

    // 按商品+客户类别查询价格
    @Select("SELECT * FROM erp_product_price WHERE tenant_id = #{tenantId} AND product_id = #{productId} AND customer_category_id = #{customerCategoryId}")
    ErpProductPrice findByProductAndCategory(@Param("tenantId") Long tenantId,
                                             @Param("productId") Long productId,
                                             @Param("customerCategoryId") Long customerCategoryId);

    // 删除某商品的全部价格
    @Delete("DELETE FROM erp_product_price WHERE tenant_id = #{tenantId} AND product_id = #{productId}")
    int deleteByProduct(@Param("tenantId") Long tenantId, @Param("productId") Long productId);
}
