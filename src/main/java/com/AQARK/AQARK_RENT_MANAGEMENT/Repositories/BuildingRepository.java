package com.AQARK.AQARK_RENT_MANAGEMENT.Repositories;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityBuildings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BuildingRepository extends JpaRepository<EntityBuildings, Long> {

    List<EntityBuildings> findAllByLandlordId(Long landlordId);

}
