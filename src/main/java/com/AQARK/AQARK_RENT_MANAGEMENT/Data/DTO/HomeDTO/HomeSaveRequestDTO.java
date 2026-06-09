package com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.HomeDTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HomeSaveRequestDTO {

    private Long id;
    private String homeType;

    private Integer numberOfRooms;
    private Integer numberOfBathrooms;

    private Boolean hasLivingRoom;
    private Boolean furnished;
    private Boolean rented;

    private Integer floorNumber;
    private String description;

    private String region;
    private String wilaya;
    private String location;

    private Double latitude;
    private Double longitude;

    private String tenantType;
    private String rentType;

    private BigDecimal price;

    private Long landlordId;
}