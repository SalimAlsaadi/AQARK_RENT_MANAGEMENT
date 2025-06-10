package com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities;


import jakarta.persistence.*;

@Entity
public class Property {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String location;
    private double rent;

    @ManyToOne
    @JoinColumn(name = "landlord_id")
    private Landlord landlord;



    // Getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public double getRent() {
        return rent;
    }

    public void setRent(double rent) {
        this.rent = rent;
    }

    public Landlord getOwner() {
        return landlord;
    }

    public void setOwner(Landlord landlord) {
        this.landlord = landlord;
    }

    @Override
    public String toString() {
        return "Property{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", location='" + location + '\'' +
                ", rent=" + rent +
                ", owner=" + landlord +
                '}';
    }
}

