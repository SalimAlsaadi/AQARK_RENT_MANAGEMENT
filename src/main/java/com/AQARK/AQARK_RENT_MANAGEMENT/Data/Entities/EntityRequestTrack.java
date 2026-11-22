package com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "request_track")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityRequestTrack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "rental_request_id",
            nullable = false
    )
    private EntityRequest rentalRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "action_id",
            nullable = false
    )
    private EntityAction action;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "status_id",
            nullable = false
    )
    private EntityUnitStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by")
    private EntityUser performedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    private EntityRole role;

    @Column(
            name = "comment",
            length = 2000
    )
    private String comment;

    @Column(
            name = "performed_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime performedAt;

    @PrePersist
    protected void onCreate() {

        if (performedAt == null) {
            performedAt = LocalDateTime.now();
        }
    }
}