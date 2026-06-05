package com.example.wms.service.erp;

import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpStockBalanceOption;
import com.example.wms.dto.erp.ErpStockOccupancyView;
import com.example.wms.entity.erp.ErpStockBalance;
import com.example.wms.entity.erp.ErpStockTxn;

import java.math.BigDecimal;
import java.util.List;

// 库存服务接口（ERP进销存）
public interface ErpStockService {
    // 分页查询库存余额
    PageResponse<ErpStockBalance> pageBalance(long page, long size, Long productId, Long warehouseId, Long locationId);

    // 分页查询库存流水
    PageResponse<ErpStockTxn> pageTxn(long page, long size, String bizType, Long bizId, Long productId);

    // 查询指定商品的库存明细（仓库-库位维度）
    List<ErpStockBalanceOption> listBalancesByProduct(Long productId);

    // 查询库存台账行的占用明细
    List<ErpStockOccupancyView> listOccupancy(Long balanceId);

    // 查询指定范围内的现存量
    BigDecimal getQtyOnHand(Long productId, Long warehouseId, Long locationId);
}
