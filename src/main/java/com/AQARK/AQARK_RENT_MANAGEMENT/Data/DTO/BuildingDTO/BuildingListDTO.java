package com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.BuildingDTO;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BuildingListDTO {

    private Long id;

    private String buildingName;
    private String buildingNumber;

    private String region;
    private String wilaya;
    private String area;

    private String statusCode;
    private String statusNameEn;
    private String statusNameAr;
    private String statusColorCode;
    private String statusIcon;

    private String numberOfFloors;

    private long totalFlats;
    private long totalRooms;
    private long totalShops;

    private long availableUnits;
    private long rentedUnits;
    private long maintenanceUnits;

    private String mainImageUrl;
}