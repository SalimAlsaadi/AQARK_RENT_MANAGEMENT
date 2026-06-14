package com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.RoomDTO;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomSaveRequestDTO {

    private String roomNumber;

    private String size;

    private Boolean rented;

    private String tenantType;

    private String rentTimeType;

    private BigDecimal price;

    private Boolean hasPrivateBathroom;

    private Boolean hasAC;

    private Boolean hasWIFI;

    private String kitchen;

    private Boolean isFurnished;

    private String description;

    /*
     * Required only if room belongs to flat.
     */
    private Long buildingId;

    /*
     * Required only if room belongs to flat.
     */
    private Long flatId;

    /*
     * Required only if room belongs to home.
     */
    private Long homeId;
}