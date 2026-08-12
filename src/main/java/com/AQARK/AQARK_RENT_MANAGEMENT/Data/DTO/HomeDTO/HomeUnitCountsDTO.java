package com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.HomeDTO;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomeUnitCountsDTO {

    private long totalFlats;

    private long flatsAvailable;
    private long flatsRented;
    private long flatsRentedAsRooms;
    private long flatsReserved;
    private long flatsUnderMaintenance;
    private long flatsBlocked;

    private long totalRooms;

    private long directRooms;
    private long roomsInFlat;

    private long roomsAvailable;
    private long roomsRented;
    private long roomsReserved;
    private long roomsUnderMaintenance;
    private long roomsBlocked;

    private long availableUnits;
    private long rentedUnits;
    private long reservedUnits;
    private long maintenanceUnits;
    private long blockedUnits;
}