package com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.RoomDTO;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomSaveRequestDTO {

    @NotBlank(message = "Room number is required.")
    @Size(max = 100, message = "Room number cannot exceed 100 characters.")
    private String roomNumber;

    @Size(max = 100, message = "Room size cannot exceed 100 characters.")
    private String size;

    @NotBlank(message = "Room status is required.")
    @Size(max = 100, message = "Status code cannot exceed 100 characters.")
    private String statusCode;

    @Size(max = 50, message = "Tenant type cannot exceed 50 characters.")
    private String tenantType;

    @Size(max = 50, message = "Rent time type cannot exceed 50 characters.")
    private String rentTimeType;

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

    @Size(max = 100, message = "Kitchen details cannot exceed 100 characters.")
    private String kitchen;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters.")
    private String description;

    private Long buildingId;

    private Long flatId;

    private Long homeId;
}