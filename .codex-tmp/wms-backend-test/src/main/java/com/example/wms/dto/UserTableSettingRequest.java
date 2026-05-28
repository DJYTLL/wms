package com.example.wms.dto;

import java.util.Map;

// 用户表格配置请求
public record UserTableSettingRequest(
    Map<String, Object> config
) {
}
