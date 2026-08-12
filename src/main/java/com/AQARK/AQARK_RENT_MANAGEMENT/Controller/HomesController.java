package com.AQARK.AQARK_RENT_MANAGEMENT.Controller;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.HomeDTO.*;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.RoomDTO.RoomDetailsDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface.HomeServiceInterface;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface.RoomsServicesInterface;
import jakarta.validation.Valid;
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
import java.util.List;

@RestController
@RequestMapping("/api/houses")
@RequiredArgsConstructor
public class HomesController {

    private final HomeServiceInterface homeService;
    private final RoomsServicesInterface roomsServic;

    private final OAuth2AuthorizedClientService authorizedClientService;
    private final JwtDecoder jwtDecoder;

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<HomeDetailsDTO> updateHome(@Valid @RequestPart("homeData") HomeUpdateRequestDTO dto,
                                                     @RequestPart(value = "newImages", required = false) List<MultipartFile> newImages,
                                                     @RequestPart(value = "imageIdsToDelete", required = false) List<Long> imageIdsToDelete,
                                                     Authentication authentication ) throws IOException {

        Long landlordId = extractLandlordId(authentication);

        return ResponseEntity.ok(homeService.updateHome(dto, newImages, imageIdsToDelete, landlordId));
    }

    @PatchMapping("/status")
    public ResponseEntity<HomeDetailsDTO> changeHomeStatus(@Valid @RequestBody HomeChangeStatusRequestDTO dto, Authentication authentication) {

        Long landlordId = extractLandlordId(authentication);

        return ResponseEntity.ok(homeService.changeHomeStatus(dto, landlordId));
    }

    @PostMapping("/getHomeById")
    public ResponseEntity<HomeDetailsDTO> getHomeById(@RequestBody HomeIdDTO home, Authentication authentication) {

        Long landlordId = extractLandlordId(authentication);

        return ResponseEntity.ok(homeService.getHomeById(home.getHomeId(), landlordId));
    }

    @PostMapping("/getHomesByLandlordId")
    public ResponseEntity<List<HomeListDTO>> getMyHomes(Authentication authentication) {

        Long landlordId = extractLandlordId(authentication);

        return ResponseEntity.ok(homeService.getHomesByLandlord(landlordId));
    }

    @PostMapping("/getRoomsByHomeId")
    public ResponseEntity<List<RoomDetailsDTO>> getRoomsByHomeId(@RequestBody HomeIdDTO dto) {
        return ResponseEntity.ok(roomsServic.getRoomsByHomeId(dto.getHomeId()));
    }

    @GetMapping
    public ResponseEntity<List<HomeListDTO>> getAllHomes() {

        return ResponseEntity.ok(homeService.getAllHomes());
    }

    @DeleteMapping("/{homeId}")
    public ResponseEntity<Void> deleteHome(@PathVariable Long homeId, Authentication authentication) {

        Long landlordId = extractLandlordId(authentication);

        homeService.deleteHome(homeId, landlordId);

        return ResponseEntity.noContent().build();
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