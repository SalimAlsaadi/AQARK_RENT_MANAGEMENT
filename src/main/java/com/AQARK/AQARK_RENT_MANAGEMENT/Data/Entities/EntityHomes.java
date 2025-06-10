package com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "EntityHome")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class EntityHomes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "home_type", nullable = false) //Villa, Duplex, Traditional
    private String homeType;

    @Min(1)
    @Column(name = "number_of_rooms", nullable = false)
    private int numberOfRooms;

    @Min(1)
    @Column(name = "number_of_bathrooms", nullable = false)
    private int numberOfBathrooms;

    @Column(name = "has_living_room", nullable = false)
    private boolean hasLivingRoom;

    @Column(name = "is_furnished", nullable = false)
    private boolean isFurnished;

    @Column(name = "is_rented", nullable = false)
    private boolean isRented;

    @Column(name = "floor_number")
    private Integer floorNumber;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String region;

    @Column(nullable = false)
    private String wilaya;

    @Column(name = "location", nullable = true)
    private String location;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "tenant_type", nullable = false)
    private String tenantType;

    @Column(name = "rent_type", nullable = false)
    private String rentType;

    @Column(nullable = false)
    private Float price;


    @ElementCollection
    @CollectionTable(name = "home_pictures", joinColumns = @JoinColumn(name = "home_id"))
    @Column(name = "picture_path")
    private List<String> picturePaths;

    @ManyToOne
    @JoinColumn(name = "landlord_id", nullable = false)
    private Landlord landlord;
}
