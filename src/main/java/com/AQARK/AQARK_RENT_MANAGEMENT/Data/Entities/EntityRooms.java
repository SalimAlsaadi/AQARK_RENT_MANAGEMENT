package com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import lombok.*;

@Entity
@Table(name = "rooms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntityRooms {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String roomName; // Optional: "Room A", "Master Bedroom", etc.

    @Column(name = "room_size")
    private String size; // e.g., "4x5m", "20 sqm"

    @Column(name = "rented", nullable = false)
    private boolean rented = true;

    @Column(name = "tenantType")
    private String tenantType;

    @Column(name = "rentalType")
    private String rentalType;    //monthly, daily

    @Column(name = "price")
    private Float price; // Optional: if rooms are rented individually


    @Column(name = "has_private_bathroom")
    private boolean hasPrivateBathroom;

    private String description;

    // Optional: images for the room
    @ElementCollection
    @CollectionTable(name = "room_pictures", joinColumns = @JoinColumn(name = "room_id"))
    @Column(name = "picture_path")
    private java.util.List<String> picturePaths;

    // One of the following should be populated:
    @ManyToOne
    @JoinColumn(name = "flat_id")
    private EntityFlats flat;

    @ManyToOne
    @JoinColumn(name = "home_id")
    private EntityHomes home;

    @AssertTrue(message = "Room must belong to either a flat or a home, not both or neither.")//validation to ensure a room belong to flat or home
    public boolean isParentValid() {
        return (flat != null) ^ (home != null); // XOR: one is set, not both
    }


    // Optional: computed method for context info
    public String getContextAddress() {
        if (flat != null && flat.getBuilding() != null) {
            return "Building: " + flat.getBuilding().getBuildingName()
                    + ", Flat Floor: " + flat.getFloorNumber()
                    + ", Region: " + flat.getRegion()
                    + ", Wilaya: " + flat.getWilaya();
        } else if (home != null) {
            return "Home: " + home.getRegion()
                    + ", Wilaya: " + home.getWilaya();
        } else {
            return "Unknown Location";
        }
    }
}
