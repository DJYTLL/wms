package com.example.wms.controller;

import com.example.wms.dto.ApiResponse;
import com.example.wms.service.QzTraySigningService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/integrations/qz")
public class QzTrayController {
    private final QzTraySigningService qzTraySigningService;

    public QzTrayController(QzTraySigningService qzTraySigningService) {
        this.qzTraySigningService = qzTraySigningService;
    }

    @GetMapping("/certificate")
    public ResponseEntity<ApiResponse<QzCertificateResponse>> getCertificate() {
        return ResponseEntity.ok(ApiResponse.ok(new QzCertificateResponse(qzTraySigningService.getCertificatePem())));
    }

    @PostMapping("/sign")
    public ResponseEntity<ApiResponse<QzSignatureResponse>> sign(@Valid @RequestBody QzSignatureRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(new QzSignatureResponse(qzTraySigningService.sign(request.payload()))));
    }
}

record QzCertificateResponse(String certificate) {
}

record QzSignatureRequest(@NotBlank String payload) {
}

record QzSignatureResponse(String signature) {
}
