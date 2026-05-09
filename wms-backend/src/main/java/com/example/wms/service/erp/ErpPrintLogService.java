package com.example.wms.service.erp;

import com.example.wms.dto.erp.ErpPrintLogCreateRequest;
import com.example.wms.entity.erp.ErpPrintLog;

import java.util.List;

// 打印日志服务（ERP进销存）
public interface ErpPrintLogService {
    ErpPrintLog record(ErpPrintLogCreateRequest request, String clientIp, String userAgent);

    List<ErpPrintLog> listByDoc(String docType, Long docId);
}
