package com.AQARK.AQARK_RENT_MANAGEMENT.Repositories;


import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityActionWorkflow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ActionWorkflowRepository extends JpaRepository<EntityActionWorkflow, Long> {

    List<EntityActionWorkflow> findAllByActionIdAndActiveTrueOrderByDisplayOrderAsc(Long actionId);

    Optional<EntityActionWorkflow> findByActionIdAndNextActionIdAndActiveTrue(Long actionId, Long nextActionId);

    Optional<EntityActionWorkflow> findByActionIdAndNextActionIsNullAndActiveTrue(Long actionId);
}