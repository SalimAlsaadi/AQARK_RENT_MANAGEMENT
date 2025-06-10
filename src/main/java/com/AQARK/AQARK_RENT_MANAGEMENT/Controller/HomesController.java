package com.AQARK.AQARK_RENT_MANAGEMENT.Controller;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.HomeDTO.HomeDetailsDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.HomeDTO.HomeSaveRequestDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface.HomeServiceInterface;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/homes")
public class HomesController {

    private final HomeServiceInterface homeService;

    @Autowired
    public HomesController(HomeServiceInterface homeService) {
        this.homeService = homeService;
    }

    @GetMapping
    public List<HomeSaveRequestDTO> getAllHomes() {
        return homeService.getAllHomes();
    }

    @GetMapping("/landlord/{landlordId}")
    public List<HomeDetailsDTO> getHomesByLandlord(@PathVariable Long landlordId) {
        return homeService.getHomesByLandlord(landlordId);
    }


    @PostMapping(value = "/save", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> saveHome(
            @RequestPart("homeData") String homeJson,
            @RequestPart(value = "pictures", required = false) List<MultipartFile> pictures
    ) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        HomeSaveRequestDTO home = mapper.readValue(homeJson, HomeSaveRequestDTO.class);
        return ResponseEntity.ok(homeService.saveHome(home,pictures));
    }


    @GetMapping("/{id}")
    public ResponseEntity<HomeDetailsDTO> getHomeById(@PathVariable Long id){
        return ResponseEntity.ok(homeService.getHomeById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteHomeById(@PathVariable Long id){
        return ResponseEntity.ok(homeService.deleteHomeById(id));
    }

    @PutMapping()
    public ResponseEntity<HomeDetailsDTO> updateHome(@RequestPart("home")HomeDetailsDTO home,
                                                     @RequestPart(value = "imagesToDelete", required = false) List<String> imagesToDelete,
                                                     @RequestPart(value = "newImages", required = false) List<MultipartFile> newImages) throws IOException {

        home.setId(home.getId());
        HomeDetailsDTO updated = homeService.updateHome(home, imagesToDelete, newImages);
        return ResponseEntity.ok(updated);
    }

}
