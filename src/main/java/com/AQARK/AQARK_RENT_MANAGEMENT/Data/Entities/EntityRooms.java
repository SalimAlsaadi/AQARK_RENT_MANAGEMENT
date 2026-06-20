package com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "rooms",
        indexes = {
                @Index(name = "IX_rooms_building_id", columnList = "building_id"),
                @Index(name = "IX_rooms_flat_id", columnList = "flat_id"),
                @Index(name = "IX_rooms_home_id", columnList = "home_id"),
                @Index(name = "IX_rooms_status_id", columnList = "status_id"),
                @Index(name = "IX_rooms_active_deleted", columnList = "active, deleted")
        }
)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EntityRooms {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_number", length = 100, nullable = false)
    private String roomNumber;

    @Column(name = "room_size", length = 100)
    private String size;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "status_id", nullable = false)
    private EntityUnitStatus status;

    @Column(name = "tenant_type", length = 50)
    private String tenantType;

    @Column(name = "rent_of_type", length = 50)
    private String rentTimeType;

    @Column(name = "price", precision = 18, scale = 3)
    private BigDecimal price;

    @Column(name = "has_private_bathroom", nullable = false)
    private boolean hasPrivateBathroom = false;

    @Column(name = "has_ac", nullable = false)
    private boolean hasAC = false;

    @Column(name = "has_wifi", nullable = false)
    private boolean hasWIFI = false;

    @Column(name = "kitchen", length = 100)
    private String kitchen;

    @Column(name = "is_furnished", nullable = false)
    private boolean isFurnished = false;

    @Column(name = "description", length = 1000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id")
    private EntityBuildings building;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flat_id")
    private EntityFlats flat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "home_id")
    private EntityHomes home;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @AssertTrue(message = "Room must belong either to building + flat, or to home only.")
    public boolean isParentValid() {
        boolean belongsToFlat =
                building != null &&
                        flat != null &&
                        home == null;

        boolean belongsToHome =
                building == null &&
                        flat == null &&
                        home != null;

        return belongsToFlat || belongsToHome;
    }


    //later i will use BaseEntity instead this
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();

        if (this.roomNumber != null) {
            this.roomNumber = this.roomNumber.trim();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();

        if (this.roomNumber != null) {
            this.roomNumber = this.roomNumber.trim();
        }
    }
}