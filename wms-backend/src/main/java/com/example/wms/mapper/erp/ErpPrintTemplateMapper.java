package com.example.wms.mapper.erp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.erp.ErpPrintTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

// 打印模板 Mapper（ERP进销存）
@Mapper
public interface ErpPrintTemplateMapper extends BaseMapper<ErpPrintTemplate> {
    // 按编码查询
    @Select("SELECT * FROM erp_print_template WHERE tenant_id = #{tenantId} AND code = #{code} AND deleted_at IS NULL")
    ErpPrintTemplate findByCode(@Param("tenantId") Long tenantId, @Param("code") String code);

    // 查询默认模板
    @Select("SELECT * FROM erp_print_template WHERE tenant_id = #{tenantId} AND doc_type = #{docType} AND is_default = true AND is_enabled = true AND deleted_at IS NULL LIMIT 1")
    ErpPrintTemplate findDefault(@Param("tenantId") Long tenantId, @Param("docType") String docType);

    @Select("SELECT * FROM erp_print_template WHERE tenant_id = #{tenantId} AND doc_type = #{docType} AND is_enabled = true AND deleted_at IS NULL ORDER BY sort_no ASC, updated_at DESC LIMIT 1")
    ErpPrintTemplate findFirstEnabled(@Param("tenantId") Long tenantId, @Param("docType") String docType);
}
