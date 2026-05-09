package com.example.wms.service.erp;

import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpCustomerCreateRequest;
import com.example.wms.dto.erp.ErpCustomerUpdateRequest;
import com.example.wms.entity.erp.ErpCustomer;

import java.util.List;

// 客户服务接口（ERP进销存）
public interface ErpCustomerService {
    // 查询客户列表
    List<ErpCustomer> listAll(String keyword, Boolean enabled, Long categoryId);

    // 分页查询客户列表
    PageResponse<ErpCustomer> page(long page, long size, String keyword, Boolean enabled, Long categoryId);

    // 查询客户详情
    ErpCustomer getById(Long id);

    // 获取下一个客户编码
    String nextCode();

    // 新增客户
    ErpCustomer create(ErpCustomerCreateRequest request);

    // 更新客户
    ErpCustomer update(Long id, ErpCustomerUpdateRequest request);

    // 删除客户
    void delete(Long id);
}
