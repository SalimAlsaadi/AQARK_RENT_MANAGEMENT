package com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "buildings",
        schema = "app_root",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "UQ_buildings_landlord_building_number",
                        columnNames = {"landlord_id", "building_number"}
                )
        },
        indexes = {
                @Index(name = "IX_buildings_landlord_id", columnList = "landlord_id"),
                @Index(name = "IX_buildings_region_id", columnList = "region_id"),
                @Index(name = "IX_buildings_wilaya_id", columnList = "wilaya_id"),
                @Index(name = "IX_buildings_area_id", columnList = "area_id"),
                @Index(name = "IX_buildings_status_id", columnList = "status_id"),
                @Index(name = "IX_buildings_active_deleted", columnList = "active, deleted")
        }
)
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class EntityBuildings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 200)
    @Column(name = "building_name", nullable = false, length = 200)
    private String buildingName;

    @NotBlank
    @Size(max = 100)
    @Column(name = "building_number", nullable = false, length = 100)
    private String buildingNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "region_id", nullable = false)
    private OmanRegionEntity region;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wilaya_id", nullable = false)
    private OmanWilayaEntity wilaya;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "area_id", nullable = false)
    private OmanAreaEntity area;

    @Column(name = "has_parking", nullable = false)
    private boolean hasParking = false;

    @Column(name = "has_gym", nullable = false)
    private boolean hasGym = false;

    @Column(name = "has_swimming_pool", nullable = false)
    private boolean hasSwimmingPool = false;

    @Column(name = "number_of_floors", nullable = false)
    private String numberOfFloors;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "landlord_id", nullable = false)
    private EntityUser landlord;

    @OneToMany(mappedBy = "building", fetch = FetchType.LAZY)
    @Builder.Default
    private List<EntityFlats> flats = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "status_id", nullable = false)
    private EntityUnitStatus status;

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

    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}