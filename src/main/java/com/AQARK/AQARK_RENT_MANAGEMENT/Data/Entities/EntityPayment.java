package com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities;


import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "payment",
        uniqueConstraints = {
                @UniqueConstraint(name = "UQ_rental_payment_number", columnNames = "payment_number" )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "request_id",
            nullable = false
    )
    private EntityRequest request;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private EntityContract contract;

    @Column(
            name = "payment_number",
            nullable = false,
            length = 50
    )
    private String paymentNumber;

    @Column(
            name = "payment_type",
            nullable = false,
            length = 30
    )
    private String paymentType;

    @Column(name = "billing_period_from")
    private LocalDate billingPeriodFrom;

    @Column(name = "billing_period_to")
    private LocalDate billingPeriodTo;

    @Column(
            name = "amount",
            nullable = false,
            precision = 12,
            scale = 3
    )
    private BigDecimal amount;

    @Column(
            name = "paid_amount",
            nullable = false,
            precision = 12,
            scale = 3
    )
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(
            name = "remaining_amount",
            precision = 12,
            scale = 3,
            insertable = false,
            updatable = false
    )
    private BigDecimal remainingAmount;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "status_id",
            nullable = false
    )
    private EntityUnitStatus status;

    @Column(
            name = "description",
            length = 1000
    )
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "created_by",
            nullable = false
    )
    private EntityUser createdBy;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        if (paidAmount == null) {
            paidAmount = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}