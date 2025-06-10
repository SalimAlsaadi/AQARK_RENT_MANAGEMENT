package com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.FlatDTO;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityBuildings;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.Landlord;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class FlatSaveRequestDTO {
    private Long flatID;
    private String numberOfRooms;
    private String numberOfBathrooms;
    private Boolean hasLivingRoom;
    private boolean rented;
    private String floorNumber;
    private String description;
    private String region;
    private String wilaya;
    private String tenantType;
    private String rentTimeType;
    private Float price;
    private String mobileNumber;
    private Landlord landlordId;
    private EntityBuildings buildingId;
    private Double latitude;
    private Double longitude;

}
