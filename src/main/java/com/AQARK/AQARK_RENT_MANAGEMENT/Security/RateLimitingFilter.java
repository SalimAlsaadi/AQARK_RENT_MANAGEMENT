package com.AQARK.AQARK_RENT_MANAGEMENT.Security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final int CAPACITY = 100;
    private static final long REFILL_INTERVAL_MS = 60_000L;

    private final ConcurrentHashMap<String, BucketEntry> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String key = resolveKey(request);
        int remaining = tryConsume(key);

        if (remaining < 0) {
            log.warn("Rate limit exceeded for key={} uri={}", key, request.getRequestURI());
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", "60");
            response.setHeader("X-Rate-Limit-Limit", String.valueOf(CAPACITY));
            response.setHeader("X-Rate-Limit-Remaining", "0");
            response.getWriter().write("{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Try again in 60 seconds.\"}");
            return;
        }

        response.setHeader("X-Rate-Limit-Limit", String.valueOf(CAPACITY));
        response.setHeader("X-Rate-Limit-Remaining", String.valueOf(remaining));
        chain.doFilter(request, response);
    }

    private String resolveKey(HttpServletRequest request) {
        // Key by the authenticated principal when available (works for both the Bearer-token
        // chain and the session/oauth2Login chain), otherwise fall back to the client IP.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName())) {
            return "user:" + authentication.getName();
        }
        return "ip:" + getClientIp(request);
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private int tryConsume(String key) {
        BucketEntry bucket = buckets.computeIfAbsent(key, k ->
                new BucketEntry(new AtomicInteger(CAPACITY), new AtomicLong(Instant.now().toEpochMilli())));

        long now = Instant.now().toEpochMilli();
        long lastRefill = bucket.lastRefillTime().get();

        if (now - lastRefill >= REFILL_INTERVAL_MS
                && bucket.lastRefillTime().compareAndSet(lastRefill, now)) {
            bucket.tokens().set(CAPACITY);
        }

        int current;
        do {
            current = bucket.tokens().get();
            if (current <= 0) return -1;
        } while (!bucket.tokens().compareAndSet(current, current - 1));

        return current - 1;
    }

    private record BucketEntry(AtomicInteger tokens, AtomicLong lastRefillTime) {}
}
