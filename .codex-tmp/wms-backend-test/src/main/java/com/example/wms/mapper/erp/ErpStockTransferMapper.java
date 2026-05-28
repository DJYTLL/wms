package com.example.wms.mapper.erp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.erp.ErpStockTransfer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ErpStockTransferMapper extends BaseMapper<ErpStockTransfer> {
    @Select("SELECT * FROM erp_stock_transfer WHERE tenant_id = #{tenantId} AND transfer_no = #{transferNo} AND deleted_at IS NULL")
    ErpStockTransfer findByTransferNo(@Param("tenantId") Long tenantId, @Param("transferNo") String transferNo);

    @Select("SELECT * FROM erp_stock_transfer WHERE tenant_id = #{tenantId} AND id = #{id} AND deleted_at IS NULL FOR UPDATE")
    ErpStockTransfer findByIdForUpdate(@Param("tenantId") Long tenantId, @Param("id") Long id);
}
