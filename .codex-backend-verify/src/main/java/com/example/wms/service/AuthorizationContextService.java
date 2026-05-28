package com.example.wms.service;

import com.example.wms.dto.AuthorizationContextResponse;

public interface AuthorizationContextService {
    AuthorizationContextResponse getCurrent();
}
