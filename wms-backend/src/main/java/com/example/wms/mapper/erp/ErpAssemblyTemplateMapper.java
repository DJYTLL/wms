package com.example.wms.mapper.erp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.erp.ErpAssemblyTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

// Assembly template mapper
@Mapper
public interface ErpAssemblyTemplateMapper extends BaseMapper<ErpAssemblyTemplate> {
    @Select("""
        <script>
        SELECT *
        FROM erp_assembly_template
        WHERE tenant_id = #{tenantId}
          AND order_type = #{orderType}
          AND deleted_at IS NULL
          <if test="keyword != null and keyword != ''">
            AND name ILIKE CONCAT('%', #{keyword}, '%')
          </if>
        ORDER BY updated_at DESC, id DESC
        </script>
        """)
    List<ErpAssemblyTemplate> findByType(@Param("tenantId") Long tenantId,
                                         @Param("orderType") String orderType,
                                         @Param("keyword") String keyword);

    @Select("""
        SELECT *
        FROM erp_assembly_template
        WHERE tenant_id = #{tenantId}
          AND order_type = #{orderType}
          AND finished_product_id = #{finishedProductId}
          AND deleted_at IS NULL
        ORDER BY updated_at DESC, id DESC
        """)
    List<ErpAssemblyTemplate> findByFinishedProduct(@Param("tenantId") Long tenantId,
                                                    @Param("orderType") String orderType,
                                                    @Param("finishedProductId") Long finishedProductId);

    @Select("SELECT * FROM erp_assembly_template WHERE tenant_id = #{tenantId} AND order_type = #{orderType} AND name = #{name} AND deleted_at IS NULL LIMIT 1")
    ErpAssemblyTemplate findByName(@Param("tenantId") Long tenantId,
                                   @Param("orderType") String orderType,
                                   @Param("name") String name);
}
