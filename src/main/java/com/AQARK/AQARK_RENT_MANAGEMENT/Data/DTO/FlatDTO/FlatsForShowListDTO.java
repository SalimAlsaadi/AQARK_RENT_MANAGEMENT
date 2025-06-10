package com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.FlatDTO;

import lombok.*;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
public class FlatsForShowListDTO {

    private Long id;
    private String buildingName;
    private String numberOfRooms;
    private String numberOfBathroom;
    private Boolean hasLivingroom;
    private Boolean hasGYM;
    private Boolean hasSwimmingPool;
    private Boolean hasParking;
    private String description;
    private String region;
    private String wilaya;
    private Double latitude;
    private Double longitude;
    private Float price;
    private String tenantType;
    private String rentTimeType;
    private List<String> pictures;


}
