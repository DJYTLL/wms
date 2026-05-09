package com.example.wms.service.erp;

import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpUnitCreateRequest;
import com.example.wms.dto.erp.ErpUnitUpdateRequest;
import com.example.wms.entity.erp.ErpUnit;

import java.util.List;

// 单位服务接口（ERP进销存）
public interface ErpUnitService {
    // 查询单位列表
    List<ErpUnit> listAll(String keyword, Boolean enabled);

    // 分页查询单位列表
    PageResponse<ErpUnit> page(long page, long size, String keyword, Boolean enabled);

    // 查询单位详情
    ErpUnit getById(Long id);

    // 新增单位
    ErpUnit create(ErpUnitCreateRequest request);

    // 更新单位
    ErpUnit update(Long id, ErpUnitUpdateRequest request);

    // 删除单位
    void delete(Long id);
}
