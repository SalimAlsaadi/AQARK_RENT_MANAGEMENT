package com.AQARK.AQARK_RENT_MANAGEMENT.Repositories;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.FlatDTO.FlatSearchRequestDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.UnitStatusCountsProjection;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityFlats;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FlatsRepository extends JpaRepository<EntityFlats, Long> {

    @Query("""
        select distinct flat
    from EntityFlats flat
    left join fetch flat.status status
    left join fetch flat.building building
    left join fetch flat.home home
    left join fetch flat.landlord landlord
    where flat.id = :flatId
      and flat.deleted = false
    """)
    EntityFlats findDetailsById(@Param("flatId") Long flatId);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT DISTINCT f FROM EntityFlats f
        LEFT JOIN FETCH f.status
        LEFT JOIN FETCH f.landlord
        LEFT JOIN FETCH f.building
        LEFT JOIN FETCH f.home
        WHERE f.id = :flatId AND f.deleted = false
    """)
    Optional<EntityFlats> findByIdForUpdate(@Param("flatId") Long flatId);

    @Query("""
        SELECT f FROM EntityFlats f
        LEFT JOIN FETCH f.building b
        LEFT JOIN FETCH b.region LEFT JOIN FETCH b.wilaya LEFT JOIN FETCH b.area
        LEFT JOIN FETCH f.landlord LEFT JOIN FETCH f.status
        WHERE b.id = :buildingId AND f.deleted = false
        ORDER BY f.floorNumber ASC, f.flatNumber ASC
    """)
    List<EntityFlats> findDetailsByBuildingId(@Param("buildingId") Long buildingId);

    @Query("""
        SELECT DISTINCT f FROM EntityFlats f
        LEFT JOIN FETCH f.status LEFT JOIN FETCH f.landlord LEFT JOIN FETCH f.home h
        WHERE h.id = :homeId AND f.deleted = false ORDER BY f.id DESC
    """)
    List<EntityFlats> findDetailsByHomeId(@Param("homeId") Long homeId);

    @Query("""
        SELECT f FROM EntityFlats f
        LEFT JOIN FETCH f.building b
        LEFT JOIN FETCH b.region LEFT JOIN FETCH b.wilaya LEFT JOIN FETCH b.area
        LEFT JOIN FETCH f.home h
        LEFT JOIN FETCH h.region LEFT JOIN FETCH h.wilaya LEFT JOIN FETCH h.area
        LEFT JOIN FETCH f.landlord LEFT JOIN FETCH f.status
        WHERE f.landlord.id = :landlordId AND f.deleted = false ORDER BY f.id DESC
    """)
    List<EntityFlats> findDetailsByLandlordId(@Param("landlordId") Long landlordId);

    @Query("""
        SELECT f FROM EntityFlats f
        LEFT JOIN FETCH f.building b
        LEFT JOIN FETCH b.region LEFT JOIN FETCH b.wilaya LEFT JOIN FETCH b.area
        LEFT JOIN FETCH f.home h
        LEFT JOIN FETCH f.landlord LEFT JOIN FETCH f.status
        WHERE f.deleted = false
          AND (:#{#filter.landlordId} IS NULL OR f.landlord.id = :#{#filter.landlordId})
          AND (:#{#filter.buildingId} IS NULL OR b.id = :#{#filter.buildingId})
          AND (:#{#filter.statusCode} IS NULL OR UPPER(f.status.code) = UPPER(:#{#filter.statusCode}))
          AND (:#{#filter.numberOfRooms} IS NULL OR f.numberOfRooms = :#{#filter.numberOfRooms})
          AND (:#{#filter.numberOfBathrooms} IS NULL OR f.numberOfBathrooms = :#{#filter.numberOfBathrooms})
          AND (:#{#filter.hasLivingRoom} IS NULL OR f.hasLivingRoom = :#{#filter.hasLivingRoom})
          AND (:#{#filter.floorNumber} IS NULL OR f.floorNumber = :#{#filter.floorNumber})
          AND (:#{#filter.tenantType} IS NULL OR f.tenantType = :#{#filter.tenantType})
          AND (:#{#filter.rentTimeType} IS NULL OR f.rentalType = :#{#filter.rentTimeType})
          AND (:#{#filter.minPrice} IS NULL OR f.price >= :#{#filter.minPrice})
          AND (:#{#filter.maxPrice} IS NULL OR f.price <= :#{#filter.maxPrice})
        ORDER BY f.id DESC
    """)
    List<EntityFlats> searchFlats(@Param("filter") FlatSearchRequestDTO filter);

    @Query("""
        SELECT COUNT(f.id) AS total,
               COALESCE(SUM(CASE WHEN UPPER(s.code) IN ('AVAILABLE','AVAILABLE_AS_ROOMS') THEN 1 ELSE 0 END), 0) AS available,
               COALESCE(SUM(CASE WHEN UPPER(s.code) IN ('RENTED','FULLY_RENTED_AS_ROOMS') THEN 1 ELSE 0 END), 0) AS rented,
               COALESCE(SUM(CASE WHEN UPPER(s.code) IN ('RESERVED','PARTIALLY_RENTED_AS_ROOMS') THEN 1 ELSE 0 END), 0) AS reserved,
               COALESCE(SUM(CASE WHEN UPPER(s.code) = 'UNDER_MAINTENANCE' THEN 1 ELSE 0 END), 0) AS underMaintenance,
               COALESCE(SUM(CASE WHEN UPPER(s.code) = 'BLOCKED' THEN 1 ELSE 0 END), 0) AS blocked
        FROM EntityFlats f JOIN f.status s
        WHERE f.home.id = :homeId AND f.active = true AND f.deleted = false
    """)
    UnitStatusCountsProjection getHomeFlatStatusCounts(@Param("homeId") Long homeId);

    boolean existsByBuildingIdAndFlatNumberIgnoreCaseAndDeletedFalse(Long buildingId, String flatNumber);
    boolean existsByHomeIdAndFlatNumberIgnoreCaseAndDeletedFalse(Long homeId, String flatNumber);
    long countByBuildingIdAndDeletedFalse(Long buildingId);
    long countByHomeIdAndActiveTrueAndDeletedFalse(Long homeId);
    long countByBuildingIdAndStatusCodeIgnoreCaseAndDeletedFalse(Long buildingId, String statusCode);
}
