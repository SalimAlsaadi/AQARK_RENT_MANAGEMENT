package com.AQARK.AQARK_RENT_MANAGEMENT.Repositories;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.OmanAreaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OmanAreaRepository extends JpaRepository<OmanAreaEntity, Long> {

    List<OmanAreaEntity> findByWilayaIdAndActiveTrueOrderByDisplayOrderAscNameEnAsc(Long wilayaId);
}