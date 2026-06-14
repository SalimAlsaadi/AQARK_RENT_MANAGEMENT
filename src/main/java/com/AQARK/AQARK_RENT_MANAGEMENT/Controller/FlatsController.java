package com.AQARK.AQARK_RENT_MANAGEMENT.Controller;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.BuildingDTO.BuildingIdDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.FlatDTO.*;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface.FlatsServicesInterface;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/flats")
public class FlatsController {

    private final FlatsServicesInterface flatsService;

    @Autowired
    public FlatsController(FlatsServicesInterface flatsService) {
        this.flatsService = flatsService;
    }

//    @PostMapping
//    public ResponseEntity<FlatDetailsDTO> saveFlat(
//            @RequestPart("flatData") String flatDataJson,
//            @RequestPart(value = "pictures", required = false) List<MultipartFile> pictures
//    ) throws IOException {
//
//        // Convert JSON string to DTO
//        ObjectMapper mapper = new ObjectMapper();
//        FlatSaveRequestDTO flatDTO = mapper.readValue(flatDataJson, FlatSaveRequestDTO.class);
//
//        FlatDetailsDTO result = flatsService.createFlat(flatDTO, pictures);
//        return ResponseEntity.ok(result);
//    }


//    // Get all flats
//    @GetMapping("/all")
//    public ResponseEntity<List<FlatsForShowListDTO>> getAllFlats() {
//        return ResponseEntity.ok(flatsService.getAllHostels());
//    }

    // Search flats
//    @PostMapping("/search")
//    public ResponseEntity<List<FlatsForShowListDTO>> searchFlats(@RequestBody HostelSearchRequestDTO search) {
//        return ResponseEntity.ok(flatsService.getHostelBySearch(search));
//    }

    // Get flats by landlord ID
    @GetMapping("/landlord/{landlordId}")
    public ResponseEntity<List<FlatDetailsDTO>> getFlatsByLandlord(@PathVariable Long landlordId) {
        return ResponseEntity.ok(flatsService.getFlatsByLandlordId(landlordId));
    }

    @PostMapping("/getFlatsByBuildingId")
    public ResponseEntity<List<FlatDetailsDTO>> getFlatsByBuildingId(@RequestBody BuildingIdDTO buildingIdD){

        return ResponseEntity.ok(flatsService.getFlatsByBuildingId(buildingIdD.getBuildingId()));

    }

    @PostMapping("/getFlatListByBuildingId")
    public ResponseEntity<List<FlatListDTO>> getFlatListByBuildingId(@RequestBody BuildingIdDTO buildingId){
        return ResponseEntity.ok(flatsService.getFlatsList(buildingId));
    }


    @PostMapping("/getFlatById")
    public ResponseEntity<FlatDetailsDTO> getFlatById(@RequestBody FlatIdDTO flat){
        return ResponseEntity.ok(flatsService.getFlatById(flat.getFlatId()));
    }

    // Update flat (including images)
    @PutMapping()
    public ResponseEntity<FlatDetailsDTO> updateFlat(
            @RequestPart("flatData") FlatSaveRequestDTO flatDTO,
            @RequestPart(value = "pictures", required = false) List<MultipartFile> newPictures,
            @RequestParam(value = "imagesToDelete", required = false) List<Long> imagesToDelete) throws IOException {

        FlatDetailsDTO result = flatsService.updateFlat(flatDTO, newPictures, imagesToDelete);
        return ResponseEntity.ok(result);
    }

    // Delete flat
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteFlat(@PathVariable Long id) {
        flatsService.deleteFlat(id);
        return ResponseEntity.ok("Flat deleted successfully.");
    }
}
