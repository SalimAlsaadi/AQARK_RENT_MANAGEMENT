package com.AQARK.AQARK_RENT_MANAGEMENT.Repositories;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityRooms;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RoomRepository extends JpaRepository<EntityRooms, Long> {

    @Query("""
        SELECT r
        FROM EntityRooms r
        LEFT JOIN FETCH r.status
        LEFT JOIN FETCH r.building b
        LEFT JOIN FETCH b.region
        LEFT JOIN FETCH b.wilaya
        LEFT JOIN FETCH b.area
        LEFT JOIN FETCH r.flat
        LEFT JOIN FETCH r.home
        WHERE r.id = :roomId
          AND r.deleted = false
    """)
    EntityRooms findDetailsById(@Param("roomId") Long roomId);

    @Query("""
        SELECT r
        FROM EntityRooms r
        LEFT JOIN FETCH r.status
        LEFT JOIN FETCH r.building b
        LEFT JOIN FETCH b.region
        LEFT JOIN FETCH b.wilaya
        LEFT JOIN FETCH b.area
        LEFT JOIN FETCH r.flat
        LEFT JOIN FETCH r.home
        WHERE b.id = :buildingId
          AND r.deleted = false
        ORDER BY r.roomNumber ASC
    """)
    List<EntityRooms> findDetailsByBuildingId(@Param("buildingId") Long buildingId);

    @Query("""
        SELECT r
        FROM EntityRooms r
        LEFT JOIN FETCH r.status
        LEFT JOIN FETCH r.building b
        LEFT JOIN FETCH b.region
        LEFT JOIN FETCH b.wilaya
        LEFT JOIN FETCH b.area
        LEFT JOIN FETCH r.flat
        LEFT JOIN FETCH r.home
        WHERE r.flat.id = :flatId
          AND r.deleted = false
        ORDER BY r.roomNumber ASC
    """)
    List<EntityRooms> findDetailsByFlatId(@Param("flatId") Long flatId);

    @Query("""
        SELECT r
        FROM EntityRooms r
        LEFT JOIN FETCH r.status
        LEFT JOIN FETCH r.home
        LEFT JOIN FETCH r.building b
        LEFT JOIN FETCH b.region
        LEFT JOIN FETCH b.wilaya
        LEFT JOIN FETCH b.area
        LEFT JOIN FETCH r.flat
        WHERE r.home.id = :homeId
          AND r.deleted = false
        ORDER BY r.roomNumber ASC
    """)
    List<EntityRooms> findDetailsByHomeId(@Param("homeId") Long homeId);

    @Query("""
        SELECT r
        FROM EntityRooms r
        LEFT JOIN FETCH r.status
        LEFT JOIN FETCH r.building b
        LEFT JOIN FETCH b.region
        LEFT JOIN FETCH b.wilaya
        LEFT JOIN FETCH b.area
        LEFT JOIN FETCH r.flat
        LEFT JOIN FETCH r.home
        WHERE r.deleted = false
        ORDER BY r.id DESC
    """)
    List<EntityRooms> findAllDetails();

    long countByFlatIdAndDeletedFalse(Long flatId);

    boolean existsByFlatIdAndRoomNumberIgnoreCaseAndDeletedFalse(Long flatId, String roomNumber);

    boolean existsByHomeIdAndRoomNumberIgnoreCaseAndDeletedFalse(Long homeId, String roomNumber);
}