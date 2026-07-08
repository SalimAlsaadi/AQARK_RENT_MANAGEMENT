package com.AQARK.AQARK_RENT_MANAGEMENT.Controller;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.BuildingDTO.BuildingDetailsDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.BuildingDTO.BuildingIdDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.BuildingDTO.BuildingListDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.BuildingDTO.BuildingUpdateRequestDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityBuildings;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface.BuildingServicesInterface;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface.PropertyCommonServiceInterface;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
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

@RestController
@RequestMapping("/api/buildings")
@RequiredArgsConstructor
public class BuildingsController {


    private final BuildingServicesInterface buildingService;
    private final PropertyCommonServiceInterface propertyCommonServiceInterface;
    private final ObjectMapper objectMapper;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final JwtDecoder jwtDecoder;


    @PostMapping("/getBuildingById")
    public BuildingDetailsDTO getBuildingById(@RequestBody BuildingIdDTO building) {
        return buildingService.getBuildingById(building.getBuildingId());
    }

    @PostMapping("/getBuildingsByLandlordId")
    public List<BuildingListDTO> getBuildingsByLandlordId(Authentication authentication) {

        Long landlordId = extractLandlordId(authentication);

        return buildingService.getBuildingsByLandlordId(landlordId);
    }

    @GetMapping("/my-buildings")
    public List<BuildingListDTO> getMyBuildings(Authentication authentication) {
        Long landlordId = extractLandlordId(authentication);

        return buildingService.getBuildingsByLandlordId(landlordId);
    }

    @GetMapping
    public List<BuildingListDTO> getAllBuildings() {
        return buildingService.getAllBuildings();
    }

    @PutMapping(
            value = "/update",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public BuildingDetailsDTO updateBuilding(
            @RequestPart("building") String buildingJson,
            @RequestPart(value = "picturesToDelete", required = false) String picturesToDeleteJson,
            @RequestPart(value = "newPictures", required = false) List<MultipartFile> newPictures
    ) throws IOException {

        BuildingUpdateRequestDTO dto = objectMapper.readValue(buildingJson, BuildingUpdateRequestDTO.class);

        List<Long> picturesToDelete = parsePicturesToDelete(picturesToDeleteJson);

        return buildingService.updateBuilding(
                dto,
                picturesToDelete,
                safePictures(newPictures)
        );
    }

    @DeleteMapping("/{id}")
    public void softDeleteBuilding(
            @PathVariable Long id
    ) {
        buildingService.softDeleteBuilding(id);
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

    private List<MultipartFile> safePictures(List<MultipartFile> pictures) {
        return pictures == null ? Collections.emptyList() : pictures;
    }

    private List<Long> parsePicturesToDelete(String value) throws IOException {
        if (value == null || value.trim().isEmpty()) {
            return Collections.emptyList();
        }

        return objectMapper.readValue(
                value,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Long.class)
        );
    }
}
