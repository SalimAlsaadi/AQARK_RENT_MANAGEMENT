package com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity(name = "property")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EntityProperty {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String location;
    private double rent;

    @ManyToOne
    @JoinColumn(name = "landlord_id")
    private EntityUser owner;

}

