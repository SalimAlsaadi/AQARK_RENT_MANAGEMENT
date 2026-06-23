package com.AQARK.AQARK_RENT_MANAGEMENT.Services.Implementation;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.UserDTO.*;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityUser;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityUserTypeMapping;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.UserType;
import com.AQARK.AQARK_RENT_MANAGEMENT.Repositories.UserRepository;
import com.AQARK.AQARK_RENT_MANAGEMENT.Repositories.UserTypeMappingRepository;
import com.AQARK.AQARK_RENT_MANAGEMENT.Repositories.UserTypeRepository;
import com.AQARK.AQARK_RENT_MANAGEMENT.Security.KeycloakAdminUserService;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface.UserServicesInterface;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class UserServicesImpl implements UserServicesInterface {

    /** Code of the landlord user type in the {@code user_types} table. */
    private static final String LANDLORD_CODE = "LANDLORD";

    private final UserRepository userRepository;
    private final KeycloakAdminUserService keycloakAdminUserService;
    private final UserTypeRepository userTypeRepository;
    private final UserTypeMappingRepository userTypeMappingRepository;

    public UserServicesImpl(UserRepository userRepository,
                            KeycloakAdminUserService keycloakAdminUserService,
                            UserTypeRepository userTypeRepository,
                            UserTypeMappingRepository userTypeMappingRepository) {
        this.userRepository = userRepository;
        this.keycloakAdminUserService = keycloakAdminUserService;
        this.userTypeRepository = userTypeRepository;
        this.userTypeMappingRepository = userTypeMappingRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDTO> getAllUsers() {
        log.info("Fetching all users");

        return userRepository.findAll()
                .stream()
                .map(this::userToDTO)
                .toList();
    }



    @Override
    @Transactional(readOnly = true)
    public GetUserByJwtDTO getUserById(Jwt jwt) {
        Long refId = jwt.getClaim("refId");
        String email=jwt.getClaim("username");

        if (refId == null) {
            throw new IllegalArgumentException("Invalid token: missing refId");
        }

        if(email==null){
            throw new IllegalArgumentException("Invalid token: missing email");
        }

        List<String> roles = jwt.getClaimAsStringList("roles");

        EntityUser user = userRepository.findById(refId)
                .orElseThrow(() -> new EntityNotFoundException("User not found for refId: " + refId));

        return userToDTOByJwt(user, roles, email);
    }



    @Override
    @Transactional
    public String createUserWithSAS(RegistrationUserDTO dto) {
        log.info("Creating AQARK user with SAS, email={}", dto.getEmail());

        validateNewUser(dto);

        List<UserType> userTypes = resolveUserTypes(dto.getUserType());
        validateCrNumber(dto, userTypes);

        EntityUser user = buildUser(dto);
        user = userRepository.save(user);

        saveUserTypes(user, userTypes);

        // Map each AQARK user type to its Keycloak realm role (e.g. LANDLORD -> ROLE_LANDLORD)
        // so the token carries the role and both the API and the SPA can authorize on it.
        List<String> realmRoles = userTypes.stream()
                .map(type -> "ROLE_" + type.getCode().toUpperCase())
                .toList();

        // Provision the identity in Keycloak. If this fails, the surrounding @Transactional
        // rolls back the AQARK user + type mappings so we don't leave a half-registered user.
        String keycloakId = keycloakAdminUserService.createUser(
                dto.getEmail(), dto.getFirstName(), dto.getLastName(), dto.getPassword(),
                user.getId(), realmRoles);

        log.info("User created successfully, aqarkUserId={}, keycloakId={}", user.getId(), keycloakId);

        return "User created successfully";
    }

    @Override
    @Transactional
    public String updateUser(Long id, UpdateUserDetailsDTO dto) {
        EntityUser user = getUserOrThrow(id);

        updateUserFields(user, dto);

        userRepository.save(user);

        keycloakAdminUserService.updateUser(user.getId(), dto.getEmail(), dto.getPassword());

        log.info("User updated his profile, userId={}", id);

        return "User details updated successfully";
    }

    @Override
    @Transactional
    public String updateUserByAdmin(UpdateUserDetailsDTO dto) {
        EntityUser user = getUserOrThrow(dto.getUserId());

        updateUserFields(user, dto);

        log.info("Admin updated user, userId={}", dto.getUserId());

        return "User details updated successfully";
    }

    @Override
    @Transactional
    public void deleteLandlord(Long id) {
        EntityUser user = getUserOrThrow(id);

        user.setIsActive(false);

        log.warn("User soft deleted/deactivated, userId={}", id);
    }

    private void validateNewUser(RegistrationUserDTO dto) {
        if (userRepository.existsByNationalId(dto.getNationalId())) {
            throw new IllegalStateException("User already exists with national ID: " + dto.getNationalId());
        }

        if (dto.getUserType() == null || dto.getUserType().isEmpty()) {
            throw new IllegalArgumentException("At least one user type is required");
        }
    }

    private EntityUser buildUser(RegistrationUserDTO dto) {
        return EntityUser.builder()
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .phoneNumber(dto.getPhoneNumber())
                .dateOfBirth(dto.getDateOfBirth())
                .address(dto.getAddress())
                .isActive(Boolean.TRUE.equals(dto.getIsActive()))
                .nationalId(dto.getNationalId())
                .crNumber(normalizeCrNumber(dto.getCrNumber()))
                .build();
    }

    /**
     * Loads the selected user types and fails if any id is unknown.
     */
    private List<UserType> resolveUserTypes(List<Integer> userTypeIds) {
        List<UserType> userTypes = userTypeRepository.findAllById(userTypeIds);

        if (userTypes.size() != userTypeIds.size()) {
            throw new EntityNotFoundException("One or more user types are invalid");
        }

        return userTypes;
    }

    /**
     * A CR number is mandatory and must be unique when the user registers as a landlord;
     * it is ignored for any other role (e.g. tenant).
     */
    private void validateCrNumber(RegistrationUserDTO dto, List<UserType> userTypes) {
        boolean isLandlord = userTypes.stream()
                .anyMatch(type -> LANDLORD_CODE.equalsIgnoreCase(type.getCode()));

        if (!isLandlord) {
            return;
        }

        String crNumber = normalizeCrNumber(dto.getCrNumber());
        if (crNumber == null) {
            throw new IllegalArgumentException("CR number is required to register as a landlord");
        }

        if (userRepository.existsByCrNumber(crNumber)) {
            throw new IllegalStateException("CR number already registered: " + crNumber);
        }
    }

    private String normalizeCrNumber(String crNumber) {
        if (crNumber == null || crNumber.isBlank()) {
            return null;
        }
        return crNumber.trim();
    }

    private void saveUserTypes(EntityUser user, List<UserType> userTypes) {
        List<EntityUserTypeMapping> mappings = userTypes.stream()
                .map(type -> EntityUserTypeMapping.builder()
                        .user(user)
                        .userType(type)
                        .isActive(true)
                        .build())
                .toList();

        userTypeMappingRepository.saveAll(mappings);
    }


    private EntityUser getUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User ID: " + id + " not found"));
    }

    private void updateUserFields(EntityUser user, UpdateUserDetailsDTO dto) {

       //  Check nationalId uniqueness (exclude current user)
        if (userRepository.existsByNationalIdAndIdNot(dto.getNationalId(), user.getId())) {
            throw new RuntimeException("National ID already used by another user");
        }


        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setAddress(dto.getAddress());
        user.setPhoneNumber(dto.getPhoneNumber());
        user.setIsActive(dto.getIsActive());
        user.setDateOfBirth(dto.getDateOfBirth());
        user.setNationalId(dto.getNationalId());
    }

    private GetUserByJwtDTO userToDTOByJwt(EntityUser user, List<String> roles, String email) {
        GetUserByJwtDTO dto = new GetUserByJwtDTO();

        dto.setId(user.getId());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setEmail(email);
        dto.setUserType(getUserTypeCodes(user.getId()));
        dto.setRoles(roles);

        return dto;
    }

    private UserDTO userToDTO(EntityUser user) {
        UserDTO dto = new UserDTO();

        dto.setId(user.getId());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setIsActive(user.getIsActive());
        dto.setUserType(getUserTypeCodes(user.getId()));

        return dto;
    }

    private List<String> getUserTypeCodes(Long userId) {
        return userTypeMappingRepository.findByUserIdAndIsActiveTrue(userId)
                .stream()
                .map(mapping -> mapping.getUserType().getCode())
                .toList();
    }
}