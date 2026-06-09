package com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.HomeDTO;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.MediaDTO.PropertyMediaDTO;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HomeDetailsDTO {

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
    private String landlordName;
    private String landlordMobileNumber;

    private List<PropertyMediaDTO> images;
}