package com.AQARK.AQARK_RENT_MANAGEMENT.Repositories;


import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityRentalDispute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RentalDisputeRepository extends JpaRepository<EntityRentalDispute, Long> {

    Optional<EntityRentalDispute> findByDisputeNumber(String disputeNumber);

    List<EntityRentalDispute> findAllByRequestIdOrderByCreatedAtDesc(Long requestId);

    List<EntityRentalDispute> findAllByContractIdOrderByCreatedAtDesc(Long contractId);

    List<EntityRentalDispute> findAllByAssignedToIdAndStatusIdOrderByCreatedAtDesc(Long assignedToId, Long statusId);
}