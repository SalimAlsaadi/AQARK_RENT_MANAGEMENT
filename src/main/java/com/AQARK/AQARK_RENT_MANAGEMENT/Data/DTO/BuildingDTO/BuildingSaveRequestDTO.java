package com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.BuildingDTO;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BuildingSaveRequestDTO {
    private String buildingName;
    private String region;
    private String wilaya;
    private int totalFlats;
    private int flatsRented;
    private int flatsNotRented;
    private int numberOfFloors;
    private boolean hasParking;
    private boolean hasGym;
    private boolean hasSwimmingPool;
    private String description;
    private Double latitude;
    private Double longitude;
    private Long landlord;
}
