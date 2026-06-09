package com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "shop_rooms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EntityShoproom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id", nullable = false)
    private EntityBuildings building;

    @Column(name = "shop_number", nullable = false, length = 50)
    private String shopNumber;

    @Column(name = "commercial_type", length = 100)
    private String commercialType;

    @Column(name = "room_size", length = 50)
    private String size;

    @Column(name = "rented", nullable = false)
    private boolean rented;

    @Column(name = "price", precision = 18, scale = 3)
    private BigDecimal price;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "has_storage")
    private boolean hasStorage;

    @Column(name = "has_parking")
    private boolean hasParking;

    @Column(name = "has_bathroom")
    private boolean hasBathroom;

    @Column(name = "has_internet")
    private boolean hasInternet;

    @Column(name = "has_ac")
    private boolean hasAC;
}