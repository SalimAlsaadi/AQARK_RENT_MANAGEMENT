package com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.HomeDTO;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.LocationDTO.LocationDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.MediaDTO.PropertyMediaDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.StatusDTO.UnitStatusDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Enums.HomeType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HomeDetailsDTO {

    private Long id;

    private HomeType homeType;
    private String homeNumber;

    private Integer numberOfRooms;
    private Integer numberOfBathrooms;
    private Boolean hasLivingRoom;
    private Boolean furnished;
    private Integer numberOfFloor;

    private String description;

    private LocationDTO location;

    private Double latitude;
    private Double longitude;

    private String tenantType;
    private String rentType;
    private BigDecimal price;

    private UnitStatusDTO status;

    private Long landlordId;
    private String landlordName;
    private String landlordMobileNumber;

    private boolean active;
    private boolean deleted;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<PropertyMediaDTO> images;
}