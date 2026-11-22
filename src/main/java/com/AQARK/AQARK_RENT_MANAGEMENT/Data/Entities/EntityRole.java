package com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "role",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "UQ_rental_role_code",
                        columnNames = "code"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "code",
            nullable = false,
            length = 50
    )
    private String code;

    @Column(
            name = "name_en",
            nullable = false,
            length = 100
    )
    private String nameEn;

    @Column(
            name = "name_ar",
            nullable = false,
            length = 100
    )
    private String nameAr;

    @Column(
            name = "description_en",
            length = 500
    )
    private String descriptionEn;

    @Column(
            name = "description_ar",
            length = 500
    )
    private String descriptionAr;

    @Column(
            name = "role_type",
            nullable = false,
            length = 30
    )
    private String roleType;

    @Column(
            name = "active",
            nullable = false
    )
    private Boolean active = true;

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

        if (active == null) {
            active = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}