package com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.BuildingDTO;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuildingDetailsDTO {

    private Long id;

    private String buildingName;
    private String buildingNumber;

    private String numberOfFloors;

    private BuildingStatusDTO status;

    private BuildingLocationDTO location;

    private BuildingFeaturesDTO features;

    private BuildingUnitCountsDTO unitCounts;

    private String description;

    private Long landlordId;
    private String landlordName;
    private String landlordMobileNumber;

    private List<String> pictureUrls;
}