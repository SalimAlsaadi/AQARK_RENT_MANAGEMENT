package com.AQARK.AQARK_RENT_MANAGEMENT.Repositories;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.RefType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefTypeRepository extends JpaRepository<RefType, Integer> {

    Optional<RefType> findByCode(String code);

    boolean existsByCode(String code);
}
