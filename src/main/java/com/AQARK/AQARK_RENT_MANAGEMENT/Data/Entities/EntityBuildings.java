package com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "buildings")
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class EntityBuildings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 200)
    @Column(name = "building_name", nullable = false)
    private String buildingName;

    @Column(name = "region")
    private String region;

    @Column(name = "wilaya")
    private String wilaya;

    @Column(name = "Area")
    private String area;

    @Column(name = "has_parking", nullable = false)
    private boolean hasParking;

    @Column(name = "has_gym", nullable = false)
    private boolean hasGym;

    @Column(name = "has_swimming_pool", nullable = false)
    private boolean hasSwimmingPool;

    @Column(name = "number_of_floors")
    private int number_of_floors;

    @Column(name = "description", length = 500)
    private String description;

    // Map location fields
    @Column(name = "latitude", nullable = true)
    private Double latitude;

    @Column(name = "longitude", nullable = true)
    private Double longitude;

    @ManyToOne
    @JoinColumn(name = "landlord_id", nullable = false)
    private EntityUser landlord;

    @OneToMany(mappedBy = "building", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EntityFlats> flats;

}
