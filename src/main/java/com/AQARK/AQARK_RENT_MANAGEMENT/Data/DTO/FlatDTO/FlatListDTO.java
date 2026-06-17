package com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.FlatDTO;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlatListDTO {

    private Long flatId;

    private String flatNumber;
    private String floorNumber;

    private String numberOfRooms;
    private String numberOfBathrooms;

    private BigDecimal price;

    private String statusCode;
    private String statusNameEn;
    private String statusNameAr;
    private String statusColorCode;
    private String statusIcon;
}