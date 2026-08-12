package com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.HomeDTO;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Enums.HomeRentalStructure;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Enums.HomeType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class HomeListDTO {

    private Long id;

    private HomeType homeType;
    private HomeRentalStructure rentalStructure;
    private String homeNumber;

    private String region;
    private String wilaya;
    private String area;

    private String statusCode;
    private String statusNameEn;
    private String statusNameAr;
    private String statusColorCode;
    private String statusIcon;

    private Integer numberOfFloors;

    /*
     * Original physical information entered for the home.
     * This is not the same as registered rentable child rooms.
     */
    private Integer declaredNumberOfRooms;
    private Integer numberOfBathrooms;

    private BigDecimal price;
    private String rentType;

    private long totalFlats;
    private long totalRooms;

    private long availableUnits;
    private long rentedUnits;
    private long maintenanceUnits;

    private List<String> mainImageUrls;
}