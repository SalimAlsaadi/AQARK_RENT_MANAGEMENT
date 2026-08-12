package com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.StatusDTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StatusDomainLevelDTO {

    @NotBlank(message = "Status domain is required.")
    private String statusDomain;

    @NotBlank(message = "Status level is required.")
    private String statusLevel;
}