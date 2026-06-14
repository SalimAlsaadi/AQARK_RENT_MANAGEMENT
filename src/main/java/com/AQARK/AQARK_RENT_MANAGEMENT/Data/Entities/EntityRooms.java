package com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "rooms",
        indexes = {
                @Index(name = "idx_rooms_building_id", columnList = "building_id"),
                @Index(name = "idx_rooms_flat_id", columnList = "flat_id"),
                @Index(name = "idx_rooms_home_id", columnList = "home_id"),
                @Index(name = "idx_rooms_rented", columnList = "rented")
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

    @Column(name = "rented", nullable = false)
    private boolean rented = false;

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

    /*
     * Used only when room belongs to a flat.
     * This makes building room queries faster and cleaner.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id")
    private EntityBuildings building;

    /*
     * Used when room belongs to a flat.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flat_id")
    private EntityFlats flat;

    /*
     * Used when room belongs to a standalone home.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "home_id")
    private EntityHomes home;

    @AssertTrue(
            message = "Room must belong either to building + flat, or to home only."
    )
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
}