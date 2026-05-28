package com.example.wms.service.impl;

import com.example.wms.service.QzTraySigningService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.Certificate;
import java.util.Base64;
import java.util.Enumeration;

@Service
public class QzTraySigningServiceImpl implements QzTraySigningService {
    private final ResourceLoader resourceLoader;

    @Value("${wms.qz.certificate-path:classpath:qz/qz-dev-cert.pem}")
    private String certificatePath;

    @Value("${wms.qz.keystore-path:classpath:qz/qz-dev.pfx}")
    private String keystorePath;

    @Value("${wms.qz.keystore-password:QzDevPfxPassword!2026}")
    private String keystorePassword;

    private String certificatePem;
    private PrivateKey privateKey;

    public QzTraySigningServiceImpl(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void initialize() {
        try {
            certificatePem = readText(certificatePath);
            privateKey = loadPrivateKey();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to initialize QZ Tray signing materials", ex);
        }
    }

    @Override
    public String getCertificatePem() {
        return certificatePem;
    }

    @Override
    public String sign(String payload) {
        try {
            Signature signature = Signature.getInstance("SHA512withRSA");
            signature.initSign(privateKey);
            signature.update(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to sign QZ Tray payload", ex);
        }
    }

    private String readText(String location) throws IOException {
        Resource resource = resourceLoader.getResource(location);
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private PrivateKey loadPrivateKey() throws Exception {
        Resource resource = resourceLoader.getResource(keystorePath);
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream inputStream = resource.getInputStream()) {
            keyStore.load(inputStream, keystorePassword.toCharArray());
        }

        Enumeration<String> aliases = keyStore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            if (!keyStore.isKeyEntry(alias)) {
                continue;
            }
            PrivateKey key = (PrivateKey) keyStore.getKey(alias, keystorePassword.toCharArray());
            Certificate certificate = keyStore.getCertificate(alias);
            if (key != null && certificate != null) {
                return key;
            }
        }
        throw new IllegalStateException("No private key entry found in QZ Tray keystore");
    }
}
