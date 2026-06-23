package com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.UserDTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class RegistrationUserDTO {
    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String phoneNumber;

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;

    private String address;

    private List<Integer> userType;

    private Boolean isActive;

    @NotBlank
    private String nationalId;

    /**
     * Commercial Registration number. Mandatory when registering as a landlord, ignored for
     * tenants. Conditional validation lives in the service layer because it depends on the
     * selected {@link #userType}.
     */
    @Size(max = 50)
    private String crNumber;

    // ---------- AUTH FIELD ----------
    // Must satisfy the Keycloak realm password policy:
    // length>=12, upper, lower, digit, special char, not equal to username.
    @NotBlank
    @Size(min = 12, message = "Password must be at least 12 characters")
    private String password;

}
