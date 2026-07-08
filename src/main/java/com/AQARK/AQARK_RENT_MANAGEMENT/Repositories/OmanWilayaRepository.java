package com.AQARK.AQARK_RENT_MANAGEMENT.Repositories;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.OmanWilayaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OmanWilayaRepository extends JpaRepository<OmanWilayaEntity, Long> {

    List<OmanWilayaEntity> findByRegionIdAndActiveTrueOrderByDisplayOrderAscNameEnAsc(Long regionId);
    OmanWilayaEntity findByIdAndRegionId(Long wilayaId, Long regionId);
}