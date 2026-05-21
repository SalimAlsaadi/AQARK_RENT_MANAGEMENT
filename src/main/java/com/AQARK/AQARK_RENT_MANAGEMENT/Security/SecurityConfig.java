package com.AQARK.AQARK_RENT_MANAGEMENT.Security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityProperties props, JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {

        http
                .cors(Customizer.withDefaults())

                // If you use HttpOnly cookies, CSRF depends:
                // - If you ONLY call APIs from your SPA with JWT cookie => better to ENABLE CSRF with token
                // - If you are purely stateless and accept only Bearer/cookie JWT => can disable but be careful
                .csrf(csrf -> csrf.disable())

                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> {
                    props.getEndpoints().getPublicEndpoints()
                            .forEach(p -> auth.requestMatchers(p).permitAll());

                    props.getEndpoints().getAuthenticatedEndpoints()
                            .forEach(p -> auth.requestMatchers(p).authenticated());

                    auth.anyRequest().authenticated(); // ✅ deny-by-default
                })

                .oauth2ResourceServer(oauth -> oauth
                        .bearerTokenResolver(new CookieBearerTokenResolver(props.getCookie().getTokenName()))
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                )

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> res.sendError(401, "Unauthorized"))
                        .accessDeniedHandler((req, res, e) -> res.sendError(403, "Forbidden"))
                )

                .headers(headers -> headers
                        .httpStrictTransportSecurity(Customizer.withDefaults())
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'"))
                );

        return http.build();
    }
}