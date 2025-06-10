package com.AQARK.AQARK_RENT_MANAGEMENT.Controller;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.FlatDTO.FlatSaveRequestDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.FlatDTO.HostelSearchRequestDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.FlatDTO.FlatsForShowListDTO;
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

    @PostMapping
    public ResponseEntity<String> saveFlat(
            @RequestPart("flatData") String flatDataJson,
            @RequestPart(value = "pictures", required = false) List<MultipartFile> pictures
    ) throws IOException {

        // Convert JSON string to DTO
        ObjectMapper mapper = new ObjectMapper();
        FlatSaveRequestDTO flatDTO = mapper.readValue(flatDataJson, FlatSaveRequestDTO.class);

        String result = flatsService.saveFlats(flatDTO, pictures);
        return ResponseEntity.ok(result);
    }


    // Get all flats
    @GetMapping("/all")
    public ResponseEntity<List<FlatsForShowListDTO>> getAllFlats() {
        return ResponseEntity.ok(flatsService.getAllHostels());
    }

    // Search flats
    @PostMapping("/search")
    public ResponseEntity<List<FlatsForShowListDTO>> searchFlats(@RequestBody HostelSearchRequestDTO search) {
        return ResponseEntity.ok(flatsService.getHostelBySearch(search));
    }

    // Get flats by landlord ID
    @GetMapping("/landlord/{landlordId}")
    public ResponseEntity<List<FlatsForShowListDTO>> getFlatsByLandlord(@PathVariable Long landlordId) {
        return ResponseEntity.ok(flatsService.getHostelByLandlordID(landlordId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FlatsForShowListDTO> getFlatById(@PathVariable long id){
        return ResponseEntity.ok(flatsService.getFlatById(id));
    }

    // Update flat (including images)
    @PutMapping()
    public ResponseEntity<String> updateFlat(
            @RequestPart("flatData") FlatSaveRequestDTO flatDTO,
            @RequestPart(value = "pictures", required = false) List<MultipartFile> newPictures,
            @RequestParam(value = "imagesToDelete", required = false) List<String> imagesToDelete) throws IOException {

        String result = flatsService.updateFlat(flatDTO, newPictures, imagesToDelete);
        return ResponseEntity.ok(result);
    }

    // Delete flat
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteFlat(@PathVariable Long id) {
        flatsService.deleteFlat(id);
        return ResponseEntity.ok("Flat deleted successfully.");
    }
}
