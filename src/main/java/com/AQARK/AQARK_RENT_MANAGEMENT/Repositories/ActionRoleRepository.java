package com.AQARK.AQARK_RENT_MANAGEMENT.Repositories;


import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityActionRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActionRoleRepository extends JpaRepository<EntityActionRole, Long> {

    List<EntityActionRole> findAllByActionIdAndActiveTrue(Long actionId);

    boolean existsByActionIdAndRoleIdAndActiveTrue(Long actionId, Long roleId);
}