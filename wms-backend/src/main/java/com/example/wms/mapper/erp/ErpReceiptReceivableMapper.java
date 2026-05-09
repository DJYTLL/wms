package com.example.wms.mapper.erp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.erp.ErpReceiptReceivable;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

// ERP收款单-应收分摊 Mapper
@Mapper
public interface ErpReceiptReceivableMapper extends BaseMapper<ErpReceiptReceivable> {
    @Select("SELECT * FROM erp_receipt_receivable WHERE tenant_id = #{tenantId} AND receipt_id = #{receiptId}")
    List<ErpReceiptReceivable> findByReceiptId(@Param("tenantId") Long tenantId, @Param("receiptId") Long receiptId);

    @Select("SELECT * FROM erp_receipt_receivable WHERE tenant_id = #{tenantId} AND receivable_id = #{receivableId}")
    List<ErpReceiptReceivable> findByReceivableId(@Param("tenantId") Long tenantId, @Param("receivableId") Long receivableId);
}
