package com.AQARK.AQARK_RENT_MANAGEMENT.Controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * BFF authentication endpoints.
 *
 * <p>Login and logout are driven by Spring Security's {@code oauth2Login()} / {@code logout()}
 * configuration; the browser must reach those via full-page navigation (not XHR), because the
 * OIDC flow involves cross-origin redirects to Keycloak that an XHR cannot follow.
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String AUTHORIZATION_ENDPOINT = "/oauth2/authorization/keycloak";

    /**
     * Entry point for the browser to start login. Navigating here (full page) sends the user
     * into the Keycloak authorization-code flow. After login Spring redirects back to the
     * configured frontend URL.
     */
    @GetMapping("/login")
    public void login(HttpServletResponse response) throws IOException {
        response.sendRedirect(AUTHORIZATION_ENDPOINT);
    }

    /**
     * Returns the currently authenticated user so the SPA can determine login state.
     * Returns 401 (handled by the security entry point) when there is no session.
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(@AuthenticationPrincipal OidcUser user,
                                                  Authentication authentication) {
        if (user == null || authentication == null) {
            return ResponseEntity.status(401).build();
        }

        // Read authorities from the Authentication (these carry the realm roles mapped by
        // KeycloakOidcAuthoritiesMapper); OidcUser.getAuthorities() keeps the unmapped originals.
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return ResponseEntity.ok(Map.of(
                "username", user.getPreferredUsername() != null ? user.getPreferredUsername() : user.getSubject(),
                "email", user.getEmail() != null ? user.getEmail() : "",
                "name", user.getFullName() != null ? user.getFullName() : "",
                "roles", roles
        ));
    }
}
