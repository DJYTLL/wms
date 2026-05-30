package com.example.wms.service.erp.support;

import java.util.List;
import java.util.Map;

public record ExcelImportSheet(List<String> headers, List<Map<String, String>> rows) {
}
