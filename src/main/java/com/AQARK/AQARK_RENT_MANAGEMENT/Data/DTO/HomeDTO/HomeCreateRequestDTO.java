package com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.HomeDTO;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Enums.HomeType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class  HomeCreateRequestDTO {

    @NotNull(message = "Home type is required.")
    private HomeType homeType;

    @Size(max = 50, message = "Home number cannot exceed 50 characters.")
    private String homeNumber;

    @NotNull(message = "Number of rooms is required.")
    @Min(value = 1, message = "Number of rooms must be at least 1.")
    @Max(value = 100, message = "Number of rooms cannot exceed 100.")
    private Integer numberOfRooms;

    @NotNull(message = "Number of bathrooms is required.")
    @Min(value = 1, message = "Number of bathrooms must be at least 1.")
    @Max(value = 100, message = "Number of bathrooms cannot exceed 100.")
    private Integer numberOfBathrooms;

    @NotNull(message = "Please specify whether the home has a living room.")
    private Boolean hasLivingRoom;

    @NotNull(message = "Please specify whether the home is furnished.")
    private Boolean furnished;

    @Min(value = 0, message = "Number of floors cannot be negative.")
    @Max(value = 100, message = "Number of floors cannot exceed 100.")
    private Integer numberOfFloor;

    @Size(max = 2000, message = "Description cannot exceed 2000 characters.")
    private String description;

    @NotNull(message = "Region is required.")
    @Positive(message = "Region ID must be greater than zero.")
    private Long regionId;

    @NotNull(message = "Wilaya is required.")
    @Positive(message = "Wilaya ID must be greater than zero.")
    private Long wilayaId;

    @NotNull(message = "Area is required.")
    @Positive(message = "Area ID must be greater than zero.")
    private Long areaId;

    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90.")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90.")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180.")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180.")
    private Double longitude;

    @NotBlank(message = "Tenant type is required.")
    @Size(max = 100, message = "Tenant type cannot exceed 100 characters.")
    private String tenantType;

    @NotBlank(message = "Rent type is required.")
    @Size(max = 100, message = "Rent type cannot exceed 100 characters.")
    private String rentTimeType;

    @NotNull(message = "Price is required.")
    @DecimalMin(value = "0.000", inclusive = true, message = "Price cannot be negative.")
    @Digits(integer = 15, fraction = 3, message = "Price supports maximum 15 digits and 3 decimal places.")
    private BigDecimal price;

    /*
     * Optional.
     * If null, system uses HOME / AVAILABLE.
     * Allowed manually: DRAFT, AVAILABLE, MAINTENANCE, INACTIVE.
     */
    @NotNull
    private Long statusId;
}