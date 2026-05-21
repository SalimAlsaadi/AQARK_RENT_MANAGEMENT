package com.AQARK.AQARK_RENT_MANAGEMENT.Repositories;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityUserTypeMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserTypeMappingRepository extends JpaRepository<EntityUserTypeMapping, Long> {

    List<EntityUserTypeMapping> findByUserIdAndIsActiveTrue(Long userId);

    boolean existsByUserIdAndUserTypeCodeAndIsActiveTrue(Long userId, String code);

    List<EntityUserTypeMapping> findByUserTypeCodeAndIsActiveTrue(String code);
}
