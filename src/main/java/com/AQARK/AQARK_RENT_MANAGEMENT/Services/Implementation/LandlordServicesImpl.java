package com.AQARK.AQARK_RENT_MANAGEMENT.Services.Implementation;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.Landlord.LandlordSaveRequestDTO;
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

    public Long createLandlord(LandlordSaveRequestDTO dto) {
        Landlord landlord = new Landlord();

        landlord.setFirstName(dto.getFirstName());
        landlord.setLastName(dto.getLastName());
        landlord.setEmail(dto.getEmail());
        landlord.setPhoneNumber(dto.getPhoneNumber());
        landlord.setDateOfBirth(dto.getDateOfBirth());
        landlord.setAddress(dto.getAddress());
        landlord.setNationalId(dto.getNationalId());
        landlord.setIsActive(dto.getIsActive());

        Landlord saved = landlordRepository.save(landlord);
        return saved.getId();
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
