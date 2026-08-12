package com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.HomeDTO;

public interface HomeUnitCountsProjection {

    Long getHomeId();

    Long getTotalFlats();

    Long getFlatsAvailable();

    Long getFlatsRented();

    Long getFlatsRentedAsRooms();

    Long getFlatsReserved();

    Long getFlatsUnderMaintenance();

    Long getFlatsBlocked();

    Long getTotalRooms();

    Long getDirectRooms();

    Long getRoomsInFlat();

    Long getRoomsAvailable();

    Long getRoomsRented();

    Long getRoomsReserved();

    Long getRoomsUnderMaintenance();

    Long getRoomsBlocked();


}