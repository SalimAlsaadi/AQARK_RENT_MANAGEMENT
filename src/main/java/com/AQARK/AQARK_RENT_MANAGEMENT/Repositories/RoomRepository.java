package com.AQARK.AQARK_RENT_MANAGEMENT.Repositories;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.UnitStatusCountsProjection;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityRooms;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<EntityRooms, Long> {

    @Query("""
        SELECT DISTINCT r FROM EntityRooms r
        LEFT JOIN FETCH r.status
        LEFT JOIN FETCH r.building
        LEFT JOIN FETCH r.home
        LEFT JOIN FETCH r.flat f
        LEFT JOIN FETCH f.building
        LEFT JOIN FETCH f.home
        WHERE r.id = :roomId AND r.deleted = false
    """)
    EntityRooms findDetailsById(@Param("roomId") Long roomId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT DISTINCT r FROM EntityRooms r
        LEFT JOIN FETCH r.status
        LEFT JOIN FETCH r.building
        LEFT JOIN FETCH r.home
        LEFT JOIN FETCH r.flat f
        LEFT JOIN FETCH f.building
        LEFT JOIN FETCH f.home
        WHERE r.id = :roomId AND r.deleted = false
    """)
    Optional<EntityRooms> findByIdForUpdate(@Param("roomId") Long roomId);

    @Query("""
        SELECT r FROM EntityRooms r
        LEFT JOIN FETCH r.status
        LEFT JOIN FETCH r.building b
        LEFT JOIN FETCH b.region LEFT JOIN FETCH b.wilaya LEFT JOIN FETCH b.area
        LEFT JOIN FETCH r.flat LEFT JOIN FETCH r.home
        WHERE b.id = :buildingId AND r.deleted = false
        ORDER BY r.roomNumber ASC
    """)
    List<EntityRooms> findDetailsByBuildingId(@Param("buildingId") Long buildingId);

    @Query("""
        SELECT r FROM EntityRooms r
        LEFT JOIN FETCH r.status
        LEFT JOIN FETCH r.building b
        LEFT JOIN FETCH b.region LEFT JOIN FETCH b.wilaya LEFT JOIN FETCH b.area
        LEFT JOIN FETCH r.flat LEFT JOIN FETCH r.home
        WHERE r.flat.id = :flatId AND r.deleted = false
        ORDER BY r.roomNumber ASC
    """)
    List<EntityRooms> findDetailsByFlatId(@Param("flatId") Long flatId);

    @Query("""
        SELECT r FROM EntityRooms r
        LEFT JOIN FETCH r.status
        LEFT JOIN FETCH r.home
        LEFT JOIN FETCH r.building b
        LEFT JOIN FETCH b.region LEFT JOIN FETCH b.wilaya LEFT JOIN FETCH b.area
        LEFT JOIN FETCH r.flat
        WHERE r.home.id = :homeId AND r.deleted = false
        ORDER BY r.roomNumber ASC
    """)
    List<EntityRooms> findDetailsByHomeId(@Param("homeId") Long homeId);

    @Query("""
        SELECT r FROM EntityRooms r
        LEFT JOIN FETCH r.status
        LEFT JOIN FETCH r.building b
        LEFT JOIN FETCH b.region LEFT JOIN FETCH b.wilaya LEFT JOIN FETCH b.area
        LEFT JOIN FETCH r.flat LEFT JOIN FETCH r.home
        WHERE r.deleted = false ORDER BY r.id DESC
    """)
    List<EntityRooms> findAllDetails();

    @Query("""
        SELECT DISTINCT r FROM EntityRooms r LEFT JOIN FETCH r.status
        WHERE r.building.id = :buildingId
          AND r.flat IS NULL AND r.home IS NULL AND r.deleted = false
    """)
    List<EntityRooms> findDirectRoomsByBuildingId(@Param("buildingId") Long buildingId);

    @Query("""
        SELECT DISTINCT r FROM EntityRooms r LEFT JOIN FETCH r.status
        WHERE r.home.id = :homeId
          AND r.flat IS NULL AND r.building IS NULL AND r.deleted = false
    """)
    List<EntityRooms> findDirectRoomsByHomeId(@Param("homeId") Long homeId);

    @Query("""
        SELECT DISTINCT r FROM EntityRooms r JOIN r.flat f LEFT JOIN FETCH r.status
        WHERE f.building.id = :buildingId AND r.deleted = false
    """)
    List<EntityRooms> findRoomsInsideBuildingFlats(@Param("buildingId") Long buildingId);

    @Query("""
        SELECT DISTINCT r FROM EntityRooms r JOIN r.flat f LEFT JOIN FETCH r.status
        WHERE f.home.id = :homeId AND r.deleted = false
    """)
    List<EntityRooms> findRoomsInsideHomeFlats(@Param("homeId") Long homeId);

    @Query("""
        SELECT COUNT(r) FROM EntityRooms r JOIN r.flat f
        WHERE f.home.id = :homeId AND r.active = true AND r.deleted = false
    """)
    long countRoomsInsideHomeFlats(@Param("homeId") Long homeId);

    @Query("""
        SELECT COUNT(r) FROM EntityRooms r
        WHERE r.home.id = :homeId
          AND r.flat IS NULL AND r.building IS NULL
          AND r.active = true AND r.deleted = false
          AND (:excludeRoomId IS NULL OR r.id <> :excludeRoomId)
    """)
    long countActiveDirectRoomsByHomeId(@Param("homeId") Long homeId, @Param("excludeRoomId") Long excludeRoomId);

    @Query("""
        SELECT COUNT(r) FROM EntityRooms r
        WHERE r.flat.id = :flatId
          AND r.active = true AND r.deleted = false
          AND (:excludeRoomId IS NULL OR r.id <> :excludeRoomId)
    """)
    long countActiveRoomsByFlatId(@Param("flatId") Long flatId, @Param("excludeRoomId") Long excludeRoomId);

    @Query("""
        SELECT COUNT(r.id) AS total,
               COALESCE(SUM(CASE WHEN UPPER(s.code) = 'AVAILABLE' THEN 1 ELSE 0 END), 0) AS available,
               COALESCE(SUM(CASE WHEN UPPER(s.code) = 'RENTED' THEN 1 ELSE 0 END), 0) AS rented,
               COALESCE(SUM(CASE WHEN UPPER(s.code) = 'RESERVED' THEN 1 ELSE 0 END), 0) AS reserved,
               COALESCE(SUM(CASE WHEN UPPER(s.code) = 'UNDER_MAINTENANCE' THEN 1 ELSE 0 END), 0) AS underMaintenance,
               COALESCE(SUM(CASE WHEN UPPER(s.code) = 'BLOCKED' THEN 1 ELSE 0 END), 0) AS blocked
        FROM EntityRooms r JOIN r.status s
        WHERE r.flat.id = :flatId AND r.active = true AND r.deleted = false
    """)
    UnitStatusCountsProjection getActiveRoomStatusCountsByFlatId(@Param("flatId") Long flatId);

    @Query("""
        SELECT COUNT(r.id) AS total,
               COALESCE(SUM(CASE WHEN UPPER(s.code) = 'AVAILABLE' THEN 1 ELSE 0 END), 0) AS available,
               COALESCE(SUM(CASE WHEN UPPER(s.code) = 'RENTED' THEN 1 ELSE 0 END), 0) AS rented,
               COALESCE(SUM(CASE WHEN UPPER(s.code) = 'RESERVED' THEN 1 ELSE 0 END), 0) AS reserved,
               COALESCE(SUM(CASE WHEN UPPER(s.code) = 'UNDER_MAINTENANCE' THEN 1 ELSE 0 END), 0) AS underMaintenance,
               COALESCE(SUM(CASE WHEN UPPER(s.code) = 'BLOCKED' THEN 1 ELSE 0 END), 0) AS blocked
        FROM EntityRooms r JOIN r.status s
        WHERE r.home.id = :homeId
          AND r.flat IS NULL AND r.building IS NULL
          AND r.active = true AND r.deleted = false
    """)
    UnitStatusCountsProjection getDirectHomeRoomStatusCounts(@Param("homeId") Long homeId);

    int countByFlatIdAndDeletedFalse(Long flatId);
    long countByBuildingIdAndFlatIsNullAndHomeIsNullAndActiveTrueAndDeletedFalse(Long buildingId);
    long countByHomeIdAndFlatIsNullAndBuildingIsNullAndActiveTrueAndDeletedFalse(Long homeId);
    long countByFlatIdAndActiveTrueAndDeletedFalse(Long flatId);
    int countByHomeIdAndDeletedFalse(Long homeId);
    boolean existsByFlatIdAndRoomNumberIgnoreCaseAndDeletedFalse(Long flatId, String roomNumber);
    boolean existsByHomeIdAndFlatIsNullAndBuildingIsNullAndRoomNumberIgnoreCaseAndDeletedFalse(Long homeId, String roomNumber);
    boolean existsByBuildingIdAndFlatIsNullAndHomeIsNullAndRoomNumberIgnoreCaseAndDeletedFalse(Long buildingId, String roomNumber);
    boolean existsByHomeIdAndRoomNumberIgnoreCaseAndDeletedFalse(Long homeId, String roomNumber);
    List<EntityRooms> findByBuildingIdAndFlatIsNullAndHomeIsNullAndDeletedFalse(Long buildingId);
}
