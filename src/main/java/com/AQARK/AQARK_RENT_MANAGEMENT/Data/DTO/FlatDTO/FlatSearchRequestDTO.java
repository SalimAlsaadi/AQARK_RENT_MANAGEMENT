package com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.FlatDTO;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlatSearchRequestDTO {

    private Long landlordId;
    private Long buildingId;

    private String statusCode;

    private String numberOfRooms;
    private String numberOfBathrooms;
    private Boolean hasLivingRoom;
    private String floorNumber;

    private String tenantType;
    private String rentTimeType;

    private BigDecimal minPrice;
    private BigDecimal maxPrice;
}