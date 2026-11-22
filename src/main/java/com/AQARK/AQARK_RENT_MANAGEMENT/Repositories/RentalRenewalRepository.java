package com.AQARK.AQARK_RENT_MANAGEMENT.Repositories;


import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityRentalRenewal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RentalRenewalRepository extends JpaRepository<EntityRentalRenewal, Long> {

    List<EntityRentalRenewal> findAllByContractIdOrderByCreatedAtDesc(Long contractId);

    List<EntityRentalRenewal> findAllByRequestIdOrderByCreatedAtDesc(Long requestId);

    List<EntityRentalRenewal> findAllByStatusIdOrderByCreatedAtDesc(Long statusId);
}