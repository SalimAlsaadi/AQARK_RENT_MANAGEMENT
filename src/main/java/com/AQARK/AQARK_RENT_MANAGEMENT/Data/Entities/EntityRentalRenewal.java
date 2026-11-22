package com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "rental_renewal")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityRentalRenewal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "request_id",
            nullable = false
    )
    private EntityRequest request;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "contract_id",
            nullable = false
    )
    private EntityContract contract;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "status_id",
            nullable = false
    )
    private EntityUnitStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "requested_by",
            nullable = false
    )
    private EntityUser requestedBy;

    @Column(
            name = "old_end_date",
            nullable = false
    )
    private LocalDate oldEndDate;

    @Column(
            name = "new_end_date",
            nullable = false
    )
    private LocalDate newEndDate;

    @Column(
            name = "old_rent_amount",
            nullable = false,
            precision = 12,
            scale = 3
    )
    private BigDecimal oldRentAmount;

    @Column(
            name = "new_rent_amount",
            nullable = false,
            precision = 12,
            scale = 3
    )
    private BigDecimal newRentAmount;

    @Column(
            name = "comment",
            length = 2000
    )
    private String comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_by")
    private EntityUser decidedBy;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}