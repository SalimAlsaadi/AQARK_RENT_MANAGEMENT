package com.AQARK.AQARK_RENT_MANAGEMENT.Services.Implementation;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.UserDTO.*;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityUser;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityUserTypeMapping;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.UserType;
import com.AQARK.AQARK_RENT_MANAGEMENT.Repositories.UserRepository;
import com.AQARK.AQARK_RENT_MANAGEMENT.Repositories.UserTypeMappingRepository;
import com.AQARK.AQARK_RENT_MANAGEMENT.Repositories.UserTypeRepository;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface.UserServicesInterface;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServicesImpl implements UserServicesInterface {

    private final UserRepository userRepository;
    private final WebClient webClient;
    private final UserTypeRepository userTypeRepository;
    private final UserTypeMappingRepository userTypeMappingRepository;

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

        EntityUser user = buildUser(dto);
        user = userRepository.save(user);

        saveUserTypes(user, dto.getUserType());

        Long sasUserId = createUserInSas(user, dto);

        user.setSasUserId(sasUserId);
        userRepository.save(user);

        log.info("User created successfully, aqarkUserId={}, sasUserId={}", user.getId(), sasUserId);

        return "User created successfully";
    }

    @Override
    @Transactional
    public String updateUser(Long id, UpdateUserDetailsDTO dto) {
        EntityUser user = getUserOrThrow(id);

        updateUserFields(user, dto);

        userRepository.save(user);

        updateUserInSas(user, dto.getPassword(), dto.getEmail());

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
        if (userRepository.existsByNationalId(dto.getEmail())) {
            throw new IllegalStateException("User already exists with email: " + dto.getEmail());
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
                .build();
    }

    private void saveUserTypes(EntityUser user, List<Integer> userTypeIds) {
        List<UserType> userTypes = userTypeRepository.findAllById(userTypeIds);

        if (userTypes.size() != userTypeIds.size()) {
            throw new EntityNotFoundException("One or more user types are invalid");
        }

        List<EntityUserTypeMapping> mappings = userTypes.stream()
                .map(type -> EntityUserTypeMapping.builder()
                        .user(user)
                        .userType(type)
                        .isActive(true)
                        .build())
                .toList();

        userTypeMappingRepository.saveAll(mappings);
    }


    private Long createUserInSas(EntityUser user, RegistrationUserDTO dto) {
        try {
            RegistrationWithSASDTO request = new RegistrationWithSASDTO();
            request.setEmail(dto.getEmail());
            request.setPassword(dto.getPassword());
            request.setRefId(user.getId());
            request.setRoles(Set.of("PUBLIC_USER"));
            request.setAllowedClientIds(Set.of("AQARK-client"));

            ApiResponseDTO<Long> response = webClient.post()
                    .uri("https://localhost:9443/api/admin/users/service")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponseDTO<Long>>() {})
                    .block();

            if (response == null || !response.isSuccess() || response.getData() == null) {
                throw new IllegalStateException(
                        response != null ? response.getMessage() : "Empty response from SAS"
                );
            }

            return response.getData();

        } catch (Exception ex) {
            log.error("Failed to create user in SAS, aqarkUserId={}, email={}", user.getId(), dto.getEmail(), ex);
            throw new RuntimeException("Failed to create user in SAS", ex);
        }
    }

    private void updateUserInSas(EntityUser user, String password, String email) {
        try {
            UpdateWithSASDTO request = new UpdateWithSASDTO();
            request.setEmail(email);
            request.setPassword(password);
            request.setRefId(user.getId());
            request.setRoles("PUBLIC_USER");
            request.setAllowedClientIds("AQARK-client");

            ApiResponseDTO<Long> response = webClient.put()
                    .uri("https://localhost:9443/api/admin/users/service")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponseDTO<Long>>() {})
                    .block();

            if (response == null || !response.isSuccess() || response.getData() == null) {
                throw new IllegalStateException(
                        response != null ? response.getMessage() : "Empty response from SAS"
                );
            }

        } catch (Exception ex) {
            log.error("Failed to create user in SAS, aqarkUserId={}, email={}", user.getId(), email, ex);
            throw new RuntimeException("Failed to create user in SAS", ex);
        }
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