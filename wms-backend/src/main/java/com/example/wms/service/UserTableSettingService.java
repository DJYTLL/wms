package com.example.wms.service;

import com.example.wms.dto.UserTableSettingRequest;
import com.example.wms.dto.UserTableSettingResponse;

public interface UserTableSettingService {
    UserTableSettingResponse getByPageKey(String pageKey);

    UserTableSettingResponse update(String pageKey, UserTableSettingRequest request);
}
