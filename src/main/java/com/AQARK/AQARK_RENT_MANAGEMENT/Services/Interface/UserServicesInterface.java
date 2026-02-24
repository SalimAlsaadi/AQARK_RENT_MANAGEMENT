package com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.Landlord.LandlordSaveRequestDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.UserDTO.RegistrationUserDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityUser;

import java.util.List;
import java.util.Optional;

public interface UserServicesInterface {

    List<EntityUser> getAllLandlords();

    Optional<EntityUser> getLandlordById(Long id);

    Long createLandlordFromSAS(LandlordSaveRequestDTO dto);

    EntityUser updateLandlord(Long id, EntityUser updatedEntityUser);

    void deleteLandlord(Long id);

    String createUserWithSAS(RegistrationUserDTO registrationUserDTO);
}
