package com.example.wms.mapper.erp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.dto.erp.ErpIdAmountPair;
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
        UPDATE erp_sale_return
        SET status = 'APPROVED',
            approved_by = #{operator},
            approved_at = NOW(),
            updated_at = NOW(),
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
            version = COALESCE(version, 0) + 1
        WHERE tenant_id = #{tenantId}
          AND id = #{id}
          AND status = 'APPROVED'
          AND deleted_at IS NULL
        RETURNING *
        """)
    ErpSaleReturn redFlushApproved(@Param("tenantId") Long tenantId,
                                   @Param("id") Long id,
                                   @Param("remark") String remark);
}
