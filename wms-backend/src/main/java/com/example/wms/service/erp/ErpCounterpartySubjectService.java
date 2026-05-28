package com.example.wms.service.erp;

import com.example.wms.dto.PageResponse;
import com.example.wms.dto.erp.ErpCounterpartySubjectCreateRequest;
import com.example.wms.dto.erp.ErpCounterpartySubjectUpdateRequest;
import com.example.wms.entity.erp.ErpCounterpartySubject;
import com.example.wms.entity.erp.ErpCounterpartySubjectLink;

import java.util.List;

// 往来主体服务接口（ERP进销存）
public interface ErpCounterpartySubjectService {
    List<ErpCounterpartySubject> listAll(String keyword, Boolean enabled);

    PageResponse<ErpCounterpartySubject> page(long page, long size, String keyword, Boolean enabled);

    ErpCounterpartySubject getById(Long id);

    ErpCounterpartySubject create(ErpCounterpartySubjectCreateRequest request);

    ErpCounterpartySubject update(Long id, ErpCounterpartySubjectUpdateRequest request);

    void delete(Long id);

    ErpCounterpartySubjectLink bindSupplier(Long id, Long supplierId, Boolean primary, String remark);

    ErpCounterpartySubjectLink bindCustomer(Long id, Long customerId, Boolean primary, String remark);
}
