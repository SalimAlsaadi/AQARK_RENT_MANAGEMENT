package com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.LocationDTO;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LocationLookupDTO {
    private Long id;
    private String code;
    private String nameEn;
    private String nameAr;
}