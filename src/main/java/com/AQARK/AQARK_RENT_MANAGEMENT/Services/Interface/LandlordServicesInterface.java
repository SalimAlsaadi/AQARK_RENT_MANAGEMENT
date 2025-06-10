package com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.Landlord;

import java.util.List;
import java.util.Optional;

public interface LandlordServicesInterface {

    List<Landlord> getAllLandlords();

    Optional<Landlord> getLandlordById(Long id);

    Landlord createLandlord(Landlord landlord);

    Landlord updateLandlord(Long id, Landlord updatedLandlord);

    void deleteLandlord(Long id);
}
