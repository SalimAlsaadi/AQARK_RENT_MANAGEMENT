package com.AQARK.AQARK_RENT_MANAGEMENT.Repositories;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityUnitStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UnitStatusRepository extends JpaRepository<EntityUnitStatus, Long> {

    Optional<EntityUnitStatus> findByStatusDomainIgnoreCaseAndCodeIgnoreCaseAndActiveTrue(String statusDomain, String code);

    Optional<EntityUnitStatus> findByStatusDomainIgnoreCaseAndCodeIgnoreCase(String statusDomain, String code);

    boolean existsByStatusDomainIgnoreCaseAndCodeIgnoreCase(String statusDomain, String code);

    List<EntityUnitStatus> findByStatusDomainIgnoreCaseAndActiveTrueOrderByNameEnAsc(String statusDomain);

    List<EntityUnitStatus> findByActiveTrueOrderByStatusDomainAscNameEnAsc();

    List<EntityUnitStatus> findAllByOrderByStatusDomainAscNameEnAsc();

    Optional<EntityUnitStatus> findByStatusDomainAndCodeAndActiveTrue(String statusDomain, String code);
}