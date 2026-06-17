package com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "unit_statuses",
        schema = "app_root",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "UQ_unit_statuses_domain_code",
                        columnNames = {"status_domain", "code"}
                )
        },
        indexes = {
                @Index(
                        name = "IX_unit_statuses_domain_active",
                        columnList = "status_domain, active"
                ),
                @Index(
                        name = "IX_unit_statuses_domain_code",
                        columnList = "status_domain, code"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntityUnitStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "status_domain", nullable = false, length = 50)
    private String statusDomain;

    @Column(name = "code", nullable = false, length = 100)
    private String code;

    @Column(name = "name_en", nullable = false, length = 150)
    private String nameEn;

    @Column(name = "name_ar", length = 150)
    private String nameAr;

    @Column(name = "description_en", length = 500)
    private String descriptionEn;

    @Column(name = "description_ar", length = 500)
    private String descriptionAr;

    @Column(name = "color_code", length = 30)
    private String colorCode;

    @Column(name = "icon", length = 100)
    private String icon;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;


    //later i will convert it to BaseEntity to handle createdBy, updatedBy and date
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        normalize();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        normalize();
    }

    private void normalize() {
        if (this.statusDomain != null) {
            this.statusDomain = this.statusDomain
                    .trim()
                    .toUpperCase();
        }

        if (this.code != null) {
            this.code = this.code
                    .trim()
                    .toUpperCase()
                    .replace(" ", "_")
                    .replace("-", "_");
        }
    }
}