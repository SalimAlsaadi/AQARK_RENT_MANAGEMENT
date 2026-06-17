package com.AQARK.AQARK_RENT_MANAGEMENT.Repositories;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.OmanRegionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OmanRegionRepository extends JpaRepository<OmanRegionEntity, Long> {

    List<OmanRegionEntity> findByActiveTrueOrderByDisplayOrderAscNameEnAsc();
}