package com.example.wms.mapper.erp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.erp.ErpAssemblyTemplateItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

// Assembly template item mapper
@Mapper
public interface ErpAssemblyTemplateItemMapper extends BaseMapper<ErpAssemblyTemplateItem> {
    @Select("SELECT * FROM erp_assembly_template_item WHERE tenant_id = #{tenantId} AND template_id = #{templateId} AND deleted_at IS NULL ORDER BY line_no ASC")
    List<ErpAssemblyTemplateItem> findByTemplateId(@Param("tenantId") Long tenantId, @Param("templateId") Long templateId);
}
