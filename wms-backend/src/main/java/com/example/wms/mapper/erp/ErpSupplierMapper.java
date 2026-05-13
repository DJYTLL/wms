package com.example.wms.mapper.erp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.erp.ErpSupplier;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.Instant;

// 供应商 Mapper（ERP进销存）
@Mapper
public interface ErpSupplierMapper extends BaseMapper<ErpSupplier> {
    // 按编码查询
    @Select("SELECT * FROM erp_supplier WHERE tenant_id = #{tenantId} AND code = #{code} AND deleted_at IS NULL")
    ErpSupplier findByCode(@Param("tenantId") Long tenantId, @Param("code") String code);

    @Select("""
        SELECT MAX(recent_at)
        FROM (
            SELECT MAX(created_at) AS recent_at
            FROM erp_purchase_order
            WHERE tenant_id = #{tenantId} AND supplier_id = #{supplierId} AND deleted_at IS NULL
            UNION ALL
            SELECT MAX(created_at) AS recent_at
            FROM erp_purchase_return
            WHERE tenant_id = #{tenantId} AND supplier_id = #{supplierId} AND deleted_at IS NULL
            UNION ALL
            SELECT MAX(created_at) AS recent_at
            FROM erp_payment
            WHERE tenant_id = #{tenantId} AND supplier_id = #{supplierId} AND deleted_at IS NULL
            UNION ALL
            SELECT MAX(created_at) AS recent_at
            FROM erp_accounts_payable
            WHERE tenant_id = #{tenantId} AND supplier_id = #{supplierId} AND deleted_at IS NULL
        ) recent_txn
        """)
    Instant findRecentTransactionAt(@Param("tenantId") Long tenantId, @Param("supplierId") Long supplierId);
}
