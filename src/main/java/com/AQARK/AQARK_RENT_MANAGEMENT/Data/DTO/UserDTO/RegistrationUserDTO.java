package com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.UserDTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

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

    private Boolean isActive;

    @NotBlank
    private String nationalId;

    // ---------- AUTH FIELD ----------
    @NotBlank
    @Size(min = 8)
    private String password;

    private Integer refTypeId;
}
