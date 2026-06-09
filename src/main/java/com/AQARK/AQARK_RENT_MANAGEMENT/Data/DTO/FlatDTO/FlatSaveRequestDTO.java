package com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.FlatDTO;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityBuildings;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityUser;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FlatSaveRequestDTO {

    private Long flatId;

    private String flat_number;
    private String numberOfRooms;
    private String numberOfBathrooms;
    private Boolean hasLivingRoom;
    private Boolean rented;
    private String floorNumber;

    private String description;
    private String tenantType;
    private String rentTimeType;

    private BigDecimal price;
    private String mobileNumber;

    private Long landlordId;
    private Long buildingId;

}