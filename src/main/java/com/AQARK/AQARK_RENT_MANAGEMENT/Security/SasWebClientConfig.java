package com.AQARK.AQARK_RENT_MANAGEMENT.Security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class SasWebClientConfig {

    @Bean
    public WebClient sasWebClient() {
        return WebClient.builder()
                .baseUrl("https://localhost:9443") // SAS server URL
                .build();
    }
}