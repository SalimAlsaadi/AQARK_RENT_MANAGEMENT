package com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "request",
        uniqueConstraints = {
                @UniqueConstraint(name = "UQ_rental_request_number", columnNames = "request_number")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "request_number",
            nullable = false,
            length = 50
    )
    private String requestNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "tenant_id",
            nullable = false
    )
    private EntityUser tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "landlord_id",
            nullable = false
    )
    private EntityUser landlord;

    @Column(
            name = "property_type",
            nullable = false,
            length = 30
    )
    private String propertyType;

    @Column(
            name = "property_id",
            nullable = false
    )
    private Long propertyId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "status_id",
            nullable = false
    )
    private EntityUnitStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_action_id")
    private EntityAction currentAction;

    @Column(name = "requested_start_date")
    private LocalDate requestedStartDate;

    @Column(name = "requested_end_date")
    private LocalDate requestedEndDate;

    @Column(
            name = "comment",
            length = 2000
    )
    private String comment;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private EntityUser updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column(
            name = "version",
            nullable = false
    )
    private Long version;

    @PrePersist
    protected void onCreate() {

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}