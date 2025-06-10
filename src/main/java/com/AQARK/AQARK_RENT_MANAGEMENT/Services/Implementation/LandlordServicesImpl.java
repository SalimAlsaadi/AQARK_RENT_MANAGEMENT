package com.AQARK.AQARK_RENT_MANAGEMENT.Services.Implementation;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.Landlord;
import com.AQARK.AQARK_RENT_MANAGEMENT.Repositories.LandlordRepository;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface.LandlordServicesInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class LandlordServicesImpl implements LandlordServicesInterface {

    @Autowired
    private LandlordRepository landlordRepository;

    public LandlordServicesImpl(LandlordRepository landlordRepository){
        this.landlordRepository=landlordRepository;
    }

    public List<Landlord> getAllLandlords() {
        return landlordRepository.findAll();
    }

    public Optional<Landlord> getLandlordById(Long id) {
        return landlordRepository.findById(id);
    }

    public Landlord createLandlord(Landlord landlord) {
        return landlordRepository.save(landlord);
    }

    public Landlord updateLandlord(Long id, Landlord updatedLandlord) {
        return landlordRepository.findById(id).map(landlord -> {
            landlord.setFirstName(updatedLandlord.getFirstName());
            landlord.setLastName(updatedLandlord.getLastName());
            landlord.setEmail(updatedLandlord.getEmail());
            landlord.setPhoneNumber(updatedLandlord.getPhoneNumber());
            landlord.setDateOfBirth(updatedLandlord.getDateOfBirth());
            landlord.setAddress(updatedLandlord.getAddress());
            landlord.setNationalId(updatedLandlord.getNationalId());
            landlord.setIsActive(updatedLandlord.getIsActive());
            return landlordRepository.save(landlord);
        }).orElseThrow(() -> new RuntimeException("Landlord not found with id " + id));
    }

    public void deleteLandlord(Long id) {
        landlordRepository.deleteById(id);
    }
}
