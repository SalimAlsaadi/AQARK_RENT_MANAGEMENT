package com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.RoomDTO;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomCreateRequestDTO {

    @NotBlank(message = "Room number is required.")
    @Size(max = 100, message = "Room number cannot exceed 100 characters.")
    private String roomNumber;

    @NotBlank(message = "Room size is required.")
    @Size(max = 100, message = "Room size cannot exceed 100 characters.")
    private String size;

    @NotBlank(message = "Room status is required.")
    @Size(max = 100, message = "Status code cannot exceed 100 characters.")
    private String statusCode;

    @NotBlank(message = "Tenant type is required.")
    @Size(max = 50, message = "Tenant type cannot exceed 50 characters.")
    private String tenantType;

    @NotBlank(message = "Rent time type is required.")
    @Size(max = 50, message = "Rent time type cannot exceed 50 characters.")
    private String rentTimeType;

    @NotNull(message = "Room price is required.")
    @DecimalMin(value = "0.000", inclusive = true, message = "Room price cannot be negative.")
    private BigDecimal price;

    @Builder.Default
    private Boolean hasPrivateBathroom = false;

    @Builder.Default
    private Boolean hasAC = false;

    @Builder.Default
    private Boolean hasWIFI = false;

    @Builder.Default
    private Boolean isFurnished = false;

    @NotBlank(message = "Kitchen details are required.")
    @Size(max = 100, message = "Kitchen details cannot exceed 100 characters.")
    private String kitchen;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters.")
    private String description;

    @Positive(message = "Building id must be greater than zero.")
    private Long buildingId;

    @Positive(message = "Flat id must be greater than zero.")
    private Long flatId;

    @Positive(message = "Home id must be greater than zero.")
    private Long homeId;
}