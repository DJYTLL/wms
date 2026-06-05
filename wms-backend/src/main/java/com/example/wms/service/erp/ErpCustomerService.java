package com.example.wms.service.erp;

import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpCustomerImportBatchSummary;
import com.example.wms.dto.erp.ErpCustomerImportItemView;
import com.example.wms.dto.erp.ErpCustomerImportResult;
import com.example.wms.dto.erp.ErpCounterpartyUnbindCheck;
import com.example.wms.dto.erp.ErpCustomerCreateRequest;
import com.example.wms.dto.erp.ErpCustomerUpdateRequest;
import com.example.wms.entity.erp.ErpCustomer;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

// 客户服务接口（ERP进销存）
public interface ErpCustomerService {
    // 查询客户列表
    List<ErpCustomer> listAll(String keyword, String contact, String phone, Boolean enabled, Long categoryId);

    // 分页查询客户列表
    PageResponse<ErpCustomer> page(long page, long size, String keyword, String contact, String phone, Boolean enabled, Long categoryId);

    // 销售单客户筛选远程搜索
    List<ErpCustomer> searchOptions(String keyword, int size);

    // 查询客户详情
    ErpCustomer getById(Long id);

    // 获取下一个客户编码
    String nextCode();

    // 新增客户
    ErpCustomer create(ErpCustomerCreateRequest request);

    // 更新客户
    ErpCustomer update(Long id, ErpCustomerUpdateRequest request);

    ErpCustomerImportResult importCustomers(MultipartFile file, String sourceName);

    List<ErpCustomerImportBatchSummary> listImportBatches();

    List<ErpCustomerImportItemView> listImportBatchItems(Long batchId);

    // 改绑往来主体前校验
    ErpCounterpartyUnbindCheck checkRebind(Long id, Long targetSubjectId);

    // 删除客户
    void delete(Long id);
}
