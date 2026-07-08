package com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.FlatDTO;

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
    private Integer numberOfRooms;
    private String numberOfBathrooms;
    private Boolean hasLivingRoom;

    private FlatStatusDTO status;

    private String floorNumber;

    private String description;

    private Long regionId;
    private String region;
    private String regionAr;

    private Long wilayaId;
    private String wilaya;
    private String wilayaAr;

    private Long areaId;
    private String area;
    private String areaAr;

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
    private String buildingNumber;

    private Boolean hasParking;
    private Boolean hasGym;
    private Boolean hasSwimmingPool;

    private List<String> images;
}