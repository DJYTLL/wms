package com.example.wms.mapper.erp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.erp.ErpReceipt;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

// ERP收款单 Mapper
@Mapper
public interface ErpReceiptMapper extends BaseMapper<ErpReceipt> {
    @Select("""
        SELECT *
        FROM erp_receipt
        WHERE tenant_id = #{tenantId}
          AND sale_order_id = #{saleOrderId}
          AND deleted_at IS NULL
        LIMIT 1
        """)
    ErpReceipt findBySaleOrderId(@Param("tenantId") Long tenantId, @Param("saleOrderId") Long saleOrderId);

    @Select("""
        UPDATE erp_receipt
        SET status = 'APPROVED',
            updated_at = NOW(),
            updated_by = #{operator}
        WHERE tenant_id = #{tenantId}
          AND id = #{id}
          AND status = 'DRAFT'
          AND deleted_at IS NULL
        RETURNING *
        """)
    ErpReceipt approveDraft(@Param("tenantId") Long tenantId,
                            @Param("id") Long id,
                            @Param("operator") String operator);

    @Select("""
        UPDATE erp_receipt
        SET status = 'RED_FLUSHED',
            red_flush_source_type = 'RECEIPT',
            red_flush_source_id = id,
            remark = #{remark},
            updated_at = NOW(),
            updated_by = #{operator}
        WHERE tenant_id = #{tenantId}
          AND id = #{id}
          AND status = 'APPROVED'
          AND deleted_at IS NULL
        RETURNING *
        """)
    ErpReceipt redFlushApproved(@Param("tenantId") Long tenantId,
                                @Param("id") Long id,
                                @Param("remark") String remark,
                                @Param("operator") String operator);
}
