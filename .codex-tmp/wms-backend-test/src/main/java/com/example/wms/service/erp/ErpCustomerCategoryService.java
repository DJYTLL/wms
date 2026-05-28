package com.example.wms.service.erp;

import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpCustomerCategoryCreateRequest;
import com.example.wms.dto.erp.ErpCustomerCategoryUpdateRequest;
import com.example.wms.entity.erp.ErpCustomerCategory;

import java.util.List;

// 客户类别服务接口（ERP进销存）
public interface ErpCustomerCategoryService {
    // 查询客户类别列表
    List<ErpCustomerCategory> listAll(String keyword, Boolean enabled);

    // 分页查询客户类别
    PageResponse<ErpCustomerCategory> page(long page, long size, String keyword, Boolean enabled);

    // 查询客户类别详情
    ErpCustomerCategory getById(Long id);

    // 获取下一个客户类别编码
    String nextCode();

    // 新增客户类别
    ErpCustomerCategory create(ErpCustomerCategoryCreateRequest request);

    // 更新客户类别
    ErpCustomerCategory update(Long id, ErpCustomerCategoryUpdateRequest request);

    // 删除客户类别
    void delete(Long id);
}
