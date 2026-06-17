package com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.BuildingDTO;

public interface BuildingUnitCountsProjection {

    Long getBuildingId();

    Long getTotalFlats();
    Long getFlatsAvailable();
    Long getFlatsRented();
    Long getFlatsReserved();
    Long getFlatsUnderMaintenance();
    Long getFlatsBlocked();

    Long getTotalRooms();
    Long getRoomsAvailable();
    Long getRoomsRented();
    Long getRoomsReserved();
    Long getRoomsUnderMaintenance();
    Long getRoomsBlocked();

    Long getTotalShops();
    Long getShopsAvailable();
    Long getShopsRented();
    Long getShopsReserved();
    Long getShopsUnderMaintenance();
    Long getShopsBlocked();
}