package com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.StatusDTO;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Enums.StatusLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UnitStatusUpdateRequestDTO {

        @NotNull(message = "Status id is required.")
        @Positive(message = "Status id must be greater than zero.")
        private Long statusId;

        @NotBlank(message = "Status domain is required")
        @Size(max = 50, message = "Status domain must not exceed 50 characters")
        private String statusDomain;

        @NotBlank(message = "Status code is required")
        @Size(max = 100, message = "Status code must not exceed 100 characters")
        private String code;

        private StatusLevel statusLevel;

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
