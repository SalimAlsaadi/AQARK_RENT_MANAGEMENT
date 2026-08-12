package com.AQARK.AQARK_RENT_MANAGEMENT.Repositories;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityUnitStatus;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Enums.StatusLevel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UnitStatusRepository extends JpaRepository<EntityUnitStatus, Long> {

    Optional<EntityUnitStatus> findByStatusDomainIgnoreCaseAndCodeIgnoreCaseAndActiveTrue(
            String statusDomain,
            String code
    );

    Optional<EntityUnitStatus> findByStatusDomainAndCodeAndActiveTrue(
            String statusDomain,
            String code
    );

    Optional<EntityUnitStatus> findByStatusDomainIgnoreCaseAndCodeIgnoreCaseAndStatusLevelAndActiveTrue(
            String statusDomain,
            String code,
            StatusLevel statusLevel
    );

    Optional<EntityUnitStatus> findByStatusDomainIgnoreCaseAndCodeIgnoreCase(
            String statusDomain,
            String code
    );

    boolean existsByStatusDomainIgnoreCaseAndCodeIgnoreCase(
            String statusDomain,
            String code
    );

    List<EntityUnitStatus> findByStatusDomainIgnoreCaseAndActiveTrueOrderByNameEnAsc(
            String statusDomain
    );

    List<EntityUnitStatus> findByStatusDomainIgnoreCaseAndStatusLevelAndActiveTrueOrderByNameEnAsc(
            String statusDomain,
            StatusLevel statusLevel
    );

    List<EntityUnitStatus> findByActiveTrueOrderByStatusDomainAscStatusLevelAscNameEnAsc();

    List<EntityUnitStatus> findAllByOrderByStatusDomainAscStatusLevelAscNameEnAsc();
}
