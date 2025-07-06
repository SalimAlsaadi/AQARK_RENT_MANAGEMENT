package com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.Landlord;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.Date;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)//because dto who coming from auth server has additional field called password. and this field no need save in resource server.
public class LandlordSaveRequestDTO {

    @NotBlank
    @Size(max = 100)
    private String firstName;

    @NotBlank
    @Size(max = 100)
    private String lastName;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Invalid phone number")
    private String phoneNumber;

    @PastOrPresent
    private Date dateOfBirth;

    private String address;

    @NotBlank
    private String nationalId;

    private Boolean isActive = true;
}
