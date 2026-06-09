package com.AQARK.AQARK_RENT_MANAGEMENT.Controller;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.BuildingDTO.BuildingDetailsDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.BuildingDTO.BuildingIdDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.BuildingDTO.BuildingSaveRequestDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityBuildings;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Implementation.BuildingServicesImpl;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface.BuildingServicesInterface;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface.PropertyCommonServiceInterface;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/buildings")
@RequiredArgsConstructor
public class BuildingsController {


    private final BuildingServicesInterface buildingService;
    private final PropertyCommonServiceInterface propertyCommonServiceInterface;

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

    @PostMapping("/getAllBuildingByLandlordId")
    public ResponseEntity<List<BuildingDetailsDTO>> getAllBuildingsByLandlordId(Authentication authentication) throws IOException {
        Jwt jwt = (Jwt) authentication.getPrincipal();

        Long landlordId = jwt.getClaim("refId");

        return ResponseEntity.ok(buildingService.getBuildingByLandlordID(landlordId));

    }

//    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    public ResponseEntity<BuildingDetailsDTO> createBuilding(
//            @RequestPart("building") String buildingJson,
//            @RequestPart(value = "pictures", required = false) List<MultipartFile> images  ) throws IOException {
//
//        // Manually convert JSON string to DTO
//        ObjectMapper mapper = new ObjectMapper();
//        BuildingSaveRequestDTO buildingDTO = mapper.readValue(buildingJson, BuildingSaveRequestDTO.class);
//
//        return ResponseEntity.ok(buildingService.createBuilding(buildingDTO, images));
//    }



    @PutMapping("")
    public ResponseEntity<BuildingDetailsDTO> updateBuilding(@RequestPart("building") BuildingDetailsDTO building,
                                                          @RequestPart(value = "imagesToDelete", required = false) List<Long> imagesToDelete,
                                                          @RequestPart(value = "newImages", required = false) List<MultipartFile> newImages) throws IOException {

        building.setId(building.getId());
        BuildingDetailsDTO updated = buildingService.updateBuilding(building, imagesToDelete, newImages);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/getBuildingById")
    public ResponseEntity<BuildingDetailsDTO> getBuildingById(@RequestBody BuildingIdDTO dto) {
        return ResponseEntity.ok(buildingService.getBuildingById(dto.getBuildingId()));
    }

    @GetMapping
    public ResponseEntity<List<EntityBuildings>> getAllBuildings() {
        return ResponseEntity.ok(buildingService.getAllBuildings());
    }

//    @DeleteMapping("/{id}")
//    public ResponseEntity<Void> deleteBuilding(@PathVariable Long id) {
//        buildingService.deleteBuilding(id);
//        return ResponseEntity.noContent().build();
//    }
}
