package com.example.wms.mapper.erp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.erp.ErpStockCountItem;
import org.apache.ibatis.annotations.Mapper;

// 库存盘点单明细 Mapper（ERP进销存）
@Mapper
public interface ErpStockCountItemMapper extends BaseMapper<ErpStockCountItem> {
}
