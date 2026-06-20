package com.AQARK.AQARK_RENT_MANAGEMENT.Security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Forces the deferred CSRF token to be loaded so its value is rendered into the
 * {@code XSRF-TOKEN} cookie on every request. With Spring Security 6 the token is loaded
 * lazily, which means a SPA never receives the cookie on safe (GET) requests. Calling
 * {@link CsrfToken#getToken()} here triggers the cookie write so Angular's HttpClient can
 * read it and echo it back as the {@code X-XSRF-TOKEN} header on mutating requests.
 */
public class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            // Render the token, which causes CookieCsrfTokenRepository to emit the cookie.
            csrfToken.getToken();
        }
        filterChain.doFilter(request, response);
    }
}
