package com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.RoomDTO;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RoomStatusDTO {

    private Long id;
    private String statusDomain;
    private String code;

    private String nameEn;
    private String nameAr;

    private String colorCode;
    private String icon;
}