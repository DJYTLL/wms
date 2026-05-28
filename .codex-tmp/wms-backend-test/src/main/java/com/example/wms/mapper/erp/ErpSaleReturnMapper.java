package com.example.wms.mapper.erp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.dto.erp.ErpIdAmountPair;
import com.example.wms.dto.erp.ErpSaleReturnRefundSnapshot;
import com.example.wms.entity.erp.ErpSaleReturn;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

// 销售退货单 Mapper
@Mapper
public interface ErpSaleReturnMapper extends BaseMapper<ErpSaleReturn> {
    @Select("SELECT * FROM erp_sale_return WHERE tenant_id = #{tenantId} AND order_no = #{orderNo} AND deleted_at IS NULL")
    ErpSaleReturn findByOrderNo(@Param("tenantId") Long tenantId, @Param("orderNo") String orderNo);

    @Select("""
        SELECT COUNT(1)
        FROM erp_sale_return
        WHERE tenant_id = #{tenantId}
          AND sale_order_id = #{saleOrderId}
          AND status = 'APPROVED'
          AND deleted_at IS NULL
        """)
    long countApprovedBySaleOrderId(@Param("tenantId") Long tenantId, @Param("saleOrderId") Long saleOrderId);

    @Select("""
        SELECT COALESCE(SUM(total_amount_incl_tax), 0)
        FROM erp_sale_return
        WHERE tenant_id = #{tenantId}
          AND sale_order_id = #{saleOrderId}
          AND status = 'APPROVED'
          AND deleted_at IS NULL
        """)
    BigDecimal sumApprovedAmountBySaleOrderId(@Param("tenantId") Long tenantId,
                                              @Param("saleOrderId") Long saleOrderId);

    @Select("""
        <script>
        SELECT sale_order_id AS id,
               COALESCE(SUM(total_amount_incl_tax), 0) AS amount
        FROM erp_sale_return
        WHERE tenant_id = #{tenantId}
          AND status = 'APPROVED'
          AND deleted_at IS NULL
          AND sale_order_id IN
          <foreach collection='saleOrderIds' item='saleOrderId' open='(' separator=',' close=')'>
            #{saleOrderId}
          </foreach>
        GROUP BY sale_order_id
        </script>
        """)
    List<ErpIdAmountPair> sumApprovedAmountsBySaleOrderIds(@Param("tenantId") Long tenantId,
                                                           @Param("saleOrderIds") List<Long> saleOrderIds);

    @Select("""
        <script>
        SELECT sr.id AS return_id,
               COALESCE(source_receivable.status, legacy_receivable.status) AS refund_status,
               COALESCE(source_receivable.unpaid_amount, legacy_receivable.unpaid_amount) AS refund_unpaid_amount
        FROM erp_sale_return sr
        LEFT JOIN LATERAL (
            SELECT ar.status, ar.unpaid_amount
            FROM erp_accounts_receivable ar
            WHERE ar.tenant_id = sr.tenant_id
              AND ar.source_type = 'SALE_RETURN'
              AND ar.source_id = sr.id
              AND ar.deleted_at IS NULL
            ORDER BY ar.id DESC
            LIMIT 1
        ) source_receivable ON TRUE
        LEFT JOIN LATERAL (
            SELECT ar.status, ar.unpaid_amount
            FROM erp_accounts_receivable ar
            WHERE source_receivable.status IS NULL
              AND ar.tenant_id = sr.tenant_id
              AND ar.sale_order_id = sr.sale_order_id
              AND ar.total_amount &lt; 0
              AND ar.deleted_at IS NULL
              AND ar.remark LIKE CONCAT('%', '销售退货单号:', sr.order_no, '%')
            ORDER BY ar.id DESC
            LIMIT 1
        ) legacy_receivable ON TRUE
        WHERE sr.tenant_id = #{tenantId}
          AND sr.id IN
          <foreach collection='returnIds' item='returnId' open='(' separator=',' close=')'>
            #{returnId}
          </foreach>
        </script>
        """)
    List<ErpSaleReturnRefundSnapshot> findRefundSnapshotsByReturnIds(@Param("tenantId") Long tenantId,
                                                                     @Param("returnIds") List<Long> returnIds);

    @Select("""
        UPDATE erp_sale_return
        SET status = 'APPROVED',
            approved_by = #{operator},
            approved_at = NOW(),
            updated_at = NOW(),
            updated_by = #{operator},
            version = COALESCE(version, 0) + 1
        WHERE tenant_id = #{tenantId}
          AND id = #{id}
          AND status = 'DRAFT'
          AND deleted_at IS NULL
        RETURNING *
        """)
    ErpSaleReturn approveDraft(@Param("tenantId") Long tenantId,
                               @Param("id") Long id,
                               @Param("operator") String operator);

    @Select("""
        UPDATE erp_sale_return
        SET status = 'RED_FLUSHED',
            red_flush_source_type = 'SALE_RETURN',
            red_flush_source_id = id,
            remark = #{remark},
            updated_at = NOW(),
            updated_by = #{operator},
            version = COALESCE(version, 0) + 1
        WHERE tenant_id = #{tenantId}
          AND id = #{id}
          AND status = 'APPROVED'
          AND deleted_at IS NULL
        RETURNING *
        """)
    ErpSaleReturn redFlushApproved(@Param("tenantId") Long tenantId,
                                   @Param("id") Long id,
                                   @Param("remark") String remark,
                                   @Param("operator") String operator);
}
