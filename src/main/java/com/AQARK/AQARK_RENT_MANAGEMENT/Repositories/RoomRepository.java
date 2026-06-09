package com.AQARK.AQARK_RENT_MANAGEMENT.Repositories;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityRooms;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<EntityRooms, Long> {

    boolean existsByFlatId(Long flatId);
    boolean existsByHomeId(Long homeId);
    List<EntityRooms> findByHomeId(Long homeId);
    List<EntityRooms> findByFlatId(Long flatId);

    int countByFlatBuildingId(Long buildingId);//count total room in building

    int countByFlatBuildingIdAndRentedTrue(Long buildingId);//count rented room in building

    int countByFlatBuildingIdAndRentedFalse(Long buildingId);//count not rented room in building


}
