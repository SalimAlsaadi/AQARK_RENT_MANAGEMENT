package com.AQARK.AQARK_RENT_MANAGEMENT.Services.Implementation;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.BuildingDTO.BuildingSaveRequestDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.FlatDTO.FlatSaveRequestDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.HomeDTO.HomeCreateRequestDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.RoomDTO.RoomCreateRequestDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Enums.PropertyType;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.AQARK.AQARK_RENT_MANAGEMENT.Exceptions.FieldValidationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PropertyCommonServices implements PropertyCommonServiceInterface {

    private final ObjectMapper objectMapper;
    private final Validator validator;

    private final BuildingServicesInterface buildingService;
    private final FlatsServicesInterface flatService;
    private final HomeServiceInterface homeService;
    private final RoomsServicesInterface roomService;

    // private final ShopServiceInterface shopService;

    @Override
    public Object create(
            String propertyType,
            String data,
            List<MultipartFile> pictures,
            Long landlordId
    ) throws IOException {

        PropertyType type = parsePropertyType(propertyType);

        if (data == null || data.trim().isEmpty()) {
            throw new IllegalArgumentException("Property data is required.");
        }

        if (landlordId == null || landlordId <= 0) {
            throw new IllegalArgumentException("Landlord id is required.");
        }

        List<MultipartFile> safePictures = pictures == null ? Collections.emptyList() : pictures;

        return switch (type) {

            case BUILDING -> buildingService.createBuilding(
                    readAndValidate(data, BuildingSaveRequestDTO.class),
                    safePictures,
                    landlordId
            );

            case FLAT -> flatService.createFlat(
                    readAndValidate(data, FlatSaveRequestDTO.class),
                    safePictures,
                    landlordId
            );

            case HOME -> homeService.createHome(
                    readAndValidate(data, HomeCreateRequestDTO.class),
                    safePictures,
                    landlordId
            );

            case ROOM -> roomService.createRoom(
                    readAndValidate(data, RoomCreateRequestDTO.class),
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



    private <T> T readAndValidate(
            String data,
            Class<T> dtoClass
    ) throws JsonProcessingException {

        T dto = objectMapper.readValue(data, dtoClass);

        validateDto(dto);

        return dto;
    }

    private <T> void validateDto(T dto) {

        Map<String, String> fieldErrors = validator.validate(dto)
                .stream()
                .collect(
                        Collectors.toMap(
                                violation -> violation.getPropertyPath().toString(),
                                ConstraintViolation::getMessage,
                                (first, second) -> first
                        )
                );

        if (!fieldErrors.isEmpty()) {
            throw new FieldValidationException(
                    "Please complete all required fields correctly.",
                    fieldErrors
            );
        }
    }
}