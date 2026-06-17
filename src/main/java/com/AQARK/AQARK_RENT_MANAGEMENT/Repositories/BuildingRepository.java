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
        SELECT b
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

    @Query(value = """
        SELECT
            b.id AS buildingId,

            ISNULL(f.totalFlats, 0) AS totalFlats,
            ISNULL(f.flatsAvailable, 0) AS flatsAvailable,
            ISNULL(f.flatsRented, 0) AS flatsRented,
            ISNULL(f.flatsReserved, 0) AS flatsReserved,
            ISNULL(f.flatsUnderMaintenance, 0) AS flatsUnderMaintenance,
            ISNULL(f.flatsBlocked, 0) AS flatsBlocked,

            ISNULL(r.totalRooms, 0) AS totalRooms,
            ISNULL(r.roomsAvailable, 0) AS roomsAvailable,
            ISNULL(r.roomsRented, 0) AS roomsRented,
            ISNULL(r.roomsReserved, 0) AS roomsReserved,
            ISNULL(r.roomsUnderMaintenance, 0) AS roomsUnderMaintenance,
            ISNULL(r.roomsBlocked, 0) AS roomsBlocked,

            ISNULL(s.totalShops, 0) AS totalShops,
            ISNULL(s.shopsAvailable, 0) AS shopsAvailable,
            ISNULL(s.shopsRented, 0) AS shopsRented,
            ISNULL(s.shopsReserved, 0) AS shopsReserved,
            ISNULL(s.shopsUnderMaintenance, 0) AS shopsUnderMaintenance,
            ISNULL(s.shopsBlocked, 0) AS shopsBlocked

        FROM app_root.buildings b

        OUTER APPLY (
            SELECT
                COUNT(*) AS totalFlats,
                SUM(CASE WHEN us.code = 'AVAILABLE' THEN 1 ELSE 0 END) AS flatsAvailable,
                SUM(CASE WHEN us.code = 'RENTED' THEN 1 ELSE 0 END) AS flatsRented,
                SUM(CASE WHEN us.code = 'RESERVED' THEN 1 ELSE 0 END) AS flatsReserved,
                SUM(CASE WHEN us.code = 'UNDER_MAINTENANCE' THEN 1 ELSE 0 END) AS flatsUnderMaintenance,
                SUM(CASE WHEN us.code = 'BLOCKED' THEN 1 ELSE 0 END) AS flatsBlocked
            FROM app_root.entity_flats ef
            JOIN app_root.unit_statuses us ON us.id = ef.status_id
            WHERE ef.building_id = b.id
        ) f

        OUTER APPLY (
            SELECT
                COUNT(*) AS totalRooms,
                SUM(CASE WHEN us.code = 'AVAILABLE' THEN 1 ELSE 0 END) AS roomsAvailable,
                SUM(CASE WHEN us.code = 'RENTED' THEN 1 ELSE 0 END) AS roomsRented,
                SUM(CASE WHEN us.code = 'RESERVED' THEN 1 ELSE 0 END) AS roomsReserved,
                SUM(CASE WHEN us.code = 'UNDER_MAINTENANCE' THEN 1 ELSE 0 END) AS roomsUnderMaintenance,
                SUM(CASE WHEN us.code = 'BLOCKED' THEN 1 ELSE 0 END) AS roomsBlocked
            FROM app_root.rooms er
            JOIN app_root.entity_flats ef ON ef.id = er.flat_id
            JOIN app_root.unit_statuses us ON us.id = er.status_id
            WHERE ef.building_id = b.id
        ) r

        OUTER APPLY (
            SELECT
                COUNT(*) AS totalShops,
                SUM(CASE WHEN us.code = 'AVAILABLE' THEN 1 ELSE 0 END) AS shopsAvailable,
                SUM(CASE WHEN us.code = 'RENTED' THEN 1 ELSE 0 END) AS shopsRented,
                SUM(CASE WHEN us.code = 'RESERVED' THEN 1 ELSE 0 END) AS shopsReserved,
                SUM(CASE WHEN us.code = 'UNDER_MAINTENANCE' THEN 1 ELSE 0 END) AS shopsUnderMaintenance,
                SUM(CASE WHEN us.code = 'BLOCKED' THEN 1 ELSE 0 END) AS shopsBlocked
            FROM app_root.shop_rooms es
            JOIN app_root.unit_statuses us ON us.id = es.status_id
            WHERE es.building_id = b.id
        ) s

        WHERE b.id = :buildingId
          AND b.deleted = 0
    """, nativeQuery = true)
    BuildingUnitCountsProjection getBuildingUnitCounts(
            @Param("buildingId") Long buildingId
    );
}