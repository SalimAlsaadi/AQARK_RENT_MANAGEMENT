package com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "rental_dispute",
        uniqueConstraints = {
                @UniqueConstraint(name = "UQ_rental_dispute_number", columnNames = "dispute_number")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityRentalDispute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "dispute_number",
            nullable = false,
            length = 50
    )
    private String disputeNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "request_id",
            nullable = false
    )
    private EntityRequest request;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private EntityContract contract;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "status_id",
            nullable = false
    )
    private EntityUnitStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "raised_by",
            nullable = false
    )
    private EntityUser raisedBy;

    @Column(
            name = "dispute_type",
            nullable = false,
            length = 50
    )
    private String disputeType;

    @Column(
            name = "title",
            nullable = false,
            length = 300
    )
    private String title;

    @Column(
            name = "description",
            nullable = false,
            length = 3000
    )
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private EntityUser assignedTo;

    @Column(
            name = "resolution",
            length = 3000
    )
    private String resolution;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private EntityUser resolvedBy;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

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