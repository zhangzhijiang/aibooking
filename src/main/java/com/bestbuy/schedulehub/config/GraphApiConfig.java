package com.bestbuy.schedulehub.config;

import com.azure.core.credential.TokenCredential;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.requests.GraphServiceClient;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Slf4j
@Configuration
public class GraphApiConfig {

    @Value("${microsoft.graph.scope}")
    private String graphScope;

    @Bean
    public GraphServiceClient<Request> graphServiceClient(TokenCredential tokenCredential) {
        // Split scope string by spaces to support multiple scopes
        List<String> scopes = List.of(graphScope.split("\\s+"));
        
        log.info("Creating GraphServiceClient with scopes: {}", scopes);
        
        TokenCredentialAuthProvider authProvider = new TokenCredentialAuthProvider(scopes, tokenCredential);

        return GraphServiceClient.builder()
                .authenticationProvider(authProvider)
                .buildClient();
    }
}
