package com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.FlatDTO;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class FlatSaveRequestDTO {


    @NotBlank(message = "Flat number is required.")
    @Size(max = 50, message = "Flat number cannot exceed 50 characters.")
    private String flatNumber;

    @NotNull(message = "Number of rooms is required.")
    @Min(value = 1, message = "Number of rooms must be at least 1.")
    @Max(value = 100, message = "Number of rooms cannot exceed 100.")
    private Integer numberOfRooms;

    @NotBlank(message = "Number of bathrooms is required.")
    private String numberOfBathrooms;

    private Boolean hasLivingRoom;

    private Boolean hasAC;

    private Boolean hasWIFI;

    private Boolean isFurnished;

    @NotBlank(message = "Status is required.")
    private String statusCode;

    @NotBlank(message = "Floor number is required.")
    private String floorNumber;


    @Size(max = 2000, message = "Description cannot exceed 2000 characters.")
    private String description;

    @NotBlank(message = "Tenant type is required.")
    private String tenantType;

    @NotBlank(message = "Rent time type is required.")
    private String rentTimeType;

    @NotNull(message = "Price is required.")
    @DecimalMin(value = "0.000", message = "Price cannot be negative.")
    private BigDecimal price;

    @NotBlank(message = "Mobile number is required.")
    @Pattern(regexp = "^9\\d{7}$", message = "Mobile number must be 8 digits and start with 9.")
    private String mobileNumber;

    private Long landlordId;

    private Long buildingId;

    private Long homeId;
}