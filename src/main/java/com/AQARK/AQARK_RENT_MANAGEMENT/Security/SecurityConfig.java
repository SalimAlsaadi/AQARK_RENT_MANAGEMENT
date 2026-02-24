package com.AQARK.AQARK_RENT_MANAGEMENT.Security;

import jakarta.servlet.http.Cookie;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

@Configuration
public class SecurityConfig {

    // ======================================================
    //  MAIN SECURITY FILTER CHAIN (RESOURCE SERVER)
    // ======================================================
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                // --------------------------------------------------
                // CORS (COOKIE-BASED AUTH REQUIRES THIS)
                // --------------------------------------------------
                .cors(cors -> cors.configurationSource(req -> {
                    CorsConfiguration c = new CorsConfiguration();
                    c.setAllowCredentials(true);
                    c.setAllowedOrigins(List.of("http://localhost:4200"));
                    c.setAllowedHeaders(List.of("*"));
                    c.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                    return c;
                }))

                // --------------------------------------------------
                // CSRF
                // --------------------------------------------------
                // For stateless JWT APIs, disabling CSRF is correct.
                // (Keycloak does the same for APIs)
                .csrf(AbstractHttpConfigurer::disable)

                // --------------------------------------------------
                // AUTHORIZATION RULES
                // --------------------------------------------------
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/landlords/register").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/api/me").fullyAuthenticated()
                        .anyRequest().permitAll()
                )

                // --------------------------------------------------
                // OAUTH2 RESOURCE SERVER (JWT IN COOKIE)
                // --------------------------------------------------
                .oauth2ResourceServer(oauth -> oauth
                        // 🔥 CRITICAL: cookie → JWT bridge
                        .bearerTokenResolver(bearerTokenResolver())
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                );

        return http.build();
    }

    // ======================================================
    // JWT → AUTHORITIES MAPPING
    // ======================================================
    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {

        // scopes -> SCOPE_xxx
        JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();

        // roles[] -> ROLE_xxx
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthorityPrefix("ROLE_");
        roles.setAuthoritiesClaimName("roles");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            var authorities = new java.util.ArrayList<>(scopes.convert(jwt));
            authorities.addAll(roles.convert(jwt));
            return authorities;
        });

        return converter;
    }

    // ======================================================
    // COOKIE → BEARER TOKEN RESOLVER (KEYCLOAK STYLE)
    // ======================================================
    @Bean
    BearerTokenResolver bearerTokenResolver() {
        return request -> {
            if (request.getCookies() == null) return null;

            for (Cookie cookie : request.getCookies()) {
                if ("SAS_TOKEN".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
            return null;
        };
    }
}
