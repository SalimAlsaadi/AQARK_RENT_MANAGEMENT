package com.AQARK.AQARK_RENT_MANAGEMENT.Repositories;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityShoproom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShopRoomRepository extends JpaRepository<EntityShoproom, Long> {

    int countByBuildingId(Long buildingId);

    int countByBuildingIdAndRentedTrue(Long buildingId);

    int countByBuildingIdAndRentedFalse(Long buildingId);
}
