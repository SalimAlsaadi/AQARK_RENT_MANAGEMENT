package com.AQARK.AQARK_RENT_MANAGEMENT.Controller;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.LocationDTO.LocationLookupDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.LocationDTO.RegionIdDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.LocationDTO.WilayaIdDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface.OmanLocationServiceInterface;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lookups/oman")
@RequiredArgsConstructor
public class OmanLocationController {

    private final OmanLocationServiceInterface locationService;

    @GetMapping("/regions")
    public List<LocationLookupDTO> getRegions() {
        return locationService.getRegions();
    }

    @PostMapping("/regions/wilayas")
    public List<LocationLookupDTO> getWilayas(@RequestBody RegionIdDTO region) {
        return locationService.getWilayasByRegionId(region.getRegionId());
    }

    @PostMapping("/wilayas/areas")
    public List<LocationLookupDTO> getAreas(@RequestBody WilayaIdDTO wilaya) {
        return locationService.getAreasByWilayaId(wilaya.getWilayaId());
    }
}