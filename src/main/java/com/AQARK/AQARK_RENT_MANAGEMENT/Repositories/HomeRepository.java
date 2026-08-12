package com.AQARK.AQARK_RENT_MANAGEMENT.Repositories;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.HomeDTO.HomeUnitCountsProjection;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityHomes;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HomeRepository extends JpaRepository<EntityHomes, Long> {

    Optional<EntityHomes> findByIdAndDeletedFalse(Long id);

    boolean existsByIdAndLandlordIdAndDeletedFalse(
            Long homeId,
            Long landlordId
    );

    boolean existsByLandlordIdAndHomeNumberIgnoreCaseAndDeletedFalse(
            Long landlordId,
            String homeNumber
    );


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT h
        FROM EntityHomes h
        LEFT JOIN FETCH h.status
        LEFT JOIN FETCH h.landlord
        WHERE h.id = :homeId
          AND h.deleted = false
    """)
    Optional<EntityHomes> findByIdForUpdate(@Param("homeId") Long homeId);

    @Query("""
        SELECT h
        FROM EntityHomes h
        LEFT JOIN FETCH h.status
        LEFT JOIN FETCH h.landlord
        LEFT JOIN FETCH h.region
        LEFT JOIN FETCH h.wilaya
        LEFT JOIN FETCH h.area
        WHERE h.id = :homeId
          AND h.deleted = false
    """)
    EntityHomes findDetailsById(
            @Param("homeId") Long homeId
    );

    @Query("""
        SELECT h
        FROM EntityHomes h
        LEFT JOIN FETCH h.status
        LEFT JOIN FETCH h.landlord
        LEFT JOIN FETCH h.region
        LEFT JOIN FETCH h.wilaya
        LEFT JOIN FETCH h.area
        WHERE h.landlord.id = :landlordId
          AND h.deleted = false
        ORDER BY h.id DESC
    """)
    List<EntityHomes> findDetailsByLandlordId(
            @Param("landlordId") Long landlordId
    );

    @Query("""
        SELECT h
        FROM EntityHomes h
        LEFT JOIN FETCH h.status
        LEFT JOIN FETCH h.landlord
        LEFT JOIN FETCH h.region
        LEFT JOIN FETCH h.wilaya
        LEFT JOIN FETCH h.area
        WHERE h.deleted = false
        ORDER BY h.id DESC
    """)
    List<EntityHomes> findAllDetails();

    @Query(
            value = HOME_COUNTS_QUERY + """
                WHERE h.id = :homeId
                  AND h.deleted = 0
            """,
            nativeQuery = true
    )
    HomeUnitCountsProjection getHomeUnitCounts(
            @Param("homeId") Long homeId
    );

    @Query(
            value = HOME_COUNTS_QUERY + """
                WHERE h.landlord_id = :landlordId
                  AND h.deleted = 0
                ORDER BY h.id DESC
            """,
            nativeQuery = true
    )
    List<HomeUnitCountsProjection> getHomeUnitCountsByLandlordId(
            @Param("landlordId") Long landlordId
    );

    @Query(
            value = HOME_COUNTS_QUERY + """
                WHERE h.deleted = 0
                ORDER BY h.id DESC
            """,
            nativeQuery = true
    )
    List<HomeUnitCountsProjection> getAllHomeUnitCounts();

    String HOME_COUNTS_QUERY = """
        SELECT
            h.id AS homeId,

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
            ISNULL(r.roomsInFlat, 0) AS roomsInFlat,
            ISNULL(r.roomsAvailable, 0) AS roomsAvailable,
            ISNULL(r.roomsRented, 0) AS roomsRented,
            ISNULL(r.roomsReserved, 0) AS roomsReserved,
            ISNULL(
                r.roomsUnderMaintenance,
                0
            ) AS roomsUnderMaintenance,
            ISNULL(r.roomsBlocked, 0) AS roomsBlocked

        FROM dbo.entity_home h

        /* =============================================
           HOME FLATS
           ============================================= */

        OUTER APPLY (
            SELECT
                COUNT_BIG(*) AS totalFlats,

                SUM(
                    CASE
                        WHEN us.code = 'AVAILABLE'
                        THEN 1
                        ELSE 0
                    END
                ) AS flatsAvailable,

                /*
                 * A whole flat is counted as rented only when
                 * the flat itself has RENTED status.
                 */
                SUM(
                    CASE
                        WHEN us.code = 'RENTED'
                        THEN 1
                        ELSE 0
                    END
                ) AS flatsRented,

                /*
                 * These flats are rented or offered through
                 * their individual rooms.
                 *
                 * They are not included in flatsAvailable
                 * or flatsRented to avoid double counting.
                 */
                SUM(
                    CASE
                        WHEN us.code IN (
                            'AVAILABLE_AS_ROOMS',
                            'PARTIALLY_RENTED_AS_ROOMS',
                            'FULLY_RENTED_AS_ROOMS'
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

            WHERE ef.home_id = h.id
              AND ef.active = 1
              AND ef.deleted = 0
        ) f

        /* =============================================
           HOME ROOMS

           Supports:
           1. Direct room attached to the home
           2. Room inside a flat attached to the home
           ============================================= */

        OUTER APPLY (
            SELECT
                COUNT_BIG(*) AS totalRooms,

                /*
                 * Direct room:
                 * belongs to the home but has no flat.
                 */
                SUM(
                    CASE
                        WHEN er.flat_id IS NULL
                        THEN 1
                        ELSE 0
                    END
                ) AS directRooms,

                /*
                 * Room inside a home flat.
                 */
                SUM(
                    CASE
                        WHEN er.flat_id IS NOT NULL
                        THEN 1
                        ELSE 0
                    END
                ) AS roomsInFlat,

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

            /*
             * Needed for:
             * - validating rooms inside active home flats
             * - supporting older records where room.home_id is null
             */
            LEFT JOIN dbo.entity_flats ef
                ON ef.id = er.flat_id
               AND ef.active = 1
               AND ef.deleted = 0

            WHERE er.active = 1
              AND er.deleted = 0

              /*
               * New records should have room.home_id populated.
               *
               * The flat fallback supports older room records
               * where home_id was not copied to the room.
               */
              AND (
                    er.home_id = h.id

                    OR (
                        er.home_id IS NULL
                        AND ef.home_id = h.id
                    )
                  )

              /*
               * A room connected to a deleted or inactive flat
               * must not be counted.
               *
               * Direct rooms do not require a flat.
               */
              AND (
                    er.flat_id IS NULL
                    OR ef.id IS NOT NULL
                  )
        ) r
        """;
}