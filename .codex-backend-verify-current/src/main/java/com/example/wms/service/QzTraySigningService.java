package com.example.wms.service;

public interface QzTraySigningService {
    String getCertificatePem();

    String sign(String payload);
}
