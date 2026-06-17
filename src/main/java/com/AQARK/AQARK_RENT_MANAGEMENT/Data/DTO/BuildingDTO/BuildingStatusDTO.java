package com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.BuildingDTO;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BuildingStatusDTO {

    private Long id;
    private String code;

    private String nameEn;
    private String nameAr;

    private String colorCode;
    private String icon;

    private boolean active;
}