package com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.Landlord.LandlordSaveRequestDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.Landlord;

import java.util.List;
import java.util.Optional;

public interface LandlordServicesInterface {

    List<Landlord> getAllLandlords();

    Optional<Landlord> getLandlordById(Long id);

    Long createLandlord(LandlordSaveRequestDTO dto);

    Landlord updateLandlord(Long id, Landlord updatedLandlord);

    void deleteLandlord(Long id);
}
