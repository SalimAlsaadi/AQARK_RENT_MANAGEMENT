package com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.StatusDTO;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UnitStatusDTO {

    private Long id;

    private String statusDomain;
    private String code;

    private String nameEn;
    private String nameAr;

    private String descriptionEn;
    private String descriptionAr;

    private String colorCode;
    private String icon;

    private boolean active;
}