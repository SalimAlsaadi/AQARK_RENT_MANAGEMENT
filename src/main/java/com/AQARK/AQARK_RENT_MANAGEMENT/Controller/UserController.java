package com.AQARK.AQARK_RENT_MANAGEMENT.Controller;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.Landlord.LandlordSaveRequestDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.UserDTO.RegistrationUserDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityUser;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Implementation.UserServicesImpl;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface.UserServicesInterface;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/landlords")
public class UserController {


    private UserServicesInterface landlordServices;

    @Autowired
    public UserController(UserServicesImpl landlordServices){
        this.landlordServices=landlordServices;
    }
    public UserController(){}

    @GetMapping
    public List<EntityUser> getAllLandlords() {
        return landlordServices.getAllLandlords();
    }

    @GetMapping("/{id}")
    public ResponseEntity<EntityUser> getLandlordById(@PathVariable Long id) {
        Optional<EntityUser> landlord = landlordServices.getLandlordById(id);
        return landlord.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/register")
    public ResponseEntity<Long> registerLandlord(@Valid @RequestBody LandlordSaveRequestDTO dto) {
        Long id = landlordServices.createLandlordFromSAS(dto);
        return ResponseEntity.ok(id);
    }

    @PostMapping("/registerWithSAS")
    public ResponseEntity<String> registerWithSAS(@Valid @RequestBody RegistrationUserDTO registrationUserDTO){
        return ResponseEntity.ok(landlordServices.createUserWithSAS(registrationUserDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EntityUser> updateLandlord(@PathVariable Long id, @Validated @RequestBody EntityUser updatedEntityUser) {
        return ResponseEntity.ok(landlordServices.updateLandlord(id, updatedEntityUser));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLandlord(@PathVariable Long id) {
        landlordServices.deleteLandlord(id);
        return ResponseEntity.noContent().build();
    }
}
