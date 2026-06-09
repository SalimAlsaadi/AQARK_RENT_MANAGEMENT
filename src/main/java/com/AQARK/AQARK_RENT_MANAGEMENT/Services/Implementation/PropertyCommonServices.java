package com.AQARK.AQARK_RENT_MANAGEMENT.Services.Implementation;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.BuildingDTO.BuildingSaveRequestDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.FlatDTO.FlatSaveRequestDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.HomeDTO.HomeSaveRequestDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.RoomDTO.RoomSaveRequestDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PropertyCommonServices implements PropertyCommonServiceInterface {

    private final ObjectMapper objectMapper;
    private final BuildingServicesInterface buildingService;
    private final FlatsServicesInterface flatService;
    private final HomeServiceInterface homeService;
    private final RoomsServicesInterface roomService;
    //private final ShopService shopService;

    public Object create(String propertyType, String data, List<MultipartFile> pictures, Long landlord ) throws IOException {

        return switch (propertyType.toUpperCase()) {

            case "BUILDING" -> buildingService.createBuilding(
                    objectMapper.readValue(data, BuildingSaveRequestDTO.class),
                    pictures,
                     landlord
            );

            case "FLAT" -> flatService.createFlat(
                    objectMapper.readValue(data, FlatSaveRequestDTO.class),
                    pictures,
                    landlord
            );

            case "HOME" -> homeService.createHome(
                    objectMapper.readValue(data, HomeSaveRequestDTO.class),
                    pictures,
                    landlord
            );

            case "ROOM" -> roomService.createRoom(
                    objectMapper.readValue(data, RoomSaveRequestDTO.class),
                    pictures
            );

//            case "SHOP" -> shopService.createShop(
//                    objectMapper.readValue(data, ShopSaveRequestDTO.class),
//                    pictures
//            );

            default -> throw new IllegalArgumentException(
                    "Unsupported property type: " + propertyType
            );
        };
    }
}

