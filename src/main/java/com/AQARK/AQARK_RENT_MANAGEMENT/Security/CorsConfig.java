package com.AQARK.AQARK_RENT_MANAGEMENT.Security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.time.Duration;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource(SecurityProperties props) {

        return request -> {
            CorsConfiguration config = new CorsConfiguration();

            config.setAllowedOrigins(props.getCors().getAllowedOrigins());
            config.setAllowedMethods(props.getCors().getAllowedMethods());
            config.setAllowedHeaders(props.getCors().getAllowedHeaders());

            config.setAllowCredentials(props.getCors().isAllowCredentials());
            config.setMaxAge(props.getCors().getMaxAge());

            return config;
        };
    }
}