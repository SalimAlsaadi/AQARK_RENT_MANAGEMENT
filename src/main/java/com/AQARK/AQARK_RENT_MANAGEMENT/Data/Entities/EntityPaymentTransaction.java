package com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities;


import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "payment_transaction",
        uniqueConstraints = {
                @UniqueConstraint(name = "UQ_rental_payment_transaction_number", columnNames = "transaction_number")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityPaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "payment_id",
            nullable = false
    )
    private EntityPayment payment;

    @Column(
            name = "transaction_number",
            nullable = false,
            length = 100
    )
    private String transactionNumber;

    @Column(
            name = "transaction_type",
            nullable = false,
            length = 30
    )
    private String transactionType;

    @Column(
            name = "amount",
            nullable = false,
            precision = 12,
            scale = 3
    )
    private BigDecimal amount;

    @Column(
            name = "payment_method",
            length = 30
    )
    private String paymentMethod;

    @Column(
            name = "provider",
            length = 100
    )
    private String provider;

    @Column(
            name = "provider_transaction_id",
            length = 200
    )
    private String providerTransactionId;

    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    private String status;

    @Column(
            name = "failure_code",
            length = 100
    )
    private String failureCode;

    @Column(
            name = "failure_reason",
            length = 1000
    )
    private String failureReason;

    @Column(
            name = "initiated_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime initiatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private EntityUser createdBy;

    @PrePersist
    protected void onCreate() {

        if (initiatedAt == null) {
            initiatedAt = LocalDateTime.now();
        }
    }
}