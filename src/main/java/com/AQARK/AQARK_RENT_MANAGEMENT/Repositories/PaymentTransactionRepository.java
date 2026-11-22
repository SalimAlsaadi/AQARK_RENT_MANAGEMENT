package com.AQARK.AQARK_RENT_MANAGEMENT.Repositories;


import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityPaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<EntityPaymentTransaction, Long> {

    Optional<EntityPaymentTransaction> findByTransactionNumber(String transactionNumber);

    Optional<EntityPaymentTransaction> findByProviderAndProviderTransactionId(String provider, String providerTransactionId);

    List<EntityPaymentTransaction> findAllByPaymentIdOrderByInitiatedAtDesc(Long paymentId);

    boolean existsByProviderAndProviderTransactionId(String provider, String providerTransactionId);
}