package com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.StatusDTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UnitStatusSaveRequestDTO {

    @NotBlank(message = "Status domain is required")
    @Size(max = 50, message = "Status domain must not exceed 50 characters")
    private String statusDomain;

    @NotBlank(message = "Status code is required")
    @Size(max = 100, message = "Status code must not exceed 100 characters")
    private String code;

    @NotBlank(message = "English status name is required")
    @Size(max = 150, message = "English name must not exceed 150 characters")
    private String nameEn;

    @Size(max = 150, message = "Arabic name must not exceed 150 characters")
    private String nameAr;

    @Size(max = 500, message = "English description must not exceed 500 characters")
    private String descriptionEn;

    @Size(max = 500, message = "Arabic description must not exceed 500 characters")
    private String descriptionAr;

    @Size(max = 30, message = "Color code must not exceed 30 characters")
    private String colorCode;

    @Size(max = 100, message = "Icon must not exceed 100 characters")
    private String icon;

    private Boolean active = true;
}