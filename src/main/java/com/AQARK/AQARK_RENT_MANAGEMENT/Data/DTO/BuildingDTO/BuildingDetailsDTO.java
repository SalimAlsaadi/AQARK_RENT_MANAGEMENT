package com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.BuildingDTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BuildingDetailsDTO {
    private Long id;
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
    private String landlordName; // e.g., "Salim Al Saadi"
    private String landLordMobileNumber;
    private List<String> pictureUrls; // image URLs instead of paths
}
