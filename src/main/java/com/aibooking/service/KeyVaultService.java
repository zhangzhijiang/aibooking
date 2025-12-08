package com.aibooking.service;

import com.azure.security.keyvault.secrets.SecretClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeyVaultService {

    private final SecretClient secretClient;
    
    @Value("${azure.keyvault.enabled:false}")
    private boolean keyVaultEnabled;

    public String getSecret(String secretName) {
        if (!keyVaultEnabled) {
            log.warn("Key Vault is disabled, returning null for secret: {}", secretName);
            return null;
        }
        
        try {
            return secretClient.getSecret(secretName).getValue();
        } catch (Exception e) {
            log.error("Error retrieving secret {} from Key Vault", secretName, e);
            return null;
        }
    }

    public void setSecret(String secretName, String secretValue) {
        if (!keyVaultEnabled) {
            log.warn("Key Vault is disabled, cannot set secret: {}", secretName);
            return;
        }
        
        try {
            secretClient.setSecret(secretName, secretValue);
            log.info("Secret {} set successfully", secretName);
        } catch (Exception e) {
            log.error("Error setting secret {} in Key Vault", secretName, e);
        }
    }
}

