package com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.RoomDTO;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomSaveRequestDTO {
    private String roomName;                // Optional: "Room A", etc.
    private String size;                    // e.g., "4x5m", "20 sqm"
    private boolean rented;     // true by default
    private String tenantType;
    private String rentalType;    //monthly, daily
    private Float price;                    // Optional individual room price
    private boolean hasAC;
    private boolean hasPrivateBathroom;
    private String description;

    // Only one of the following should be set (not both)
    private Long flatId;
    private Long homeId;


}
