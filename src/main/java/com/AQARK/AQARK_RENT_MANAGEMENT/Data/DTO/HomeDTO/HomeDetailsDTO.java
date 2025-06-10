package com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.HomeDTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HomeDetailsDTO {
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
    private String landlordName;           // e.g., "Salim Al Saadi"
    private String landlordMobileNumber;   // e.g., "+96891234567"
    private List<String> pictureUrls;      // URLs to images
}
