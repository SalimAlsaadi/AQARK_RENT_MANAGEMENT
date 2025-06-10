package com.AQARK.AQARK_RENT_MANAGEMENT.Repositories;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityRooms;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<EntityRooms, Long> {

    boolean existsByFlatId(Long flatId);
    boolean existsByHomeId(Long homeId);

}
