package com.AQARK.AQARK_RENT_MANAGEMENT.Repositories;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityHomes;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HomeRepository extends JpaRepository<EntityHomes,Long> {

    Optional<EntityHomes> findByIdAndDeletedFalse(Long id);

    List<EntityHomes> findByLandlordIdAndDeletedFalseOrderByCreatedAtDesc(Long landlordId);

    List<EntityHomes> findByDeletedFalseOrderByCreatedAtDesc();

    boolean existsByIdAndLandlordIdAndDeletedFalse(
            Long homeId,
            Long landlordId
    );
}
