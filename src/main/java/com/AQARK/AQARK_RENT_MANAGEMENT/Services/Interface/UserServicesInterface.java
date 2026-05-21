package com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.UserDTO.*;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

public interface UserServicesInterface {

    List<UserDTO> getAllUsers();

    GetUserByJwtDTO getUserById(@AuthenticationPrincipal Jwt jwt);

    //Long createLandlordFromSAS(LandlordSaveRequestDTO dto);

    String updateUser(Long id, UpdateUserDetailsDTO updateUserDetailsDTO);

    String updateUserByAdmin( UpdateUserDetailsDTO userDetailsDTO);

    void deleteLandlord(Long id);

    String createUserWithSAS(RegistrationUserDTO registrationUserDTO);

}
