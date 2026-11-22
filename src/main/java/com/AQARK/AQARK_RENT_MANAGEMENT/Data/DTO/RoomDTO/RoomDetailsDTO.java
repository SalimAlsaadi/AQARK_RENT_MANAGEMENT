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

    private RoomStatusDTO status;

    private String tenantType;
    private String rentTimeType;

    private BigDecimal price;

    private Boolean hasPrivateBathroom;
    private Boolean hasAC;
    private Boolean hasWIFI;
    private Boolean isFurnished;

    private String kitchen;
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

    private Double latitude;
    private Double longitude;

    private Boolean fromFlat;
    private Boolean fromHome;

    private Long flatId;
    private Long homeId;
    private Long buildingId;

    private String buildingName;
    private String buildingNumber;

    private String flatLabel;
    private String homeLabel;

    private List<PropertyMediaDTO> images;
}