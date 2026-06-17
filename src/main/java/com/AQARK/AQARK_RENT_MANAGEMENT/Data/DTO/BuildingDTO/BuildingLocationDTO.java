package com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.BuildingDTO;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BuildingLocationDTO {

    private Long regionId;
    private String regionNameEn;
    private String regionNameAr;

    private Long wilayaId;
    private String wilayaNameEn;
    private String wilayaNameAr;

    private Long areaId;
    private String areaNameEn;
    private String areaNameAr;

    private Double latitude;
    private Double longitude;
}