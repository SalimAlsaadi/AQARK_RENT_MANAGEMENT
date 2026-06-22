package com.AQARK.AQARK_RENT_MANAGEMENT.Controller;

import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface.PropertyCommonServiceInterface;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/properties")
@RequiredArgsConstructor
public class PropertyCommonController {

    private final PropertyCommonServiceInterface propertyCommonServiceInterface;

    @PostMapping(
            value = "/createProperties",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> createProperty(
            @RequestPart("propertyType") String propertyType,
            @RequestPart("data") String data,
            @RequestPart(value = "pictures", required = false) List<MultipartFile> pictures,
            Authentication authentication
    ) throws IOException {

        Long landlordId = extractLandlordId(authentication);

        Object result = propertyCommonServiceInterface.create(
                propertyType,
                data,
                pictures == null ? Collections.emptyList() : pictures,
                landlordId
        );

        return ResponseEntity.ok(result);
    }

    private Long extractLandlordId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new IllegalStateException("Invalid authentication.");
        }

        Object refId = jwt.getClaim("refId");

        if (refId == null) {
            throw new IllegalStateException("Missing refId in token.");
        }

        if (refId instanceof Number number) {
            return number.longValue();
        }

        return Long.parseLong(refId.toString());
    }
}