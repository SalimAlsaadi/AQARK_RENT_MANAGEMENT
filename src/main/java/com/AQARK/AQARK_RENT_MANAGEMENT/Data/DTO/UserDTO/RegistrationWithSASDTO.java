package com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.UserDTO;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class RegistrationWithSASDTO {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 150)
    private String email;

    /**
     * Raw password sent from AQARK.
     * SAS will encode it before saving.
     */
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be at least 8 characters")
    private String password;



    /**
     * ID of profile created in AQARK DB
     */
    @NotNull(message = "refId is required")
    private Long refId;

    /**
     * Roles to assign inside SAS
     */
    @NotEmpty(message = "At least one role must be assigned")
    private Set<String> roles = new HashSet<>();

    /**
     * OAuth client IDs this user can access
     */
    @NotEmpty(message = "At least one allowed client is required")
    private Set<String> allowedClientIds = new HashSet<>();
}
