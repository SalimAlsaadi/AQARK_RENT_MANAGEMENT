package com.AQARK.AQARK_RENT_MANAGEMENT.Security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${cors.allowed-origins:https://localhost:8443,https://localhost:8008,http://localhost:4200}")
    private List<String> allowedOrigins;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    @Value("${keycloak.issuer-uri:https://localhost:8443/realms/AQARK-realm}")
    private String keycloakIssuerUri;

    private final KeycloakJwtRolesConverter keycloakJwtRolesConverter;
    private final KeycloakOidcAuthoritiesMapper keycloakOidcAuthoritiesMapper;
    private final RateLimitingFilter rateLimitingFilter;
    private final RequestIdFilter requestIdFilter;
    private final JwtDecoder jwtDecoder;
    private final ClientRegistrationRepository clientRegistrationRepository;

    public SecurityConfig(KeycloakJwtRolesConverter keycloakJwtRolesConverter,
                          KeycloakOidcAuthoritiesMapper keycloakOidcAuthoritiesMapper,
                          RateLimitingFilter rateLimitingFilter,
                          RequestIdFilter requestIdFilter,
                          JwtDecoder jwtDecoder,
                          ClientRegistrationRepository clientRegistrationRepository) {
        this.keycloakJwtRolesConverter = keycloakJwtRolesConverter;
        this.keycloakOidcAuthoritiesMapper = keycloakOidcAuthoritiesMapper;
        this.rateLimitingFilter = rateLimitingFilter;
        this.requestIdFilter = requestIdFilter;
        this.jwtDecoder = jwtDecoder;
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    /**
     * Chain 1 — stateless Bearer-JWT resource server for non-browser / M2M clients.
     * Only matches requests that carry an {@code Authorization: Bearer ...} header, so
     * browser/session traffic falls through to the BFF chain below.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher(bearerTokenRequestMatcher())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers("/actuator/**").hasRole("ACTUATOR")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/**").authenticated()
                .anyRequest().denyAll()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .decoder(jwtDecoder)
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
                .authenticationEntryPoint(authenticationEntryPoint())
                .accessDeniedHandler(accessDeniedHandler())
            )
            .addFilterBefore(requestIdFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(rateLimitingFilter, RequestIdFilter.class)
            .headers(this::applySecurityHeaders);

        return http.build();
    }

    /**
     * Chain 2 — BFF for the browser/SPA. Handles the OIDC authorization-code login, keeps the
     * tokens in the server-side session, and authenticates subsequent requests via the session
     * cookie. Unauthenticated API requests get a 401 JSON response (no redirect) so the SPA can
     * decide when to start login by navigating the browser to /api/auth/login.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain bffSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/login").permitAll()
                .requestMatchers("/oauth2/authorization/**", "/login/oauth2/code/**").permitAll()
                .requestMatchers("/error").permitAll()
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers("/actuator/**").hasRole("ACTUATOR")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/**").authenticated()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .authorizationEndpoint(authz -> authz
                    .authorizationRequestResolver(pkceAuthorizationRequestResolver()))
                .userInfoEndpoint(userInfo -> userInfo
                    .userAuthoritiesMapper(keycloakOidcAuthoritiesMapper))
                .successHandler(loginSuccessHandler())
            )
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/api/auth/logout", "GET"))
                .logoutSuccessHandler(oidcLogoutSuccessHandler())
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
            )
            .exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint()))
            .addFilterBefore(requestIdFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(rateLimitingFilter, RequestIdFilter.class)
            .addFilterAfter(new CsrfCookieFilter(), UsernamePasswordAuthenticationFilter.class)
            .headers(this::applySecurityHeaders);

        return http.build();
    }

    /**
     * Matches requests carrying a Bearer token, routing them to the stateless resource-server chain.
     */
    private RequestMatcher bearerTokenRequestMatcher() {
        return request -> {
            String header = request.getHeader(HttpHeaders.AUTHORIZATION);
            return header != null && header.regionMatches(true, 0, "Bearer ", 0, 7);
        };
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(keycloakJwtRolesConverter);
        converter.setPrincipalClaimName("sub");
        return converter;
    }

    /**
     * Enables PKCE on the authorization request. The Keycloak client requires S256 PKCE
     * (pkce.code.challenge.method=S256), which Spring does not enable by default for
     * confidential clients.
     */
    private OAuth2AuthorizationRequestResolver pkceAuthorizationRequestResolver() {
        DefaultOAuth2AuthorizationRequestResolver resolver =
            new DefaultOAuth2AuthorizationRequestResolver(
                clientRegistrationRepository, "/oauth2/authorization");
        resolver.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());
        return resolver;
    }

    private SimpleUrlAuthenticationSuccessHandler loginSuccessHandler() {
        SimpleUrlAuthenticationSuccessHandler handler =
            new SimpleUrlAuthenticationSuccessHandler(frontendUrl);
        handler.setAlwaysUseDefaultTargetUrl(true);
        return handler;
    }

    /**
     * RP-initiated logout: redirect the browser to Keycloak's end-session endpoint (ending the
     * SSO session) with an id_token_hint, asking Keycloak to return to the frontend afterwards.
     * Built manually from the issuer because the provider is configured with explicit endpoints
     * (no OIDC discovery), so the end_session_endpoint is not auto-populated.
     */
    private LogoutSuccessHandler oidcLogoutSuccessHandler() {
        return (request, response, authentication) -> {
            String target = frontendUrl;
            if (authentication != null && authentication.getPrincipal() instanceof OidcUser oidcUser) {
                target = UriComponentsBuilder
                    .fromUriString(keycloakIssuerUri + "/protocol/openid-connect/logout")
                    .queryParam("id_token_hint", oidcUser.getIdToken().getTokenValue())
                    .queryParam("post_logout_redirect_uri", frontendUrl)
                    .build().toUriString();
            }
            response.sendRedirect(target);
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of(
            "Authorization", "Content-Type", "X-Requested-With",
            "X-Request-ID", "X-XSRF-TOKEN", "Accept", "Origin"
        ));
        config.setExposedHeaders(List.of("X-Request-ID", "X-Rate-Limit-Limit", "X-Rate-Limit-Remaining"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private void applySecurityHeaders(HeadersConfigurer<HttpSecurity> headers) {
        headers
            .httpStrictTransportSecurity(hsts -> hsts
                .maxAgeInSeconds(31536000)
                .includeSubDomains(true)
                .preload(true)
            )
            .frameOptions(frame -> frame.deny())
            .contentTypeOptions(cto -> {})
            .referrerPolicy(referrer ->
                referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN));
    }

    private AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            String requestId = MDC.get("requestId");
            log.warn("Authentication failed: requestId={} uri={} reason={}",
                requestId, request.getRequestURI(), authException.getMessage());

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(toJson(Map.of(
                "status", HttpStatus.UNAUTHORIZED.value(),
                "error", "Unauthorized",
                "message", "Authentication required",
                "requestId", requestId != null ? requestId : ""
            )));
        };
    }

    private AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            String requestId = MDC.get("requestId");
            log.warn("Access denied: requestId={} uri={}", requestId, request.getRequestURI());

            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(toJson(Map.of(
                "status", HttpStatus.FORBIDDEN.value(),
                "error", "Forbidden",
                "message", "Insufficient permissions",
                "requestId", requestId != null ? requestId : ""
            )));
        };
    }

    private String toJson(Map<String, Object> map) {
        try {
            return new ObjectMapper().writeValueAsString(map);
        } catch (Exception e) {
            return "{\"status\":500}";
        }
    }
}
