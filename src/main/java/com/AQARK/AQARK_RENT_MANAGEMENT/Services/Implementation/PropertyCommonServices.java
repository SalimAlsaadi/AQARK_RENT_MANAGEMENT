package com.AQARK.AQARK_RENT_MANAGEMENT.Services.Implementation;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.BuildingDTO.BuildingSaveRequestDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.FlatDTO.FlatSaveRequestDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.HomeDTO.HomeCreateRequestDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.RoomDTO.RoomSaveRequestDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Enums.PropertyType;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PropertyCommonServices implements PropertyCommonServiceInterface {

    private final ObjectMapper objectMapper;

    private final BuildingServicesInterface buildingService;
    private final FlatsServicesInterface flatService;
    private final HomeServiceInterface homeService;
    private final RoomsServicesInterface roomService;

    // private final ShopServiceInterface shopService;

    @Override
    public Object create(String propertyType, String data, List<MultipartFile> pictures, Long landlordId) throws IOException {

        PropertyType type = parsePropertyType(propertyType);

        if (data == null || data.trim().isEmpty()) {
            throw new IllegalArgumentException("Property data is required.");
        }

        if (landlordId == null || landlordId <= 0) {
            throw new IllegalArgumentException("Landlord id is required.");
        }

        List<MultipartFile> safePictures =
                pictures == null ? Collections.emptyList() : pictures;

        return switch (type) {

            case BUILDING -> buildingService.createBuilding(
                    objectMapper.readValue(data, BuildingSaveRequestDTO.class),
                    safePictures,
                    landlordId
            );

            case FLAT -> flatService.createFlat(
                    objectMapper.readValue(data, FlatSaveRequestDTO.class),
                    safePictures,
                    landlordId
            );

            case HOME -> homeService.createHome(
                    objectMapper.readValue(data, HomeCreateRequestDTO.class),
                    safePictures,
                    landlordId
            );

            case ROOM -> roomService.createRoom(
                    objectMapper.readValue(data, RoomSaveRequestDTO.class),
                    safePictures,
                    landlordId
            );

            case SHOP -> throw new UnsupportedOperationException(
                    "SHOP property type is not implemented yet."
            );
        };
    }

    private PropertyType parsePropertyType(String propertyType) {
        if (propertyType == null || propertyType.trim().isEmpty()) {
            throw new IllegalArgumentException("Property type is required.");
        }

        try {
            return PropertyType.valueOf(
                    propertyType.trim().toUpperCase()
            );
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Unsupported property type: " + propertyType
            );
        }
    }
}