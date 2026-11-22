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
public class HomeUpdateRequestDTO {


    @NotNull(message = "Home id is required.")
    @Positive(message = "Home id must be greater than zero.")
    private Long id;

    private HomeType homeType;

    @Size(max = 50, message = "Home number cannot exceed 50 characters.")
    private String homeNumber;

    @Min(value = 1, message = "Number of rooms must be at least 1.")
    @Max(value = 100, message = "Number of rooms cannot exceed 100.")
    private Integer numberOfRooms;

    @Min(value = 1, message = "Number of bathrooms must be at least 1.")
    @Max(value = 100, message = "Number of bathrooms cannot exceed 100.")
    private Integer numberOfBathrooms;

    private Boolean hasLivingRoom;

    private Boolean furnished;

    @Min(value = 0, message = "Number of floors cannot be negative.")
    @Max(value = 100, message = "Number of floors cannot exceed 100.")
    private Integer numberOfFloor;

    @Size(max = 2000, message = "Description cannot exceed 2000 characters.")
    private String description;

    @Size(max = 100, message = "Tenant type cannot exceed 100 characters.")
    private String tenantType;

    @Size(max = 100, message = "Rent type cannot exceed 100 characters.")
    private String rentType;

    @DecimalMin(value = "0.000", inclusive = true, message = "Price cannot be negative.")
    @Digits(integer = 15, fraction = 3, message = "Price supports maximum 15 digits and 3 decimal places.")
    private BigDecimal price;
}