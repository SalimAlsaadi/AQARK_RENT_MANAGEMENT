package com.AQARK.AQARK_RENT_MANAGEMENT.Services.Implementation;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.Landlord.LandlordSaveRequestDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.UserDTO.RegistrationUserDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.UserDTO.RegistrationWithSASDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityUser;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.RefType;
import com.AQARK.AQARK_RENT_MANAGEMENT.Repositories.RefTypeRepository;
import com.AQARK.AQARK_RENT_MANAGEMENT.Repositories.UserRepository;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface.UserServicesInterface;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class UserServicesImpl implements UserServicesInterface {


    private final UserRepository userRepository;
    private final WebClient webClient;
    private final RefTypeRepository refTypeRepository;

    @Autowired
    public UserServicesImpl(UserRepository userRepository, WebClient webClient, RefTypeRepository refTypeRepository){
        this.userRepository = userRepository;
        this.webClient = webClient;
        this.refTypeRepository = refTypeRepository;
    }

    @Override
    public List<EntityUser> getAllLandlords() {
        return userRepository.findAll();
    }

    @Override
    public Optional<EntityUser> getLandlordById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public Long createLandlordFromSAS(LandlordSaveRequestDTO dto) {    //request registration user from SAS system,
                                                                       // and this function called to save new user in aqark DB and return
        EntityUser user = new EntityUser();

        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setEmail(dto.getEmail());
        user.setPhoneNumber(dto.getPhoneNumber());
        user.setDateOfBirth(dto.getDateOfBirth());
        user.setAddress(dto.getAddress());
        user.setNationalId(dto.getNationalId());
        user.setIsActive(dto.getIsActive());

        RefType refType=refTypeRepository.findById(dto.getRef_type()).
                                     orElseThrow(()-> new EntityNotFoundException("this ref_type: "+dto.getRef_type() + " not found"));

        user.setRefType(refType);
        EntityUser saved = userRepository.save(user);
        return saved.getId();
    }


    @Transactional
    @Override
    public String createUserWithSAS(RegistrationUserDTO dto) {

        RefType refType=refTypeRepository.findById(dto.getRefTypeId()).
                orElseThrow(()-> new EntityNotFoundException("RefType not found: " + dto.getRefTypeId()));

        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalStateException("User already exists with email: " + dto.getEmail());
        }

        // 1️⃣ Save users in AQARK
        EntityUser user = EntityUser.builder()
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .email(dto.getEmail())
                .phoneNumber(dto.getPhoneNumber())
                .dateOfBirth(dto.getDateOfBirth())
                .address(dto.getAddress())
                .isActive(dto.getIsActive())
                .nationalId(dto.getNationalId())
                .refType(refType)
                .build();

        user = userRepository.save(user);

        try {

            // ================================
            // 4️⃣ PREPARE SAS REQUEST
            // ================================
            RegistrationWithSASDTO request = new RegistrationWithSASDTO();
            request.setEmail(user.getEmail());
            request.setPassword(dto.getPassword());
            request.setRefId(user.getId());
            request.setRoles(Set.of("PUBLIC_USER"));
            request.setAllowedClientIds(Set.of("AQARK-client"));

            // ================================
            // 5️⃣ CALL SAS SYSTEM
            // ================================
            Long sasUserId = webClient.post()
                    .uri("https://localhost:9443/admin/users/service")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Long.class)
                    .block();

            // ================================
            // 6️⃣ LINK SAS ID BACK
            // ================================
            user.setSasUserId(sasUserId);
            userRepository.save(user);

            return "User created successfully with SAS ID: " + sasUserId;

        } catch (Exception ex){
            // ================================
            // if sas not complete save the user then ROLLBACK AQARK USER and delete from aqark user table
            // ================================
            userRepository.delete(user);

            throw new RuntimeException("Failed to create user in SAS. Local user rolled back.", ex);
        }

    }



    @Override
    public EntityUser updateLandlord(Long id, EntityUser updatedEntityUser) {
        return userRepository.findById(id).map(landlord -> {
            landlord.setFirstName(updatedEntityUser.getFirstName());
            landlord.setLastName(updatedEntityUser.getLastName());
            landlord.setEmail(updatedEntityUser.getEmail());
            landlord.setPhoneNumber(updatedEntityUser.getPhoneNumber());
            landlord.setDateOfBirth(updatedEntityUser.getDateOfBirth());
            landlord.setAddress(updatedEntityUser.getAddress());
            landlord.setNationalId(updatedEntityUser.getNationalId());
            landlord.setIsActive(updatedEntityUser.getIsActive());
            return userRepository.save(landlord);
        }).orElseThrow(() -> new RuntimeException("Landlord not found with id " + id));
    }

    @Override
    public void deleteLandlord(Long id) {
        userRepository.deleteById(id);
    }
}
