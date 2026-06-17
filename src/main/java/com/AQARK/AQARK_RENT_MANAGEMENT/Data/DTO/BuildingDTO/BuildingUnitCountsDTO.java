package com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.BuildingDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class BuildingUnitCountsDTO {

    private long totalFlats;
    private long flatsAvailable;
    private long flatsRented;
    private long flatsReserved;
    private long flatsUnderMaintenance;
    private long flatsBlocked;

    private long totalRooms;
    private long roomsAvailable;
    private long roomsRented;
    private long roomsReserved;
    private long roomsUnderMaintenance;
    private long roomsBlocked;

    private long totalShops;
    private long shopsAvailable;
    private long shopsRented;
    private long shopsReserved;
    private long shopsUnderMaintenance;
    private long shopsBlocked;
}