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
import org.springframework.security.oauth2.jwt.Jwt;
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

    @PostMapping( value = "/createProperties", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createProperty(
            @RequestPart("propertyType") String propertyType,
            @RequestPart("data") String data,
            @RequestPart(value = "pictures", required = false) List<MultipartFile> pictures,
            Authentication authentication) throws IOException {

        Jwt jwt = (Jwt) authentication.getPrincipal();

        Long landlordId = jwt.getClaim("refId");

        return ResponseEntity.ok(propertyCommonServiceInterface.create(propertyType, data, pictures, landlordId));
    }

    @PutMapping("/getBuildingById")
    public BuildingDetailsDTO getBuildingById(@RequestBody BuildingIdDTO building) {
        return buildingService.getBuildingById(building.getBuildingId());
    }

    @PostMapping("/getBuildingsByLandlordId")
    public List<BuildingListDTO> getBuildingsByLandlordId(Authentication authentication) {

        Long landlordId = extractRefId(authentication);

        return buildingService.getBuildingsByLandlordId(landlordId);
    }

    @GetMapping("/my-buildings")
    public List<BuildingListDTO> getMyBuildings(Authentication authentication) {
        Long landlordId = extractRefId(authentication);

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

    private Long extractRefId(Authentication authentication) {
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
