package com.example.wms.service.erp;

import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpLocationCreateRequest;
import com.example.wms.dto.erp.ErpLocationUpdateRequest;
import com.example.wms.entity.erp.ErpLocation;

import java.util.List;

// 库位服务接口（ERP进销存）
public interface ErpLocationService {
    // 查询库位列表
    List<ErpLocation> listAll(String keyword, Boolean enabled, Long warehouseId);

    // 分页查询库位列表
    PageResponse<ErpLocation> page(long page, long size, String keyword, Boolean enabled, Long warehouseId);

    // 查询库位详情
    ErpLocation getById(Long id);

    // 新增库位
    ErpLocation create(ErpLocationCreateRequest request);

    // 更新库位
    ErpLocation update(Long id, ErpLocationUpdateRequest request);

    // 删除库位
    void delete(Long id);
}
