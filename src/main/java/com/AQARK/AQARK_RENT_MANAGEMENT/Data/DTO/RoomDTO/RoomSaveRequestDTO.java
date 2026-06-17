package com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.RoomDTO;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomSaveRequestDTO {

    @NotBlank(message = "Room number is required")
    @Size(max = 100)
    private String roomNumber;

    @Size(max = 100)
    private String size;

    private String statusCode;

    @Size(max = 50)
    private String tenantType;

    @Size(max = 50)
    private String rentTimeType;

    @DecimalMin(value = "0.000", message = "Price cannot be negative")
    private BigDecimal price;

    private Boolean hasPrivateBathroom = false;
    private Boolean hasAC = false;
    private Boolean hasWIFI = false;
    private Boolean isFurnished = false;

    @Size(max = 100)
    private String kitchen;

    @Size(max = 1000)
    private String description;

    /*
     * Required when room belongs to flat.
     */
    private Long buildingId;

    /*
     * Required when room belongs to flat.
     */
    private Long flatId;

    /*
     * Required when room belongs to standalone home.
     */
    private Long homeId;
}