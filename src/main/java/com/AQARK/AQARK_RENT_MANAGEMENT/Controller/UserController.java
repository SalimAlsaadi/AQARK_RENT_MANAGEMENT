package com.AQARK.AQARK_RENT_MANAGEMENT.Controller;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.UserDTO.*;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Implementation.UserServicesImpl;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface.UserServicesInterface;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {


    private UserServicesInterface userServices;

    @Autowired
    public UserController(UserServicesImpl landlordServices){
        this.userServices=landlordServices;

    }

    @PostMapping("/getAllUsers")
    @PreAuthorize("hasRole('AQARK_ADMIN')")
    public ResponseEntity<List<UserDTO>> getAllUsers() {

        log.info("Fetching all users");

        List<UserDTO> users = userServices.getAllUsers();

        return ResponseEntity.ok(users);
    }

    @PostMapping("/getUser")
    public ResponseEntity<GetUserByJwtDTO> getUserByJwt(@AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity.ok(userServices.getUserById(jwt));
    }

//    @PostMapping("/register")
//    public ResponseEntity<Long> registerLandlord(@Valid @RequestBody LandlordSaveRequestDTO dto) {
//        Long id = landlordServices.createLandlordFromSAS(dto);
//        return ResponseEntity.ok(id);
//    }

    @PostMapping("/registerWithSAS")
    public ResponseEntity<Map<String, Object>> registerWithSAS(
            @Valid @RequestBody RegistrationUserDTO request
    ) {
        log.info("Registering user with SAS, email= "+ request.getEmail());

        String result = userServices.createUserWithSAS(request);

        log.info("User registered successfully with SAS, email= "+ request.getEmail());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", result
        ));
    }

    @PutMapping("/updateUserDetailsByUser")
    public ResponseEntity<Map<String, Object>> updateCurrentUser(@Valid @RequestBody UpdateUserDetailsDTO request, @AuthenticationPrincipal Jwt jwt) {
        Long refId = getRefId(jwt);

        log.info("User update requested by refId= "+refId);

        String result = userServices.updateUser(refId, request);

        log.info("User updated successfully by refId= "+refId);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", result
        ));
    }


    @PutMapping("/updateUserDetailsByAdmin")
    @PreAuthorize("hasRole('AQARK_ADMIN')")
    public ResponseEntity<String> updateUserByAdmin(@RequestBody UpdateUserDetailsDTO updateUserDetailsDTO) {
        return ResponseEntity.ok(userServices.updateUserByAdmin(updateUserDetailsDTO));
    }

    @DeleteMapping("/deleteUserByID")
    @PreAuthorize("hasRole('AQARK_ADMIN')")
    public ResponseEntity<Void> deleteUser(@RequestBody UserIdDTO userId) {
        log.info("Delete user requested for userId= "+userId);

        userServices.deleteLandlord(userId.getUserId());

        log.warn("User deleted/deactivated successfully, userId=  "+ userId);

        return ResponseEntity.noContent().build();
    }

     //helper method to get userId from token
    private Long getRefId(Jwt jwt) {
        Long refId = jwt.getClaim("refId");

        if (refId == null) {
            log.error("JWT does not contain required claim refId");
            throw new IllegalArgumentException("Invalid token: missing refId");
        }

        return refId;
    }

    private String getEmail(Jwt jwt) {
        String email = jwt.getClaim("username");

        if (email == null) {
            log.error("JWT does not contain required claim UserName");
            throw new IllegalArgumentException("Invalid token: missing refId");
        }

        return email;
    }
}
