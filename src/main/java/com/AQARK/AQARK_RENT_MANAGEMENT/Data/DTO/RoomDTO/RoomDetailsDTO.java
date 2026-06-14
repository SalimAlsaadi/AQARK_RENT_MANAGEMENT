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

    private String roomNumber;
    private String size;

    private Boolean rented;
    private String tenantType;
    private String rentTimeType;

    private BigDecimal price;

    private Boolean hasPrivateBathroom;
    private Boolean hasAC;
    private Boolean hasWIFI;
    private Boolean isFurnished;

    private String kitchen;

    private String description;

    private String region;  //will take region from building or home
    private String wilaya; //will take wilaya from building or home
    private String area;  //will take area from building or home

    private Double latitude; //will take latitude from building or home
    private Double longitude; //will take longitude from building or home

    private Boolean fromFlat;

    private Long flatId;
    private Long homeId;
    private Long buildingId;

    private String buildingName;
    private String flatLabel;
    private String homeLabel;

    private List<PropertyMediaDTO> images;
}