package com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.BuildingDTO;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BuildingSaveRequestDTO {

    private Long id;
    private String buildingName;
    private String region;
    private String wilaya;
    private String area;

    private Integer numberOfFloors;

    private Boolean hasParking;
    private Boolean hasGym;
    private Boolean hasSwimmingPool;

    private String description;
    private Double latitude;
    private Double longitude;

    private Long landlordId;
}