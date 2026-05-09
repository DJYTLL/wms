package com.example.wms.mapper.erp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.erp.ErpStockCount;
import org.apache.ibatis.annotations.Mapper;

// 库存盘点单 Mapper（ERP进销存）
@Mapper
public interface ErpStockCountMapper extends BaseMapper<ErpStockCount> {
}
