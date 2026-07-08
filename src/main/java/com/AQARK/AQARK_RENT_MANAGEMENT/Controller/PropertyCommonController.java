package com.AQARK.AQARK_RENT_MANAGEMENT.Controller;

import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface.PropertyCommonServiceInterface;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/properties")
@RequiredArgsConstructor
public class PropertyCommonController {

    private final PropertyCommonServiceInterface propertyCommonServiceInterface;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final JwtDecoder jwtDecoder;

    @PostMapping(value = "/createProperties", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createProperty(@RequestPart("propertyType") String propertyType,
                                            @RequestPart("data") String data,
                                            @RequestPart(value = "pictures", required = false) List<MultipartFile> pictures,
                                            Authentication authentication) throws IOException {

        Long landlordId = extractLandlordId(authentication);

        Object result = propertyCommonServiceInterface.create(propertyType, data, pictures == null ? Collections.emptyList() : pictures, landlordId);

        return ResponseEntity.ok(result);
    }


    private Long extractLandlordId(Authentication authentication) {

        Jwt jwt = resolveAccessToken(authentication);

        String refId = jwt.getClaimAsString("refId");

        if (refId == null || refId.isBlank()) {
            throw new IllegalStateException(
                    "refId claim is missing from the Keycloak access token."
            );
        }

        try {
            return Long.parseLong(refId);
        } catch (NumberFormatException exception) {
            throw new IllegalStateException(
                    "Invalid refId claim value in Keycloak access token: " + refId,
                    exception
            );
        }
    }

    private Jwt resolveAccessToken(Authentication authentication) {

        if (authentication instanceof OAuth2AuthenticationToken oauth2Authentication) {

            OAuth2AuthorizedClient authorizedClient =
                    authorizedClientService.loadAuthorizedClient(
                            oauth2Authentication.getAuthorizedClientRegistrationId(),
                            oauth2Authentication.getName()
                    );

            if (authorizedClient == null || authorizedClient.getAccessToken() == null) {
                throw new IllegalStateException(
                        "No Keycloak access token found for the current authenticated session."
                );
            }

            return jwtDecoder.decode(
                    authorizedClient.getAccessToken().getTokenValue()
            );
        }

        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            return jwtAuthentication.getToken();
        }

        throw new IllegalStateException(
                "Unsupported authentication type: " +
                        (authentication == null
                                ? "null"
                                : authentication.getClass().getName())
        );
    }
}