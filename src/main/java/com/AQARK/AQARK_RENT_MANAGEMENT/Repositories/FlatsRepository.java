package com.AQARK.AQARK_RENT_MANAGEMENT.Repositories;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.FlatDTO.FlatSearchRequestDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityFlats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FlatsRepository extends JpaRepository<EntityFlats, Long> {

    @Query("""
        SELECT f
        FROM EntityFlats f
        LEFT JOIN FETCH f.building b
        LEFT JOIN FETCH b.region
        LEFT JOIN FETCH b.wilaya
        LEFT JOIN FETCH b.area
        LEFT JOIN FETCH f.landlord
        LEFT JOIN FETCH f.status
        WHERE f.id = :id
          AND f.deleted = false
    """)
    EntityFlats findDetailsById(@Param("id") Long id);

    @Query("""
        SELECT f
        FROM EntityFlats f
        LEFT JOIN FETCH f.building b
        LEFT JOIN FETCH b.region
        LEFT JOIN FETCH b.wilaya
        LEFT JOIN FETCH b.area
        LEFT JOIN FETCH f.landlord
        LEFT JOIN FETCH f.status
        WHERE b.id = :buildingId
          AND f.deleted = false
        ORDER BY f.floorNumber ASC, f.flatNumber ASC
    """)
    List<EntityFlats> findDetailsByBuildingId(@Param("buildingId") Long buildingId);

    @Query("""
        SELECT f
        FROM EntityFlats f
        LEFT JOIN FETCH f.building b
        LEFT JOIN FETCH b.region
        LEFT JOIN FETCH b.wilaya
        LEFT JOIN FETCH b.area
        LEFT JOIN FETCH f.landlord
        LEFT JOIN FETCH f.status
        WHERE f.landlord.id = :landlordId
          AND f.deleted = false
        ORDER BY f.id DESC
    """)
    List<EntityFlats> findDetailsByLandlordId(@Param("landlordId") Long landlordId);

    @Query("""
        SELECT f
        FROM EntityFlats f
        LEFT JOIN FETCH f.building b
        LEFT JOIN FETCH b.region
        LEFT JOIN FETCH b.wilaya
        LEFT JOIN FETCH b.area
        LEFT JOIN FETCH f.landlord
        LEFT JOIN FETCH f.status
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

    boolean existsByBuildingIdAndFlatNumberIgnoreCaseAndDeletedFalse(
            Long buildingId,
            String flatNumber
    );

    long countByBuildingIdAndDeletedFalse(Long buildingId);

    long countByBuildingIdAndStatusCodeIgnoreCaseAndDeletedFalse(
            Long buildingId,
            String statusCode
    );
}