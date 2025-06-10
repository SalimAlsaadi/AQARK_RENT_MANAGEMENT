package com.AQARK.AQARK_RENT_MANAGEMENT.Controller;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.BuildingDTO.BuildingDetailsDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.BuildingDTO.BuildingSaveRequestDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityBuildings;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Implementation.BuildingServicesImpl;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface.BuildingServicesInterface;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/buildings")
public class BuildingsController {


    private final BuildingServicesInterface buildingService;

    @Autowired
    public BuildingsController(BuildingServicesImpl buildingService) {
        this.buildingService = buildingService;
    }


    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BuildingDetailsDTO> createBuilding(
            @RequestPart("building") String buildingJson,
            @RequestPart(value = "pictures", required = false) List<MultipartFile> images  ) throws IOException {

        // Manually convert JSON string to DTO
        ObjectMapper mapper = new ObjectMapper();
        BuildingSaveRequestDTO buildingDTO = mapper.readValue(buildingJson, BuildingSaveRequestDTO.class);

        return ResponseEntity.ok(buildingService.createBuilding(buildingDTO, images));
    }



    @PutMapping("")
    public ResponseEntity<BuildingDetailsDTO> updateBuilding(@RequestPart("building") BuildingDetailsDTO building,
                                                          @RequestPart(value = "imagesToDelete", required = false) List<String> imagesToDelete,
                                                          @RequestPart(value = "newImages", required = false) List<MultipartFile> newImages) throws IOException {

        building.setId(building.getId());
        BuildingDetailsDTO updated = buildingService.updateBuilding(building, imagesToDelete, newImages);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BuildingDetailsDTO> getBuildingById(@PathVariable Long id) {
        return ResponseEntity.ok(buildingService.getBuildingById(id));
    }

    @GetMapping
    public ResponseEntity<List<EntityBuildings>> getAllBuildings() {
        return ResponseEntity.ok(buildingService.getAllBuildings());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBuilding(@PathVariable Long id) {
        buildingService.deleteBuilding(id);
        return ResponseEntity.noContent().build();
    }
}
