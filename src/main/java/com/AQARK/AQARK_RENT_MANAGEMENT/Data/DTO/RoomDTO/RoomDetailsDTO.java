package com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.RoomDTO;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RoomDetailsDTO {
    private Long id;

    private String description;
    private Float price;
    private boolean isRented;
    private String tenantType;        // e.g., Family, Bachelor
    private String rentalType;    //monthly, daily

    // Address-related
    private String region;
    private String wilaya;
    private String location;          // e.g., Flat X, Building Y or Home address

    private Double latitude;
    private Double longitude;

    // Source of the room
    private boolean fromFlat;         // true if it's from flat/building, false if from home
    private Long sourceId;            // either flatId or homeId depending on `fromFlat`

    private List<String> pictureUrls; // Transformed URLs of room images
}
