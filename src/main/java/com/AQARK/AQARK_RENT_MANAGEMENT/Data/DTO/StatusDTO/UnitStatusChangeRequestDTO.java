package com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.StatusDTO;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Enums.StatusLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UnitStatusChangeRequestDTO {

    @NotBlank(message = "Status code is required")
    @Size(max = 100, message = "Status code must not exceed 100 characters")
    private String statusCode;

    private StatusLevel statusLevel;

    @Size(max = 500, message = "Reason must not exceed 500 characters")
    private String reason;
}