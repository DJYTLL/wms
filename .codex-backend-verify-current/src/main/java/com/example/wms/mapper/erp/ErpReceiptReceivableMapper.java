package com.example.wms.mapper.erp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.erp.ErpReceiptReceivable;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

// ERP收款单-应收分摊 Mapper
@Mapper
public interface ErpReceiptReceivableMapper extends BaseMapper<ErpReceiptReceivable> {
    @Select("""
        SELECT *
        FROM erp_receipt_receivable
        WHERE tenant_id = #{tenantId}
          AND receipt_id = #{receiptId}
          AND deleted_at IS NULL
        """)
    List<ErpReceiptReceivable> findByReceiptId(@Param("tenantId") Long tenantId, @Param("receiptId") Long receiptId);

    @Select("""
        SELECT *
        FROM erp_receipt_receivable
        WHERE tenant_id = #{tenantId}
          AND receivable_id = #{receivableId}
          AND deleted_at IS NULL
        """)
    List<ErpReceiptReceivable> findByReceivableId(@Param("tenantId") Long tenantId, @Param("receivableId") Long receivableId);

    @Select("""
        SELECT COALESCE(SUM(rr.allocated_amount), 0)
        FROM erp_receipt_receivable rr
        JOIN erp_receipt r
          ON r.tenant_id = rr.tenant_id
         AND r.id = rr.receipt_id
         AND r.status = 'APPROVED'
         AND r.deleted_at IS NULL
        WHERE rr.tenant_id = #{tenantId}
          AND rr.receivable_id = #{receivableId}
          AND rr.deleted_at IS NULL
        """)
    BigDecimal sumApprovedAllocatedAmountByReceivableId(@Param("tenantId") Long tenantId,
                                                        @Param("receivableId") Long receivableId);
}
