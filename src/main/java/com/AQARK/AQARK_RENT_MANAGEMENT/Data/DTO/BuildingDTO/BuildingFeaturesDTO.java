package com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.BuildingDTO;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BuildingFeaturesDTO {

    private boolean hasParking;
    private boolean hasGym;
    private boolean hasSwimmingPool;
}