package com.AQARK.AQARK_RENT_MANAGEMENT.Repositories;


import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityRentalTermination;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RentalTerminationRepository extends JpaRepository<EntityRentalTermination, Long> {

    List<EntityRentalTermination> findAllByContractIdOrderByCreatedAtDesc(Long contractId);

    List<EntityRentalTermination> findAllByRequestIdOrderByCreatedAtDesc(Long requestId);

    List<EntityRentalTermination> findAllByStatusIdOrderByCreatedAtDesc(Long statusId);
}