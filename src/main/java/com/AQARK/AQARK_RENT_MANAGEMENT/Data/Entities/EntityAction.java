package com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "action",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "UQ_rental_action_code",
                        columnNames = "code"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "code",
            nullable = false,
            length = 100
    )
    private String code;

    @Column(
            name = "name_en",
            nullable = false,
            length = 150
    )
    private String nameEn;

    @Column(
            name = "name_ar",
            nullable = false,
            length = 150
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
            name = "action_type",
            nullable = false,
            length = 30
    )
    private String actionType;

    @Column(
            name = "requires_comment",
            nullable = false
    )
    private Boolean requiresComment = false;

    @Column(
            name = "requires_attachment",
            nullable = false
    )
    private Boolean requiresAttachment = false;

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

        if (requiresComment == null) {
            requiresComment = false;
        }

        if (requiresAttachment == null) {
            requiresAttachment = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}