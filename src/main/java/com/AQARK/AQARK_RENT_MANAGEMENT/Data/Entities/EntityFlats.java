package com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "EntityFlats")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class EntityFlats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "building_id", nullable = true)
    private EntityBuildings building;

    @ManyToOne
    @JoinColumn(name = "landlord_id", nullable = false)
    private Landlord landlord;

    @OneToMany(mappedBy = "flat", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EntityRooms> rooms;

    @Min(1)
    @Column(name = "number_of_rooms", nullable = false)
    private String numberOfRooms;

    @Min(1)
    @Column(name = "number_of_bathrooms", nullable = false)
    private String numberOfBathrooms;

    @Column(name = "has_living_room", nullable = false)
    private boolean hasLivingRoom;

    @Column(name = "is_rented", nullable = false)
    private boolean isRented;

    @Min(0)
    @Column(name = "floor_number", nullable = false)
    private String floorNumber;

    @Column(name = "tenant_type")
    private String tenantType;

    @Lob
    @Column(name = "description")
    private String description;


    @Column(name = "type_of_rent")
    private String rentalType;

    @Column(name = "region")
    private String region;

    @Column(name = "wilaya")
    private String wilaya;

    @Column(name = "price")
    private Float price;

    @Column(name = "landlord_mobile_number")
    private String landlordMobileNumber;

    @Column(name = "latitude", nullable = true)
    private Double latitude;

    @Column(name = "longitude", nullable = true)
    private Double longitude;

    @ElementCollection
    @CollectionTable(name = "hostel_pictures", joinColumns = @JoinColumn(name = "hostel_id"))
    @Column(name = "picture_path", nullable = false)
    private List<String> picturePaths;

}
