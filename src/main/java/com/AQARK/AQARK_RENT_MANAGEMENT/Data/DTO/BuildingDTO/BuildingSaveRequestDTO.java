package com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.BuildingDTO;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BuildingSaveRequestDTO {

    @NotBlank(message = "Building name is required")
    @Size(max = 200, message = "Building name must not exceed 200 characters")
    private String buildingName;

    @NotBlank(message = "Building number is required")
    @Size(max = 100, message = "Building number must not exceed 100 characters")
    private String buildingNumber;

    @NotNull(message = "Region is required")
    private Long regionId;

    @NotNull(message = "Wilaya is required")
    private Long wilayaId;

    @NotNull(message = "Area is required")
    private Long areaId;

    @NotNull(message = "Number of floors is required")
    @Min(value = 0, message = "Number of floors cannot be negative")
    private String numberOfFloors;

    private String statusCode;

    private Boolean hasParking = false;
    private Boolean hasGym = false;
    private Boolean hasSwimmingPool = false;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @DecimalMin(value = "-90.0", message = "Latitude must be valid")
    @DecimalMax(value = "90.0", message = "Latitude must be valid")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "Longitude must be valid")
    @DecimalMax(value = "180.0", message = "Longitude must be valid")
    private Double longitude;

    private Long landlordId;
}