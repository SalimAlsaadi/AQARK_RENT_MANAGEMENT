package com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Enums.CommercialType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "shop_rooms",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "UQ_shop_rooms_building_shop_number",
                        columnNames = {"building_id", "shop_number"}
                )
        },
        indexes = {
                @Index(name = "IX_shop_rooms_building_id", columnList = "building_id"),
                @Index(name = "IX_shop_rooms_status_id", columnList = "status_id"),
                @Index(name = "IX_shop_rooms_building_status_deleted", columnList = "building_id, status_id, deleted"),
                @Index(name = "IX_shop_rooms_active_deleted", columnList = "active, deleted")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntityShoproom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Shop must belong to a building.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "building_id", nullable = false)
    private EntityBuildings building;

    @Column(name = "shop_number", nullable = false, length = 50)
    private String shopNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "commercial_type", nullable = false, length = 100)
    private CommercialType commercialType;

    @Column(name = "room_size", length = 50)
    private String size;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "status_id", nullable = false)
    private EntityUnitStatus status;


    @Column(name = "price", precision = 18, scale = 3)
    private BigDecimal price;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "has_storage", nullable = false)
    private boolean hasStorage = false;

    @Column(name = "has_parking", nullable = false)
    private boolean hasParking = false;

    @Column(name = "has_bathroom", nullable = false)
    private boolean hasBathroom = false;

    @Column(name = "has_internet", nullable = false)
    private boolean hasInternet = false;

    @Column(name = "has_ac", nullable = false)
    private boolean hasAC = false;

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

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();

        if (this.shopNumber != null) {
            this.shopNumber = this.shopNumber.trim();
        }


    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();

        if (this.shopNumber != null) {
            this.shopNumber = this.shopNumber.trim();
        }
    }
}