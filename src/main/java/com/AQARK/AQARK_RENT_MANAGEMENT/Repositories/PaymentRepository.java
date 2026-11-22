package com.AQARK.AQARK_RENT_MANAGEMENT.Repositories;


import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository
        extends JpaRepository<EntityPayment, Long> {

    Optional<EntityPayment> findByPaymentNumber(String paymentNumber);

    List<EntityPayment> findAllByRequestIdOrderByCreatedAtDesc(Long requestId);

    List<EntityPayment> findAllByContractIdOrderByDueDateAsc(Long contractId);

    List<EntityPayment> findAllByContractIdAndStatusId(Long contractId, Long statusId);

    List<EntityPayment> findAllByStatusIdAndDueDateBefore(Long statusId, LocalDate date);

    boolean existsByContractIdAndPaymentTypeAndBillingPeriodFromAndBillingPeriodTo(Long contractId, String paymentType, LocalDate from, LocalDate to);
}