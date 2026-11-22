package com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.RoomDTO;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomUpdateRequestDTO {


    private Long roomId;

    @Size(min = 1, max = 100, message = "Room number must contain between 1 and 100 characters.")
    private String roomNumber;

    @Size(max = 100, message = "Room size cannot exceed 100 characters.")
    private String size;

    @Size(min = 1, max = 100, message = "Status code must contain between 1 and 100 characters.")
    private String statusCode;

    @Size(max = 50, message = "Tenant type cannot exceed 50 characters.")
    private String tenantType;

    @Size(max = 50, message = "Rent time type cannot exceed 50 characters.")
    private String rentTimeType;

    @DecimalMin(value = "0.000", inclusive = true, message = "Room price cannot be negative.")
    private BigDecimal price;

    private Boolean hasPrivateBathroom;
    private Boolean hasAC;
    private Boolean hasWIFI;
    private Boolean isFurnished;

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