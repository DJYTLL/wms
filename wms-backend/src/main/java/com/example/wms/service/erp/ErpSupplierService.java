package com.example.wms.service.erp;

import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpSupplierCreateRequest;
import com.example.wms.dto.erp.ErpSupplierUpdateRequest;
import com.example.wms.entity.erp.ErpSupplier;

import java.util.List;

// 供应商服务接口（ERP进销存）
public interface ErpSupplierService {
    // 查询供应商列表
    List<ErpSupplier> listAll(String keyword, String contact, String phone, String status);

    // 分页查询供应商列表
    PageResponse<ErpSupplier> page(long page, long size, String keyword, String contact, String phone, String status);

    // 查询供应商详情
    ErpSupplier getById(Long id);

    // 获取下一个供应商编码
    String nextCode();

    // 新增供应商
    ErpSupplier create(ErpSupplierCreateRequest request);

    // 更新供应商
    ErpSupplier update(Long id, ErpSupplierUpdateRequest request);

    // 删除供应商
    void delete(Long id);
}
