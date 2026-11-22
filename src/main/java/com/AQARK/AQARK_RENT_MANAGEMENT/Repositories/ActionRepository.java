package com.AQARK.AQARK_RENT_MANAGEMENT.Repositories;


import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityAction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ActionRepository
        extends JpaRepository<EntityAction, Long> {

    Optional<EntityAction> findByCode(String code);

    Optional<EntityAction> findByCodeAndActiveTrue(String code);

    List<EntityAction> findAllByActiveTrueOrderByIdAsc();

    boolean existsByCode(String code);
}