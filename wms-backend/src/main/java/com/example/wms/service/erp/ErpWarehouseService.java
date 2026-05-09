package com.example.wms.service.erp;

import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpWarehouseCreateRequest;
import com.example.wms.dto.erp.ErpWarehouseUpdateRequest;
import com.example.wms.entity.erp.ErpWarehouse;

import java.util.List;

// 仓库服务接口（ERP进销存）
public interface ErpWarehouseService {
    // 查询仓库列表
    List<ErpWarehouse> listAll(String keyword, Boolean enabled);

    // 分页查询仓库列表
    PageResponse<ErpWarehouse> page(long page, long size, String keyword, Boolean enabled);

    // 查询仓库详情
    ErpWarehouse getById(Long id);

    // 新增仓库
    ErpWarehouse create(ErpWarehouseCreateRequest request);

    // 更新仓库
    ErpWarehouse update(Long id, ErpWarehouseUpdateRequest request);

    // 删除仓库
    void delete(Long id);
}
