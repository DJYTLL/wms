package com.example.wms.service.erp;

import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpCategoryCreateRequest;
import com.example.wms.dto.erp.ErpCategoryUpdateRequest;
import com.example.wms.entity.erp.ErpCategory;

import java.util.List;

// 分类服务接口（ERP进销存）
public interface ErpCategoryService {
    // 查询分类列表
    List<ErpCategory> listAll(String keyword, Boolean enabled);

    // 分页查询分类列表
    PageResponse<ErpCategory> page(long page, long size, String keyword, Boolean enabled);

    // 查询分类详情
    ErpCategory getById(Long id);

    // 新增分类
    ErpCategory create(ErpCategoryCreateRequest request);

    // 更新分类
    ErpCategory update(Long id, ErpCategoryUpdateRequest request);

    // 删除分类
    void delete(Long id);
}
