package com.example.wms.mapper.erp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.erp.ErpAccountsReceivable;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

// ERP应收单 Mapper
@Mapper
public interface ErpAccountsReceivableMapper extends BaseMapper<ErpAccountsReceivable> {
    @Select("""
        SELECT *
        FROM erp_accounts_receivable
        WHERE tenant_id = #{tenantId}
          AND sale_order_id = #{saleOrderId}
          AND total_amount >= 0
          AND deleted_at IS NULL
        ORDER BY id DESC
        LIMIT 1
        """)
    ErpAccountsReceivable findBySaleOrderId(@Param("tenantId") Long tenantId, @Param("saleOrderId") Long saleOrderId);

    @Select("""
        SELECT *
        FROM erp_accounts_receivable
        WHERE tenant_id = #{tenantId}
          AND source_type = #{sourceType}
          AND source_id = #{sourceId}
          AND deleted_at IS NULL
        ORDER BY id DESC
        LIMIT 1
        """)
    ErpAccountsReceivable findBySource(@Param("tenantId") Long tenantId,
                                       @Param("sourceType") String sourceType,
                                       @Param("sourceId") Long sourceId);

    @Select("""
        SELECT c.id AS customer_id,
               c.name AS customer_name,
               COALESCE(SUM(ar.unpaid_amount), 0) AS total_debt
        FROM erp_customer c
        LEFT JOIN erp_accounts_receivable ar
          ON ar.customer_id = c.id
         AND ar.tenant_id = #{tenantId}
         AND ar.status <> 'RED_FLUSHED'
         AND ar.deleted_at IS NULL
        WHERE c.tenant_id = #{tenantId}
          AND c.deleted_at IS NULL
          AND (COALESCE(CAST(#{keyword} AS TEXT), '') = ''
               OR LOWER(c.name) LIKE CONCAT('%', LOWER(CAST(#{keyword} AS TEXT)), '%'))
        GROUP BY c.id, c.name
        ORDER BY total_debt DESC, c.id
        """)
    List<com.example.wms.dto.erp.ErpCustomerDebtView> listCustomerDebt(@Param("tenantId") Long tenantId,
                                                                       @Param("keyword") String keyword);

    @Select("""
        UPDATE erp_accounts_receivable
        SET paid_amount = paid_amount + #{delta},
            unpaid_amount = total_amount - (paid_amount + #{delta}),
            status = CASE
                WHEN total_amount - (paid_amount + #{delta}) = 0 THEN 'SETTLED'
                ELSE 'OPEN'
            END,
            updated_at = NOW()
        WHERE tenant_id = #{tenantId}
          AND id = #{id}
          AND deleted_at IS NULL
          AND status <> 'RED_FLUSHED'
          AND (
              (total_amount >= 0 AND paid_amount + #{delta} BETWEEN 0 AND total_amount)
              OR
              (total_amount < 0 AND paid_amount + #{delta} BETWEEN total_amount AND 0)
          )
        RETURNING *
        """)
    ErpAccountsReceivable applyPaidDeltaIfInRange(@Param("tenantId") Long tenantId,
                                                  @Param("id") Long id,
                                                  @Param("delta") BigDecimal delta);
}
