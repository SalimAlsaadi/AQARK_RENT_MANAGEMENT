package com.AQARK.AQARK_RENT_MANAGEMENT.Repositories;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.BuildingDTO.BuildingUnitCountsProjection;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityBuildings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BuildingRepository extends JpaRepository<EntityBuildings, Long> {

    List<EntityBuildings> findAllByLandlordIdAndDeletedFalseOrderByIdDesc(Long landlordId);

    List<EntityBuildings> findAllByDeletedFalseOrderByIdDesc();

    boolean existsByLandlordIdAndBuildingNumberIgnoreCaseAndDeletedFalse(
            Long landlordId,
            String buildingNumber
    );

    @Query("""
        SELECT b
        FROM EntityBuildings b
        LEFT JOIN FETCH b.region
        LEFT JOIN FETCH b.wilaya
        LEFT JOIN FETCH b.area
        LEFT JOIN FETCH b.status
        LEFT JOIN FETCH b.landlord
        WHERE b.id = :id
          AND b.deleted = false
    """)
    EntityBuildings findDetailsById(@Param("id") Long id);

    @Query("""
        SELECT DISTINCT b
        FROM EntityBuildings b
        LEFT JOIN FETCH b.region
        LEFT JOIN FETCH b.wilaya
        LEFT JOIN FETCH b.area
        LEFT JOIN FETCH b.status
        LEFT JOIN FETCH b.landlord
        WHERE b.landlord.id = :landlordId
          AND b.deleted = false
        ORDER BY b.id DESC
    """)
    List<EntityBuildings> findDetailsByLandlordId(@Param("landlordId") Long landlordId);


    @Query(
            value = """
                SELECT
                    b.id AS buildingId,

                    /* =========================================
                       FLATS
                       ========================================= */

                    ISNULL(f.totalFlats, 0) AS totalFlats,
                    ISNULL(f.flatsAvailable, 0) AS flatsAvailable,
                    ISNULL(f.flatsRented, 0) AS flatsRented,
                    ISNULL(f.flatsRentedAsRooms, 0) AS flatsRentedAsRooms,
                    ISNULL(f.flatsReserved, 0) AS flatsReserved,
                    ISNULL(f.flatsUnderMaintenance, 0) AS flatsUnderMaintenance,
                    ISNULL(f.flatsBlocked, 0) AS flatsBlocked,

                    /* =========================================
                       ROOMS
                       ========================================= */

                    ISNULL(r.totalRooms, 0) AS totalRooms,
                    ISNULL(r.directRooms, 0) AS directRooms,
                    ISNULL(r.flatRooms, 0) AS roomsInFlat,
                    ISNULL(r.roomsAvailable, 0) AS roomsAvailable,
                    ISNULL(r.roomsRented, 0) AS roomsRented,
                    ISNULL(r.roomsReserved, 0) AS roomsReserved,
                    ISNULL(r.roomsUnderMaintenance,0) AS roomsUnderMaintenance,
                    ISNULL(r.roomsBlocked, 0) AS roomsBlocked,

                    /* =========================================
                       SHOPS
                       ========================================= */

                    ISNULL(s.totalShops, 0) AS totalShops,
                    ISNULL(s.shopsAvailable, 0) AS shopsAvailable,
                    ISNULL(s.shopsRented, 0) AS shopsRented,
                    ISNULL(s.shopsReserved, 0) AS shopsReserved,
                    ISNULL(s.shopsUnderMaintenance,0) AS shopsUnderMaintenance,
                    ISNULL(s.shopsBlocked, 0) AS shopsBlocked

                FROM dbo.buildings b

                /* =============================================
                   FLAT STATISTICS
                   ============================================= */

                OUTER APPLY (
                    SELECT
                        COUNT_BIG(*) AS totalFlats,

                        /*
                         * Only whole flats are counted as available.
                         *
                         * A flat with AVAILABLE_AS_ROOMS is not also
                         * counted as one available flat because its
                         * individual rooms are counted below.
                         */
                        SUM(
                            CASE
                                WHEN us.code = 'AVAILABLE'
                                THEN 1
                                ELSE 0
                            END
                        ) AS flatsAvailable,

                        /*
                         * Only a flat rented as one complete unit is
                         * counted here.
                         *
                         * FULLY_RENTED_AS_ROOMS is represented through
                         * the individual rented rooms.
                         */
                        SUM(
                            CASE
                                WHEN us.code = 'RENTED'
                                THEN 1
                                ELSE 0
                            END
                        ) AS flatsRented,
                
                            /*
                             * Flats that have at least one room rented.
                             *
                             * PARTIALLY_RENTED_AS_ROOMS:
                             * Some rooms are rented.
                             *
                             * FULLY_RENTED_AS_ROOMS:
                             * All rentable rooms are rented.
                             */
                        SUM(
                                CASE
                                    WHEN us.code IN (
                                        'PARTIALLY_RENTED_AS_ROOMS',
                                        'FULLY_RENTED_AS_ROOMS',
                                        'AVAILABLE_AS_ROOMS'
                                    )
                                    THEN 1
                                    ELSE 0
                                END
                            ) AS flatsRentedAsRooms,

                        SUM(
                            CASE
                                WHEN us.code = 'RESERVED'
                                THEN 1
                                ELSE 0
                            END
                        ) AS flatsReserved,

                        SUM(
                            CASE
                                WHEN us.code = 'UNDER_MAINTENANCE'
                                THEN 1
                                ELSE 0
                            END
                        ) AS flatsUnderMaintenance,

                        SUM(
                            CASE
                                WHEN us.code = 'BLOCKED'
                                THEN 1
                                ELSE 0
                            END
                        ) AS flatsBlocked

                    FROM dbo.entity_flats ef

                    INNER JOIN dbo.unit_statuses us
                        ON us.id = ef.status_id

                    WHERE ef.building_id = b.id
                      AND ef.active = 1
                      AND ef.deleted = 0
                ) f

                /* =============================================
                   ROOM STATISTICS

                   Supports:
                   1. Direct room in building
                   2. Room inside building flat
                   ============================================= */

                OUTER APPLY (
                    SELECT
                        COUNT_BIG(*) AS totalRooms,

                        SUM(
                            CASE
                                WHEN er.flat_id IS NULL
                                THEN 1
                                ELSE 0
                            END
                        ) AS directRooms,

                        SUM(
                            CASE
                                WHEN er.flat_id IS NOT NULL
                                THEN 1
                                ELSE 0
                            END
                        ) AS flatRooms,

                        SUM(
                            CASE
                                WHEN us.code = 'AVAILABLE'
                                THEN 1
                                ELSE 0
                            END
                        ) AS roomsAvailable,

                        SUM(
                            CASE
                                WHEN us.code = 'RENTED'
                                THEN 1
                                ELSE 0
                            END
                        ) AS roomsRented,

                        SUM(
                            CASE
                                WHEN us.code = 'RESERVED'
                                THEN 1
                                ELSE 0
                            END
                        ) AS roomsReserved,

                        SUM(
                            CASE
                                WHEN us.code = 'UNDER_MAINTENANCE'
                                THEN 1
                                ELSE 0
                            END
                        ) AS roomsUnderMaintenance,

                        SUM(
                            CASE
                                WHEN us.code = 'BLOCKED'
                                THEN 1
                                ELSE 0
                            END
                        ) AS roomsBlocked

                    FROM dbo.rooms er

                    INNER JOIN dbo.unit_statuses us
                        ON us.id = er.status_id

                    LEFT JOIN dbo.entity_flats ef
                        ON ef.id = er.flat_id
                       AND ef.active = 1
                       AND ef.deleted = 0

                    WHERE er.active = 1
                      AND er.deleted = 0

                      /*
                       * New records:
                       * building_id is populated for direct rooms
                       * and rooms inside building flats.
                       *
                       * The ef.building_id fallback also supports
                       * older records where building_id was not
                       * copied to the room.
                       */
                      AND (
                            er.building_id = b.id

                            OR (
                                er.building_id IS NULL
                                AND ef.building_id = b.id
                            )
                          )

                      /*
                       * Ensure a room attached to a deleted flat is
                       * not counted through the legacy fallback.
                       */
                      AND (
                            er.flat_id IS NULL
                            OR ef.id IS NOT NULL
                          )
                ) r

                /* =============================================
                   SHOP STATISTICS
                   ============================================= */

                OUTER APPLY (
                    SELECT
                        COUNT_BIG(*) AS totalShops,

                        SUM(
                            CASE
                                WHEN us.code = 'AVAILABLE'
                                THEN 1
                                ELSE 0
                            END
                        ) AS shopsAvailable,

                        SUM(
                            CASE
                                WHEN us.code = 'RENTED'
                                THEN 1
                                ELSE 0
                            END
                        ) AS shopsRented,

                        SUM(
                            CASE
                                WHEN us.code = 'RESERVED'
                                THEN 1
                                ELSE 0
                            END
                        ) AS shopsReserved,

                        SUM(
                            CASE
                                WHEN us.code = 'UNDER_MAINTENANCE'
                                THEN 1
                                ELSE 0
                            END
                        ) AS shopsUnderMaintenance,

                        SUM(
                            CASE
                                WHEN us.code = 'BLOCKED'
                                THEN 1
                                ELSE 0
                            END
                        ) AS shopsBlocked

                    FROM dbo.shop_rooms es

                    INNER JOIN dbo.unit_statuses us
                        ON us.id = es.status_id

                    WHERE es.building_id = b.id
                      AND es.active = 1
                      AND es.deleted = 0
                ) s

                WHERE b.id = :buildingId
                  AND b.active = 1
                  AND b.deleted = 0
                """,
            nativeQuery = true
    )
    BuildingUnitCountsProjection getBuildingUnitCounts(@Param("buildingId") Long buildingId);

    @Query(
            value = """
                SELECT
                    b.id AS buildingId,

                    /* =========================================
                       FLATS
                       ========================================= */

                    ISNULL(f.totalFlats, 0) AS totalFlats,
                    ISNULL(f.flatsAvailable, 0) AS flatsAvailable,
                    ISNULL(f.flatsRented, 0) AS flatsRented,
                    ISNULL(f.flatsReserved, 0) AS flatsReserved,
                    ISNULL(f.flatsUnderMaintenance, 0) AS flatsUnderMaintenance,
                    ISNULL(f.flatsBlocked, 0) AS flatsBlocked,

                    /* =========================================
                       ROOMS
                       ========================================= */

                    ISNULL(r.totalRooms, 0) AS totalRooms,
                    ISNULL(r.directRooms, 0) AS directRooms,
                    ISNULL(r.flatRooms, 0) AS roomsInFlat,
                    ISNULL(r.roomsAvailable, 0) AS roomsAvailable,
                    ISNULL(r.roomsRented, 0) AS roomsRented,
                    ISNULL(r.roomsReserved, 0) AS roomsReserved,
                    ISNULL(r.roomsUnderMaintenance,0) AS roomsUnderMaintenance,
                    ISNULL(r.roomsBlocked, 0) AS roomsBlocked,

                    /* =========================================
                       SHOPS
                       ========================================= */

                    ISNULL(s.totalShops, 0) AS totalShops,
                    ISNULL(s.shopsAvailable, 0) AS shopsAvailable,
                    ISNULL(s.shopsRented, 0) AS shopsRented,
                    ISNULL(s.shopsReserved, 0) AS shopsReserved,
                    ISNULL(s.shopsUnderMaintenance,0) AS shopsUnderMaintenance,
                    ISNULL(s.shopsBlocked, 0) AS shopsBlocked

                FROM dbo.buildings b

                /* =============================================
                   FLAT STATISTICS
                   ============================================= */

                OUTER APPLY (
                    SELECT
                        COUNT_BIG(*) AS totalFlats,

                        /*
                         * Only whole flats are counted as available.
                         *
                         * A flat with AVAILABLE_AS_ROOMS is not also
                         * counted as one available flat because its
                         * individual rooms are counted below.
                         */
                        SUM(
                            CASE
                                WHEN us.code = 'AVAILABLE'
                                THEN 1
                                ELSE 0
                            END
                        ) AS flatsAvailable,

                        /*
                         * Only a flat rented as one complete unit is
                         * counted here.
                         *
                         * FULLY_RENTED_AS_ROOMS is represented through
                         * the individual rented rooms.
                         */
                        SUM(
                            CASE
                                WHEN us.code = 'RENTED'
                                THEN 1
                                ELSE 0
                            END
                        ) AS flatsRented,

                        SUM(
                            CASE
                                WHEN us.code = 'RESERVED'
                                THEN 1
                                ELSE 0
                            END
                        ) AS flatsReserved,

                        SUM(
                            CASE
                                WHEN us.code = 'UNDER_MAINTENANCE'
                                THEN 1
                                ELSE 0
                            END
                        ) AS flatsUnderMaintenance,

                        SUM(
                            CASE
                                WHEN us.code = 'BLOCKED'
                                THEN 1
                                ELSE 0
                            END
                        ) AS flatsBlocked

                    FROM dbo.entity_flats ef

                    INNER JOIN dbo.unit_statuses us
                        ON us.id = ef.status_id

                    WHERE ef.building_id = b.id
                      AND ef.active = 1
                      AND ef.deleted = 0
                ) f

                /* =============================================
                   ROOM STATISTICS

                   Supports:
                   1. Direct room in building
                   2. Room inside building flat
                   ============================================= */

                OUTER APPLY (
                    SELECT
                        COUNT_BIG(*) AS totalRooms,

                        SUM(
                            CASE
                                WHEN er.flat_id IS NULL
                                THEN 1
                                ELSE 0
                            END
                        ) AS directRooms,

                        SUM(
                            CASE
                                WHEN er.flat_id IS NOT NULL
                                THEN 1
                                ELSE 0
                            END
                        ) AS flatRooms,

                        SUM(
                            CASE
                                WHEN us.code = 'AVAILABLE'
                                THEN 1
                                ELSE 0
                            END
                        ) AS roomsAvailable,

                        SUM(
                            CASE
                                WHEN us.code = 'RENTED'
                                THEN 1
                                ELSE 0
                            END
                        ) AS roomsRented,

                        SUM(
                            CASE
                                WHEN us.code = 'RESERVED'
                                THEN 1
                                ELSE 0
                            END
                        ) AS roomsReserved,

                        SUM(
                            CASE
                                WHEN us.code = 'UNDER_MAINTENANCE'
                                THEN 1
                                ELSE 0
                            END
                        ) AS roomsUnderMaintenance,

                        SUM(
                            CASE
                                WHEN us.code = 'BLOCKED'
                                THEN 1
                                ELSE 0
                            END
                        ) AS roomsBlocked

                    FROM dbo.rooms er

                    INNER JOIN dbo.unit_statuses us
                        ON us.id = er.status_id

                    LEFT JOIN dbo.entity_flats ef
                        ON ef.id = er.flat_id
                       AND ef.active = 1
                       AND ef.deleted = 0

                    WHERE er.active = 1
                      AND er.deleted = 0

                      /*
                       * New records:
                       * building_id is populated for direct rooms
                       * and rooms inside building flats.
                       *
                       * The ef.building_id fallback also supports
                       * older records where building_id was not
                       * copied to the room.
                       */
                      AND (
                            er.building_id = b.id

                            OR (
                                er.building_id IS NULL
                                AND ef.building_id = b.id
                            )
                          )

                      /*
                       * Ensure a room attached to a deleted flat is
                       * not counted through the legacy fallback.
                       */
                      AND (
                            er.flat_id IS NULL
                            OR ef.id IS NOT NULL
                          )
                ) r

                /* =============================================
                   SHOP STATISTICS
                   ============================================= */

                OUTER APPLY (
                    SELECT
                        COUNT_BIG(*) AS totalShops,

                        SUM(
                            CASE
                                WHEN us.code = 'AVAILABLE'
                                THEN 1
                                ELSE 0
                            END
                        ) AS shopsAvailable,

                        SUM(
                            CASE
                                WHEN us.code = 'RENTED'
                                THEN 1
                                ELSE 0
                            END
                        ) AS shopsRented,

                        SUM(
                            CASE
                                WHEN us.code = 'RESERVED'
                                THEN 1
                                ELSE 0
                            END
                        ) AS shopsReserved,

                        SUM(
                            CASE
                                WHEN us.code = 'UNDER_MAINTENANCE'
                                THEN 1
                                ELSE 0
                            END
                        ) AS shopsUnderMaintenance,

                        SUM(
                            CASE
                                WHEN us.code = 'BLOCKED'
                                THEN 1
                                ELSE 0
                            END
                        ) AS shopsBlocked

                    FROM dbo.shop_rooms es

                    INNER JOIN dbo.unit_statuses us
                        ON us.id = es.status_id

                    WHERE es.building_id = b.id
                      AND es.active = 1
                      AND es.deleted = 0
                ) s

                WHERE b.landlord_id = :landlordId
                  AND b.deleted = 0
                ORDER BY b.id DESC
                """,
            nativeQuery = true
    )
    List<BuildingUnitCountsProjection> getBuildingUnitCountsByLandlordId(@Param("landlordId") Long landlordId);



    @Query(
            value = """
                SELECT
                    b.id AS buildingId,

                    /* =========================================
                       FLATS
                       ========================================= */

                    ISNULL(f.totalFlats, 0) AS totalFlats,
                    ISNULL(f.flatsAvailable, 0) AS flatsAvailable,
                    ISNULL(f.flatsRented, 0) AS flatsRented,
                    ISNULL(f.flatsReserved, 0) AS flatsReserved,
                    ISNULL(f.flatsUnderMaintenance, 0) AS flatsUnderMaintenance,
                    ISNULL(f.flatsBlocked, 0) AS flatsBlocked,

                    /* =========================================
                       ROOMS
                       ========================================= */

                    ISNULL(r.totalRooms, 0) AS totalRooms,
                    ISNULL(r.directRooms, 0) AS directRooms,
                    ISNULL(r.flatRooms, 0) AS roomsInFlat,
                    ISNULL(r.roomsAvailable, 0) AS roomsAvailable,
                    ISNULL(r.roomsRented, 0) AS roomsRented,
                    ISNULL(r.roomsReserved, 0) AS roomsReserved,
                    ISNULL(r.roomsUnderMaintenance,0) AS roomsUnderMaintenance,
                    ISNULL(r.roomsBlocked, 0) AS roomsBlocked,

                    /* =========================================
                       SHOPS
                       ========================================= */

                    ISNULL(s.totalShops, 0) AS totalShops,
                    ISNULL(s.shopsAvailable, 0) AS shopsAvailable,
                    ISNULL(s.shopsRented, 0) AS shopsRented,
                    ISNULL(s.shopsReserved, 0) AS shopsReserved,
                    ISNULL(s.shopsUnderMaintenance,0) AS shopsUnderMaintenance,
                    ISNULL(s.shopsBlocked, 0) AS shopsBlocked

                FROM dbo.buildings b

                /* =============================================
                   FLAT STATISTICS
                   ============================================= */

                OUTER APPLY (
                    SELECT
                        COUNT_BIG(*) AS totalFlats,

                        /*
                         * Only whole flats are counted as available.
                         *
                         * A flat with AVAILABLE_AS_ROOMS is not also
                         * counted as one available flat because its
                         * individual rooms are counted below.
                         */
                        SUM(
                            CASE
                                WHEN us.code = 'AVAILABLE'
                                THEN 1
                                ELSE 0
                            END
                        ) AS flatsAvailable,

                        /*
                         * Only a flat rented as one complete unit is
                         * counted here.
                         *
                         * FULLY_RENTED_AS_ROOMS is represented through
                         * the individual rented rooms.
                         */
                        SUM(
                            CASE
                                WHEN us.code = 'RENTED'
                                THEN 1
                                ELSE 0
                            END
                        ) AS flatsRented,

                        SUM(
                            CASE
                                WHEN us.code = 'RESERVED'
                                THEN 1
                                ELSE 0
                            END
                        ) AS flatsReserved,

                        SUM(
                            CASE
                                WHEN us.code = 'UNDER_MAINTENANCE'
                                THEN 1
                                ELSE 0
                            END
                        ) AS flatsUnderMaintenance,

                        SUM(
                            CASE
                                WHEN us.code = 'BLOCKED'
                                THEN 1
                                ELSE 0
                            END
                        ) AS flatsBlocked

                    FROM dbo.entity_flats ef

                    INNER JOIN dbo.unit_statuses us
                        ON us.id = ef.status_id

                    WHERE ef.building_id = b.id
                      AND ef.active = 1
                      AND ef.deleted = 0
                ) f

                /* =============================================
                   ROOM STATISTICS

                   Supports:
                   1. Direct room in building
                   2. Room inside building flat
                   ============================================= */

                OUTER APPLY (
                    SELECT
                        COUNT_BIG(*) AS totalRooms,

                        SUM(
                            CASE
                                WHEN er.flat_id IS NULL
                                THEN 1
                                ELSE 0
                            END
                        ) AS directRooms,

                        SUM(
                            CASE
                                WHEN er.flat_id IS NOT NULL
                                THEN 1
                                ELSE 0
                            END
                        ) AS flatRooms,

                        SUM(
                            CASE
                                WHEN us.code = 'AVAILABLE'
                                THEN 1
                                ELSE 0
                            END
                        ) AS roomsAvailable,

                        SUM(
                            CASE
                                WHEN us.code = 'RENTED'
                                THEN 1
                                ELSE 0
                            END
                        ) AS roomsRented,

                        SUM(
                            CASE
                                WHEN us.code = 'RESERVED'
                                THEN 1
                                ELSE 0
                            END
                        ) AS roomsReserved,

                        SUM(
                            CASE
                                WHEN us.code = 'UNDER_MAINTENANCE'
                                THEN 1
                                ELSE 0
                            END
                        ) AS roomsUnderMaintenance,

                        SUM(
                            CASE
                                WHEN us.code = 'BLOCKED'
                                THEN 1
                                ELSE 0
                            END
                        ) AS roomsBlocked

                    FROM dbo.rooms er

                    INNER JOIN dbo.unit_statuses us
                        ON us.id = er.status_id

                    LEFT JOIN dbo.entity_flats ef
                        ON ef.id = er.flat_id
                       AND ef.active = 1
                       AND ef.deleted = 0

                    WHERE er.active = 1
                      AND er.deleted = 0

                      /*
                       * New records:
                       * building_id is populated for direct rooms
                       * and rooms inside building flats.
                       *
                       * The ef.building_id fallback also supports
                       * older records where building_id was not
                       * copied to the room.
                       */
                      AND (
                            er.building_id = b.id

                            OR (
                                er.building_id IS NULL
                                AND ef.building_id = b.id
                            )
                          )

                      /*
                       * Ensure a room attached to a deleted flat is
                       * not counted through the legacy fallback.
                       */
                      AND (
                            er.flat_id IS NULL
                            OR ef.id IS NOT NULL
                          )
                ) r

                /* =============================================
                   SHOP STATISTICS
                   ============================================= */

                OUTER APPLY (
                    SELECT
                        COUNT_BIG(*) AS totalShops,

                        SUM(
                            CASE
                                WHEN us.code = 'AVAILABLE'
                                THEN 1
                                ELSE 0
                            END
                        ) AS shopsAvailable,

                        SUM(
                            CASE
                                WHEN us.code = 'RENTED'
                                THEN 1
                                ELSE 0
                            END
                        ) AS shopsRented,

                        SUM(
                            CASE
                                WHEN us.code = 'RESERVED'
                                THEN 1
                                ELSE 0
                            END
                        ) AS shopsReserved,

                        SUM(
                            CASE
                                WHEN us.code = 'UNDER_MAINTENANCE'
                                THEN 1
                                ELSE 0
                            END
                        ) AS shopsUnderMaintenance,

                        SUM(
                            CASE
                                WHEN us.code = 'BLOCKED'
                                THEN 1
                                ELSE 0
                            END
                        ) AS shopsBlocked

                    FROM dbo.shop_rooms es

                    INNER JOIN dbo.unit_statuses us
                        ON us.id = es.status_id

                    WHERE es.building_id = b.id
                      AND es.active = 1
                      AND es.deleted = 0
                ) s

                WHERE b.deleted = 0
                ORDER BY b.id DESC
                """,
            nativeQuery = true
    )
    List<BuildingUnitCountsProjection> getAllBuildingUnitCounts();
}