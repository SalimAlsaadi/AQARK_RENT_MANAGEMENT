package com.AQARK.AQARK_RENT_MANAGEMENT.Data.Enums;

public enum HomeRentalStructure {

    /**
     * The Home is rented as one complete rentable property.
     *
     * There are no independently rentable child Flats
     * or direct Home Rooms.
     */
    WHOLE_HOME,

    /**
     * The Home contains one or more independently rentable
     * child units.
     *
     * Child units may be:
     * - direct Home Rooms
     * - Flats
     * - Rooms inside Flats
     * - any combination of them
     *
     * The remaining part of the Home may still be rentable
     * using the Home's own commercial fields.
     */
    MULTI_UNITS_HOME
}
