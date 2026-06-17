package com.AQARK.AQARK_RENT_MANAGEMENT.Controller;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.FlatDTO.*;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface.FlatsServicesInterface;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/flats")
@RequiredArgsConstructor
public class FlatsController {

    private final FlatsServicesInterface flatsService;
    private final ObjectMapper objectMapper;

    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public FlatDetailsDTO createFlat(
            @RequestPart("flat") String flatJson,
            @RequestPart(value = "pictures", required = false) List<MultipartFile> pictures,
            Authentication authentication
    ) throws IOException {

        FlatSaveRequestDTO dto =
                objectMapper.readValue(flatJson, FlatSaveRequestDTO.class);

        Long landlordId = extractRefId(authentication);

        return flatsService.createFlat(
                dto,
                safeFiles(pictures),
                landlordId
        );
    }

    @GetMapping("/{id}")
    public FlatDetailsDTO getFlatById(
            @PathVariable Long id
    ) {
        return flatsService.getFlatById(id);
    }

    @GetMapping("/building/{buildingId}")
    public List<FlatDetailsDTO> getFlatsByBuildingId(
            @PathVariable Long buildingId
    ) {
        return flatsService.getFlatsByBuildingId(buildingId);
    }

    @GetMapping("/building/{buildingId}/list")
    public List<FlatListDTO> getFlatListByBuildingId(
            @PathVariable Long buildingId
    ) {
        return flatsService.getFlatListByBuildingId(buildingId);
    }

    @GetMapping("/landlord/{landlordId}")
    public List<FlatDetailsDTO> getFlatsByLandlord(
            @PathVariable Long landlordId
    ) {
        return flatsService.getFlatsByLandlordId(landlordId);
    }

    @GetMapping("/my-flats")
    public List<FlatDetailsDTO> getMyFlats(
            Authentication authentication
    ) {
        Long landlordId = extractRefId(authentication);

        return flatsService.getFlatsByLandlordId(landlordId);
    }

    @PostMapping("/search")
    public List<FlatDetailsDTO> searchFlats(
            @RequestBody FlatSearchRequestDTO filter
    ) {
        return flatsService.searchFlats(filter);
    }

    @PutMapping(
            value = "/{id}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public FlatDetailsDTO updateFlat(
            @PathVariable Long id,
            @RequestPart("flat") String flatJson,
            @RequestPart(value = "newPictures", required = false) List<MultipartFile> newPictures,
            @RequestPart(value = "imagesToDelete", required = false) String imagesToDeleteJson
    ) throws IOException {

        FlatUpdateRequestDTO dto =
                objectMapper.readValue(flatJson, FlatUpdateRequestDTO.class);

        List<Long> imagesToDelete =
                parseIds(imagesToDeleteJson);

        return flatsService.updateFlat(
                id,
                dto,
                safeFiles(newPictures),
                imagesToDelete
        );
    }

    @DeleteMapping("/{id}")
    public void deleteFlat(
            @PathVariable Long id
    ) {
        flatsService.deleteFlat(id);
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

    private List<MultipartFile> safeFiles(List<MultipartFile> files) {
        return files == null ? Collections.emptyList() : files;
    }

    private List<Long> parseIds(String json) throws IOException {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }

        return objectMapper.readValue(
                json,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Long.class)
        );
    }
}