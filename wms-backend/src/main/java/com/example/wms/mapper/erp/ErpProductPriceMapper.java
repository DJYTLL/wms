package com.example.wms.mapper.erp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.erp.ErpProductPrice;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

// 商品价格 Mapper（ERP进销存）
@Mapper
public interface ErpProductPriceMapper extends BaseMapper<ErpProductPrice> {
    // 按商品列出价格
    @Select("SELECT * FROM erp_product_price WHERE tenant_id = #{tenantId} AND product_id = #{productId} AND deleted_at IS NULL ORDER BY id")
    List<ErpProductPrice> listByProduct(@Param("tenantId") Long tenantId, @Param("productId") Long productId);

    // 按商品+客户类别查询价格
    @Select("SELECT * FROM erp_product_price WHERE tenant_id = #{tenantId} AND product_id = #{productId} AND customer_category_id = #{customerCategoryId} AND deleted_at IS NULL")
    ErpProductPrice findByProductAndCategory(@Param("tenantId") Long tenantId,
                                             @Param("productId") Long productId,
                                             @Param("customerCategoryId") Long customerCategoryId);

    // 删除某商品的全部价格
    @Update("""
        UPDATE erp_product_price
        SET deleted_at = NOW()
        WHERE tenant_id = #{tenantId}
          AND product_id = #{productId}
          AND deleted_at IS NULL
        """)
    int deleteByProduct(@Param("tenantId") Long tenantId, @Param("productId") Long productId);

    @Insert("""
        INSERT INTO erp_product_price (tenant_id, product_id, customer_category_id, sale_price, created_at, updated_at, deleted_at)
        VALUES (#{tenantId}, #{productId}, #{customerCategoryId}, #{salePrice}, NOW(), NOW(), NULL)
        ON CONFLICT (tenant_id, product_id, customer_category_id) WHERE deleted_at IS NULL
        DO UPDATE SET
            sale_price = EXCLUDED.sale_price,
            updated_at = NOW()
        """)
    int upsertActivePrice(@Param("tenantId") Long tenantId,
                          @Param("productId") Long productId,
                          @Param("customerCategoryId") Long customerCategoryId,
                          @Param("salePrice") java.math.BigDecimal salePrice);
}
