package com.example.wms.mapper.erp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.erp.ErpSaleOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

// 销售单 Mapper（ERP进销存）
@Mapper
public interface ErpSaleOrderMapper extends BaseMapper<ErpSaleOrder> {
    // 按单号查询
    @Select("SELECT * FROM erp_sale_order WHERE tenant_id = #{tenantId} AND order_no = #{orderNo} AND deleted_at IS NULL")
    ErpSaleOrder findByOrderNo(@Param("tenantId") Long tenantId, @Param("orderNo") String orderNo);

    @Select("""
        UPDATE erp_sale_order
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
    ErpSaleOrder approveDraft(@Param("tenantId") Long tenantId,
                              @Param("id") Long id,
                              @Param("operator") String operator);

    @Select("""
        UPDATE erp_sale_order
        SET status = 'RED_FLUSHED',
            red_flush_source_type = 'SALE_ORDER',
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
    ErpSaleOrder redFlushApproved(@Param("tenantId") Long tenantId,
                                  @Param("id") Long id,
                                  @Param("remark") String remark);
}
