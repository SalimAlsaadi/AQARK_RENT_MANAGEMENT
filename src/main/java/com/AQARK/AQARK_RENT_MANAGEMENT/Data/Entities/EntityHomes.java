package com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Enums.HomeType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "entity_home",
        indexes = {
                @Index(name = "IX_entity_home_landlord_id", columnList = "landlord_id"),
                @Index(name = "IX_entity_home_status_id", columnList = "status_id"),
                @Index(name = "IX_entity_home_active_deleted", columnList = "active, deleted"),
                @Index(name = "IX_entity_home_region_wilaya", columnList = "region_id, wilaya_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntityHomes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "home_type", nullable = false, length = 100)
    private HomeType homeType;

    @Column(name = "home_number", length = 50)
    private String homeNumber;

    @NotNull
    @Min(1)
    @Column(name = "number_of_rooms", nullable = false)
    private Integer numberOfRooms;

    @NotNull
    @Min(1)
    @Column(name = "number_of_bathrooms", nullable = false)
    private Integer numberOfBathrooms;

    @Column(name = "has_living_room", nullable = false)
    private boolean hasLivingRoom;

    @Column(name = "is_furnished", nullable = false)
    private boolean furnished;

    @Min(0)
    @Column(name = "number_of_floor")
    private Integer numberOfFloor;

    @Column(name = "description", length = 2000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "region_id", nullable = false)
    private OmanRegionEntity region;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wilaya_id", nullable = false)
    private OmanWilayaEntity wilaya;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "area_id", nullable = false)
    private OmanAreaEntity area;

    @DecimalMin(value = "-90.0", inclusive = true)
    @DecimalMax(value = "90.0", inclusive = true)
    @Column(name = "latitude")
    private Double latitude;

    @DecimalMin(value = "-180.0", inclusive = true)
    @DecimalMax(value = "180.0", inclusive = true)
    @Column(name = "longitude")
    private Double longitude;

    @NotBlank
    @Column(name = "tenant_type", nullable = false, length = 100)
    private String tenantType;

    @NotBlank
    @Column(name = "rent_type", nullable = false, length = 100)
    private String rentType;

    @NotNull
    @DecimalMin(value = "0.000", inclusive = true)
    @Column(name = "price", nullable = false, precision = 18, scale = 3)
    private BigDecimal price;

    /*
     * HOME status:
     * AVAILABLE
     * AVAILABLE_AS_ROOMS
     * PARTIALLY_RENTED_AS_ROOMS
     * FULLY_RENTED_AS_ROOMS
     * RENTED
     * MAINTENANCE
     * INACTIVE
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_id", nullable = false)
    private EntityUnitStatus status;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "landlord_id", nullable = false)
    private EntityUser landlord;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;


    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();

    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();

    }

}