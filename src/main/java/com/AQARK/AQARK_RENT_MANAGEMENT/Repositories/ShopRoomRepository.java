package com.AQARK.AQARK_RENT_MANAGEMENT.Repositories;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityShoproom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShopRoomRepository extends JpaRepository<EntityShoproom, Long> {

    List<EntityShoproom> findByBuildingIdAndDeletedFalseOrderByShopNumberAsc(
            Long buildingId
    );

    long countByBuildingIdAndDeletedFalse(Long buildingId);

    long countByBuildingIdAndStatusCodeIgnoreCaseAndDeletedFalse(
            Long buildingId,
            String statusCode
    );

    boolean existsByBuildingIdAndShopNumberIgnoreCaseAndDeletedFalse(
            Long buildingId,
            String shopNumber
    );
}