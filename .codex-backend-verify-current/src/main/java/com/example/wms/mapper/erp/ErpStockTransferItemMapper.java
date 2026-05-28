package com.example.wms.mapper.erp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.erp.ErpStockTransferItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ErpStockTransferItemMapper extends BaseMapper<ErpStockTransferItem> {
    @Select("SELECT * FROM erp_stock_transfer_item WHERE tenant_id = #{tenantId} AND transfer_id = #{transferId} AND deleted_at IS NULL ORDER BY line_no ASC")
    List<ErpStockTransferItem> findByTransferId(@Param("tenantId") Long tenantId, @Param("transferId") Long transferId);
}
