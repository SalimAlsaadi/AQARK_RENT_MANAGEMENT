package com.AQARK.AQARK_RENT_MANAGEMENT.Controller;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.BuildingDTO.BuildingIdDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.FlatDTO.*;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.HomeDTO.HomeIdDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Exceptions.ApiResponse;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface.FlatsServicesInterface;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
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
import java.util.Map;

@RestController
@RequestMapping("/api/flats")
@RequiredArgsConstructor
public class FlatsController {

    private final FlatsServicesInterface flatsService;
    private final ObjectMapper objectMapper;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public FlatDetailsDTO createFlat(@RequestPart("flat") String flatJson, @RequestPart(value = "pictures", required = false) List<MultipartFile> pictures, Authentication authentication) throws IOException {

        FlatSaveRequestDTO dto = objectMapper.readValue(flatJson, FlatSaveRequestDTO.class);

        Long landlordId = extractRefId(authentication);

        return flatsService.createFlat(
                dto,
                safeFiles(pictures),
                landlordId
        );
    }

    @PostMapping("/getFlatById")
    public FlatDetailsDTO getFlatById(@RequestBody FlatIdDTO flat) {
        return flatsService.getFlatById(flat.getFlatId());
    }

    @PostMapping("/getFlatsByBuildingId")
    public List<FlatDetailsDTO> getFlatsByBuildingId(@RequestBody BuildingIdDTO building) {
        return flatsService.getFlatsByBuildingId(building.getBuildingId());
    }

    @PostMapping("/getFlatsByHomeId")
    public List<FlatDetailsDTO> getFlatsByHomeId(@RequestBody HomeIdDTO home){
        return flatsService.getFlatsByHomeId(home.getHomeId());
    }

    @PostMapping("/getFlatListByBuildingId")
    public List<FlatListDTO> getFlatListByBuildingId(@RequestBody BuildingIdDTO building) {
        return flatsService.getFlatListByBuildingId(building.getBuildingId());
    }

    @PostMapping("/getFlatListByHomeId")
    public List<FlatListDTO> getFlatListByHomeId(@RequestBody HomeIdDTO home){
        return flatsService.getFlatListByHomeId(home.getHomeId());
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

    @PostMapping(value = "/updateFlat", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public FlatDetailsDTO updateFlat(
            @RequestPart("flat") String flatJson,
            @RequestPart(value = "newImages", required = false) List<MultipartFile> newPictures,
            @RequestPart(value = "imageIdsToDelete", required = false) String imagesToDeleteJson
    ) throws IOException {

        FlatUpdateRequestDTO dto = objectMapper.readValue(flatJson, FlatUpdateRequestDTO.class);

        List<Long> imagesToDelete = parseIds(imagesToDeleteJson);

        return flatsService.updateFlat(dto, safeFiles(newPictures), imagesToDelete);
    }

    @PostMapping("/deleteFlat")
    public ResponseEntity<?> deleteFlat(@RequestBody FlatIdDTO flat) {
        flatsService.deleteFlat(flat.getFlatId());
        return ResponseEntity.ok(
                ApiResponse.success("Flat deleted successfully",  null)
        );

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