package com.AQARK.AQARK_RENT_MANAGEMENT.Controller;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.Landlord;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Implementation.LandlordServicesImpl;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface.LandlordServicesInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/landlord")
public class LandlordsController {

    @Autowired
    private final LandlordServicesInterface landlordServices;

    public LandlordsController(LandlordServicesImpl landlordServices){
        this.landlordServices=landlordServices;
    }

    @GetMapping
    public List<Landlord> getAllLandlords() {
        return landlordServices.getAllLandlords();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Landlord> getLandlordById(@PathVariable Long id) {
        Optional<Landlord> landlord = landlordServices.getLandlordById(id);
        return landlord.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Landlord> createLandlord(@Validated @RequestBody Landlord landlord) {
        return ResponseEntity.ok(landlordServices.createLandlord(landlord));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Landlord> updateLandlord(
            @PathVariable Long id,
            @Validated @RequestBody Landlord updatedLandlord
    ) {
        return ResponseEntity.ok(landlordServices.updateLandlord(id, updatedLandlord));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLandlord(@PathVariable Long id) {
        landlordServices.deleteLandlord(id);
        return ResponseEntity.noContent().build();
    }
}
