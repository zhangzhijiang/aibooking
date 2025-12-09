package com.bestbuy.schedulehub.config;

import com.azure.core.credential.TokenCredential;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.requests.GraphServiceClient;
import okhttp3.Request;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class GraphApiConfig {

    @Value("${microsoft.graph.scope}")
    private String graphScope;

    @Bean
    public GraphServiceClient<Request> graphServiceClient(TokenCredential tokenCredential) {
        TokenCredentialAuthProvider authProvider = new TokenCredentialAuthProvider(List.of(graphScope),
                tokenCredential);

        return GraphServiceClient.builder()
                .authenticationProvider(authProvider)
                .buildClient();
    }
}
