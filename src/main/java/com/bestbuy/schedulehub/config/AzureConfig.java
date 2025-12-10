package com.bestbuy.schedulehub.config;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;

@Slf4j
@Configuration
public class AzureConfig {

    @Value("${azure.activedirectory.tenant-id}")
    private String tenantId;

    @Value("${azure.activedirectory.client-id}")
    private String clientId;

    @Value("${azure.activedirectory.client-secret:}")
    private String clientSecret;

    @Value("${azure.activedirectory.authentication-mode:application}")
    private String authenticationMode;

    @PostConstruct
    public void logConfiguration() {
        log.info("═══════════════════════════════════════════════════════════════");
        log.info("🔧 Azure AD Configuration - Application Permissions");
        log.info("═══════════════════════════════════════════════════════════════");
        log.info("Tenant ID: {}", tenantId);
        log.info("Client ID: {}", clientId);
        log.info("Authentication Mode: Application (Service-Driven)");
        log.info("Architecture: Service Principal with Application Permissions");
        log.info("Client Secret: {} (length: {})",
                clientSecret != null && clientSecret.length() > 10
                        ? clientSecret.substring(0, 10) + "..."
                        : "null or empty",
                clientSecret != null ? clientSecret.length() : 0);

        // Validate client secret format
        if (clientSecret != null && !clientSecret.trim().isEmpty()) {
            // Azure client secrets are typically 40+ characters, not GUIDs
            if (clientSecret.matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")) {
                log.error("⚠️  WARNING: Client secret appears to be a GUID (Secret ID)!");
                log.error("   Azure client secrets should be long random strings (40+ characters)");
                log.error("   You may have copied the Secret ID instead of the Secret Value");
                log.error("   Go to Azure Portal → App Registration → Certificates & secrets");
                log.error("   Copy the 'Value' column, not the 'Secret ID' column");
            } else if (clientSecret.length() < 20) {
                log.warn("⚠️  WARNING: Client secret seems too short ({} characters)", clientSecret.length());
                log.warn("   Azure client secrets are typically 40+ characters long");
            }
        } else {
            log.error("❌ ERROR: Client secret is required for application authentication!");
        }

        if (!"application".equalsIgnoreCase(authenticationMode)) {
            log.warn("⚠️  WARNING: This application is designed for application permissions only.");
            log.warn("   Delegated authentication is not supported.");
            log.warn("   Setting authentication-mode to 'application' is recommended.");
        }

        log.info("═══════════════════════════════════════════════════════════════");
        log.info("✅ Service-Driven Architecture Enabled");
        log.info("   - No user interaction required");
        log.info("   - Can manage calendars for all users in tenant");
        log.info("   - Requires X-User-Id header in API requests");
        log.info("═══════════════════════════════════════════════════════════════");
    }

    @Bean
    public TokenCredential tokenCredential() {
        // This application only supports application authentication (service-driven)
        // It acts as a service principal with application permissions
        log.info("Creating Azure AD TokenCredential using Application Authentication (Client Credentials Flow)");
        log.info("Service-Driven Architecture: No user interaction required");

        if (clientSecret == null || clientSecret.trim().isEmpty()) {
            throw new IllegalStateException(
                    "Client secret is required for application authentication mode. " +
                            "This application uses application permissions and requires a client secret.");
        }

        return new ClientSecretCredentialBuilder()
                .tenantId(tenantId)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .build();
    }
}
