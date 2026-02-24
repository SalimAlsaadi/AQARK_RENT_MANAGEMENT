package com.AQARK.AQARK_RENT_MANAGEMENT.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class UserInfoController {

    @GetMapping("/me")
    public Map<String, Object> me(@AuthenticationPrincipal Jwt jwt) {

        assert jwt.getIssuedAt() != null;
        assert jwt.getExpiresAt() != null;

        return Map.of(
                "subject", jwt.getSubject(),
                "issuedAt", jwt.getIssuedAt(),
                "expiresAt", jwt.getExpiresAt(),
                "claims", jwt.getClaims()
        );
    }
}

