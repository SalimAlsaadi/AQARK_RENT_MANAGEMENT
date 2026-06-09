package com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.FlatDTO;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.MediaDTO.PropertyMediaDTO;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlatDetailsDTO {

    private Long id;

    private String flatNumber;
    private String numberOfRooms;
    private String numberOfBathrooms;
    private Boolean hasLivingRoom;
    private Boolean rented;
    private String floorNumber;

    private String description;
    private String region;
    private String wilaya;
    private String area;
    private String tenantType;
    private String rentTimeType;

    private BigDecimal price;

    private Double latitude;
    private Double longitude;

    private Long landlordId;
    private String landlordName;
    private String landlordPhoneNumber;

    private Long buildingId;
    private String buildingName;

    // From building only, not saved in flat table
    private Boolean hasParking;
    private Boolean hasGym;
    private Boolean hasSwimmingPool;

    private List<String> images;
}