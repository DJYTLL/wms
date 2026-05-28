package com.example.wms.mapper.erp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.erp.ErpReceiptMethod;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

// 收款方式 Mapper（ERP进销存）
@Mapper
public interface ErpReceiptMethodMapper extends BaseMapper<ErpReceiptMethod> {
    @Select("SELECT * FROM erp_receipt_method WHERE tenant_id = #{tenantId} AND code = #{code} AND deleted_at IS NULL")
    ErpReceiptMethod findByCode(@Param("tenantId") Long tenantId, @Param("code") String code);

    @Select("SELECT * FROM erp_receipt_method WHERE tenant_id = #{tenantId} AND is_default = true AND deleted_at IS NULL LIMIT 1")
    ErpReceiptMethod findDefault(@Param("tenantId") Long tenantId);
}
