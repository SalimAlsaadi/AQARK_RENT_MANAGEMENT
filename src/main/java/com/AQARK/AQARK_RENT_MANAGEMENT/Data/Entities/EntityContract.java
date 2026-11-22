package com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "contract",
        uniqueConstraints = {
                @UniqueConstraint(name = "UQ_rental_contract_number", columnNames = "contract_number"),
                @UniqueConstraint(name = "UQ_rental_contract_request", columnNames = "request_id")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityContract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "request_id",
            nullable = false,
            unique = true
    )
    private EntityRequest request;

    @Column(
            name = "contract_number",
            nullable = false,
            length = 50
    )
    private String contractNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "status_id",
            nullable = false
    )
    private EntityUnitStatus status;

    @Column(
            name = "start_date",
            nullable = false
    )
    private LocalDate startDate;

    @Column(
            name = "end_date",
            nullable = false
    )
    private LocalDate endDate;

    @Column(
            name = "rent_amount",
            nullable = false,
            precision = 12,
            scale = 3
    )
    private BigDecimal rentAmount;

    @Column(
            name = "rent_type_time",
            nullable = false,
            length = 30
    )
    private String rentTypeTime;

    @Column(
            name = "terms_and_conditions",
            columnDefinition = "nvarchar(max)"
    )
    private String termsAndConditions;

    @Column(
            name = "tenant_signed",
            nullable = false
    )
    private Boolean tenantSigned = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_signed_by")
    private EntityUser tenantSignedBy;

    @Column(name = "tenant_signed_at")
    private LocalDateTime tenantSignedAt;

    @Column(
            name = "landlord_signed",
            nullable = false
    )
    private Boolean landlordSigned = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "landlord_signed_by")
    private EntityUser landlordSignedBy;

    @Column(name = "landlord_signed_at")
    private LocalDateTime landlordSignedAt;

    @Column(name = "activated_at")
    private LocalDateTime activatedAt;

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

        if (tenantSigned == null) {
            tenantSigned = false;
        }

        if (landlordSigned == null) {
            landlordSigned = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}