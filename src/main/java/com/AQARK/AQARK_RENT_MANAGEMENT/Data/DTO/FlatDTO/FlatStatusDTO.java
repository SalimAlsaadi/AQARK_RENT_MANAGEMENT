package com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.FlatDTO;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FlatStatusDTO {

    private Long id;
    private String statusDomain;
    private String code;

    private String nameEn;
    private String nameAr;

    private String colorCode;
    private String icon;
}