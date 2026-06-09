package com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "entity_flats")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class EntityFlats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "flat_number")
    private String flatNumber;

    @ManyToOne
    @JoinColumn(name = "building_id", nullable = true)
    private EntityBuildings building;

    @ManyToOne
    @JoinColumn(name = "landlord_id", nullable = false)
    private EntityUser landlord;

    @OneToMany(mappedBy = "flat", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EntityRooms> rooms;

    @Column(name = "number_of_rooms", nullable = false)
    private String numberOfRooms;


    @Column(name = "number_of_bathrooms", nullable = false)
    private String numberOfBathrooms;

    @Column(name = "has_living_room", nullable = false)
    private boolean hasLivingRoom;

    @Column(name = "is_rented", nullable = false)
    private boolean isRented;

    @Column(name = "floor_number", nullable = false)
    private String floorNumber;

    @Column(name = "tenant_type")
    private String tenantType;

    @Lob
    @Column(name = "description")
    private String description;


    @Column(name = "type_of_rent")
    private String rentalType;

    @Column(name = "price")
    private BigDecimal price;

    @Column(name = "landlord_mobile_number")
    private String landlordMobileNumber;



}
