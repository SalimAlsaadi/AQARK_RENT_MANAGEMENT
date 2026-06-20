package com.AQARK.AQARK_RENT_MANAGEMENT.Security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class KeycloakWebClientConfig {

    @Value("${keycloak.issuer-uri:https://localhost:8443/realms/AQARK-realm}")
    private String keycloakIssuerUri;

    @Bean
    public WebClient keycloakWebClient() {
        return WebClient.builder()
                .baseUrl(keycloakIssuerUri)
                .build();
    }
}
