package com.AQARK.AQARK_RENT_MANAGEMENT.Repositories;


import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityContract;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ContractRepository extends JpaRepository<EntityContract, Long> {

    Optional<EntityContract> findByContractNumber(String contractNumber);

    Optional<EntityContract> findByRequestId(Long requestId);

    boolean existsByRequestId(Long requestId);

    List<EntityContract> findAllByStatusIdAndEndDateBetween(Long statusId, LocalDate from, LocalDate to);
}