package com.example.wms.mapper.erp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.erp.ErpPaymentPayable;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

// ERP付款单-应付分摊 Mapper
@Mapper
public interface ErpPaymentPayableMapper extends BaseMapper<ErpPaymentPayable> {
    @Select("""
        SELECT *
        FROM erp_payment_payable
        WHERE tenant_id = #{tenantId}
          AND payment_id = #{paymentId}
          AND deleted_at IS NULL
        """)
    List<ErpPaymentPayable> findByPaymentId(@Param("tenantId") Long tenantId, @Param("paymentId") Long paymentId);

    @Select("""
        SELECT *
        FROM erp_payment_payable
        WHERE tenant_id = #{tenantId}
          AND payable_id = #{payableId}
          AND deleted_at IS NULL
        """)
    List<ErpPaymentPayable> findByPayableId(@Param("tenantId") Long tenantId, @Param("payableId") Long payableId);

    @Select("""
        SELECT COALESCE(SUM(pp.allocated_amount), 0)
        FROM erp_payment_payable pp
        JOIN erp_payment p
          ON p.tenant_id = pp.tenant_id
         AND p.id = pp.payment_id
         AND p.status = 'APPROVED'
         AND p.deleted_at IS NULL
        WHERE pp.tenant_id = #{tenantId}
          AND pp.payable_id = #{payableId}
          AND pp.deleted_at IS NULL
        """)
    BigDecimal sumApprovedAllocatedAmountByPayableId(@Param("tenantId") Long tenantId,
                                                     @Param("payableId") Long payableId);
}
