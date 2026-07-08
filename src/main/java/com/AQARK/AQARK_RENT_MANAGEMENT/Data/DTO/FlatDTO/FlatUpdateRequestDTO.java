package com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.FlatDTO;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlatUpdateRequestDTO {

    @NotNull(message = "Flat id is required")
    private Long flatId;

    @NotBlank(message = "Flat number is required")
    @Size(max = 50, message = "Flat number must not exceed 50 characters")
    private String flatNumber;

    @NotBlank(message = "Number of rooms is required")
    @Size(max = 50, message = "Number of rooms must not exceed 50 characters")
    private Integer numberOfRooms;

    @NotBlank(message = "Number of bathrooms is required")
    @Size(max = 50, message = "Number of bathrooms must not exceed 50 characters")
    private String numberOfBathrooms;

    private Boolean hasLivingRoom = false;

    private String statusCode;

    @NotBlank(message = "Floor number is required")
    @Size(max = 50, message = "Floor number must not exceed 50 characters")
    private String floorNumber;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @Size(max = 100, message = "Tenant type must not exceed 100 characters")
    private String tenantType;

    @Size(max = 100, message = "Rent time type must not exceed 100 characters")
    private String rentTimeType;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.000", message = "Price cannot be negative")
    private BigDecimal price;

    @Size(max = 30, message = "Mobile number must not exceed 30 characters")
    private String mobileNumber;

    private Long buildingId;
}