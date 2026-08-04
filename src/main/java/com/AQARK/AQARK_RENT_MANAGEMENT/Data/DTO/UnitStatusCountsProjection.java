package com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO;

public interface UnitStatusCountsProjection {
    Long getTotal();
    Long getAvailable();
    Long getRented();
    Long getReserved();
    Long getUnderMaintenance();
    Long getBlocked();
}
