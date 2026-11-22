package com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "rental_termination")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityRentalTermination {

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
            name = "requested_termination_date",
            nullable = false
    )
    private LocalDate requestedTerminationDate;

    @Column(name = "actual_termination_date")
    private LocalDate actualTerminationDate;

    @Column(
            name = "reason",
            nullable = false,
            length = 2000
    )
    private String reason;

    @Column(
            name = "penalty_amount",
            nullable = false,
            precision = 12,
            scale = 3
    )
    private BigDecimal penaltyAmount = BigDecimal.ZERO;

    @Column(
            name = "decision_comment",
            length = 2000
    )
    private String decisionComment;

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

        if (penaltyAmount == null) {
            penaltyAmount = BigDecimal.ZERO;
        }
    }
}