package com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "entity_flats",
        indexes = {
                @Index(name = "IX_entity_flats_building_id", columnList = "building_id"),
                @Index(name = "IX_entity_flats_home_id", columnList = "home_id"),
                @Index(name = "IX_entity_flats_landlord_id", columnList = "landlord_id"),
                @Index(name = "IX_entity_flats_status_id", columnList = "status_id"),
                @Index(name = "IX_entity_flats_price", columnList = "price"),
                @Index(name = "IX_entity_flats_floor_number", columnList = "floor_number"),
                @Index(name = "IX_entity_flats_active_deleted", columnList = "active, deleted")
        }
)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class EntityFlats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "flat_number", nullable = false, length = 50)
    private String flatNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id")
    private EntityBuildings building;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "home_id")
    private EntityHomes home;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "landlord_id", nullable = false)
    private EntityUser landlord;

    @OneToMany(mappedBy = "flat", fetch = FetchType.LAZY)
    @Builder.Default
    private List<EntityRooms> rooms = new ArrayList<>();

    @Column(name = "number_of_rooms", nullable = false, length = 50)
    private Integer numberOfRooms;

    @Column(name = "number_of_bathrooms", nullable = false, length = 50)
    private String numberOfBathrooms;

    @Column(name = "is_furnished", nullable = false)
    private boolean isFurnished;

    @Column(name = "has_ac", nullable = false)
    private boolean hasAC;

    @Column(name = "has_wifi", nullable = false)
    private boolean hasWIFI;

    @Column(name = "has_living_room", nullable = false)
    private boolean hasLivingRoom;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "status_id", nullable = false)
    private EntityUnitStatus status;

    @Column(name = "floor_number", nullable = false, length = 50)
    private String floorNumber;

    @Column(name = "tenant_type", length = 100)
    private String tenantType;

    @Lob
    @Column(name = "description")
    private String description;

    @Column(name = "type_of_rent", length = 100)
    private String rentalType;

    @Column(name = "price", precision = 18, scale = 3)
    private BigDecimal price;

    @Column(name = "landlord_mobile_number", length = 30)
    private String landlordMobileNumber;

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

    //later i will use BaseEntity instead this
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();

        if (this.flatNumber != null) {
            this.flatNumber = this.flatNumber.trim();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();

        if (this.flatNumber != null) {
            this.flatNumber = this.flatNumber.trim();
        }
    }

    @AssertTrue(message = "Flat must belong to exactly one building or one home.")
    public boolean isParentValid() {
        boolean inBuilding = building != null && home == null;
        boolean inHome = building == null && home != null;
        return inBuilding || inHome;
    }
}