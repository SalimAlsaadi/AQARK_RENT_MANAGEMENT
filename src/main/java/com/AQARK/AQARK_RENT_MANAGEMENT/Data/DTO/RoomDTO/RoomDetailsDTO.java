package com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.RoomDTO;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.MediaDTO.PropertyMediaDTO;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomDetailsDTO {

    private Long id;

    private String roomName;
    private String size;

    private Boolean rented;
    private String tenantType;
    private String rentalType;

    private BigDecimal price;

    private Boolean hasPrivateBathroom;
    private String description;

    private String region;
    private String wilaya;
    private String location;

    private Double latitude;
    private Double longitude;

    private Boolean fromFlat;
    private Long flatId;
    private Long homeId;
    private Long buildingId;

    private String buildingName;
    private String flatLabel;
    private String homeLabel;

    private List<PropertyMediaDTO> images;
}