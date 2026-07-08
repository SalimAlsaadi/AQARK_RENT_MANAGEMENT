package com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.LocationDTO;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationDTO {

    private Long regionId;
    private String regionNameEn;
    private String regionNameAr;

    private Long wilayaId;
    private String wilayaNameEn;
    private String wilayaNameAr;

    private Long areaId;
    private String areaNameEn;
    private String areaNameAr;
}