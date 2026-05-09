package com.example.wms.mapper.erp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.erp.ErpAccountsPayable;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

// ERP应付单 Mapper
@Mapper
public interface ErpAccountsPayableMapper extends BaseMapper<ErpAccountsPayable> {
    @Select("SELECT * FROM erp_accounts_payable WHERE tenant_id = #{tenantId} AND purchase_order_id = #{purchaseOrderId}")
    ErpAccountsPayable findByPurchaseOrderId(@Param("tenantId") Long tenantId, @Param("purchaseOrderId") Long purchaseOrderId);

    @Select("SELECT * FROM erp_accounts_payable WHERE tenant_id = #{tenantId} AND purchase_return_id = #{purchaseReturnId}")
    ErpAccountsPayable findByPurchaseReturnId(@Param("tenantId") Long tenantId, @Param("purchaseReturnId") Long purchaseReturnId);

    @Select("""
        SELECT s.id AS supplier_id,
               s.name AS supplier_name,
               COALESCE(SUM(ap.unpaid_amount), 0) AS total_debt
        FROM erp_supplier s
        LEFT JOIN erp_accounts_payable ap
          ON ap.supplier_id = s.id
         AND ap.tenant_id = #{tenantId}
         AND ap.status <> 'RED_FLUSHED'
        WHERE s.tenant_id = #{tenantId}
          AND (COALESCE(CAST(#{keyword} AS TEXT), '') = ''
               OR LOWER(s.name) LIKE CONCAT('%', LOWER(CAST(#{keyword} AS TEXT)), '%'))
        GROUP BY s.id, s.name
        ORDER BY total_debt DESC, s.id
        """)
    List<com.example.wms.dto.erp.ErpSupplierDebtView> listSupplierDebt(@Param("tenantId") Long tenantId,
                                                                      @Param("keyword") String keyword);
}
