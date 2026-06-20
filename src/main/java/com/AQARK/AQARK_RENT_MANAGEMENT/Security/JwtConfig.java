package com.AQARK.AQARK_RENT_MANAGEMENT.Security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.time.Instant;
import java.util.List;

/**
 * Defines the JwtDecoder bean separately to avoid a circular dependency
 * between SecurityConfig → RateLimitingFilter → JwtDecoder.
 */
@Configuration
public class JwtConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Value("${keycloak.client-id:spring-boot-app}")
    private String clientId;

    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

        OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(issuerUri);

        OAuth2TokenValidator<Jwt> audienceValidator =
            new JwtClaimValidator<List<String>>("aud",
                aud -> aud != null && aud.contains(clientId));

        OAuth2TokenValidator<Jwt> notBeforeValidator =
            new JwtClaimValidator<Instant>("nbf",
                nbf -> nbf == null || !Instant.now().isBefore(nbf));

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
            issuerValidator, audienceValidator, notBeforeValidator
        ));

        return decoder;
    }
}
