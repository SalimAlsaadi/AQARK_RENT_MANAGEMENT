package com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.HomeDTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class HomeSaveRequestDTO {
    private Long id;
    private String homeType;
    private int numberOfRooms;
    private int numberOfBathrooms;
    private boolean hasLivingRoom;
    private boolean furnished;
    private boolean rented;
    private Integer floorNumber;
    private String description;
    private String region;
    private String wilaya;
    private String location;
    private Double latitude;
    private Double longitude;
    private String tenantType;
    private String rentType;
    private Float price;
    private List<String> picturePaths;
    private String landlordMobileNumber;
}


