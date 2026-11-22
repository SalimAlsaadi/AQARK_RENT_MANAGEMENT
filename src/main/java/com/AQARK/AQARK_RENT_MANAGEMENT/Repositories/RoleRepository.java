package com.AQARK.AQARK_RENT_MANAGEMENT.Repositories;


import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<EntityRole, Long> {

    Optional<EntityRole> findByCode(String code);

    Optional<EntityRole> findByCodeAndActiveTrue(String code);

    List<EntityRole> findAllByActiveTrueOrderByIdAsc();

    boolean existsByCode(String code);
}