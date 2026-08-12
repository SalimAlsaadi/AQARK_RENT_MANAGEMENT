package com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.StatusDTO;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Enums.StatusLevel;
import lombok.*;

@Getter
@Builder
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UnitStatusDTO {

    private Long id;

    private String statusDomain;
    private String code;

    private StatusLevel statusLevel;
    private String nameEn;
    private String nameAr;

    private String descriptionEn;
    private String descriptionAr;

    private String colorCode;
    private String icon;

    private boolean active;
}