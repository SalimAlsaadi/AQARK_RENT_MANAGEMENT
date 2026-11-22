package com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "action_workflow")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityActionWorkflow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "action_id",
            nullable = false
    )
    private EntityAction action;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "next_action_id")
    private EntityAction nextAction;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "status_id",
            nullable = false
    )
    private EntityUnitStatus status;

    @Column(
            name = "display_order",
            nullable = false
    )
    private Integer displayOrder = 1;

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

        if (displayOrder == null) {
            displayOrder = 1;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}