package com.AQARK.AQARK_RENT_MANAGEMENT.Services.Implementation;

import com.AQARK.AQARK_RENT_MANAGEMENT.Common_Utilities.PropertyMediaService;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.FlatDTO.*;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.MediaDTO.PropertyMediaDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.*;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.StatusDomain;
import com.AQARK.AQARK_RENT_MANAGEMENT.Repositories.*;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface.FlatsServicesInterface;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface.UnitStatusServiceInterface;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FlatsServicesImpl implements FlatsServicesInterface {

    private final FlatsRepository flatsRepository;
    private final UserRepository userRepository;
    private final BuildingRepository buildingRepository;
    private final RoomRepository roomRepository;
    private final PropertyMediaService propertyMediaService;
    private final UnitStatusServiceInterface unitStatusService;

    @Override
    @Transactional
    public FlatDetailsDTO createFlat(
            FlatSaveRequestDTO dto,
            List<MultipartFile> pictures,
            Long landlordId
    ) throws IOException {

        Long finalLandlordId = landlordId != null ? landlordId : dto.getLandlordId();

        validateCreateRequest(dto, finalLandlordId);

        EntityUser landlord = getLandlord(finalLandlordId);
        EntityBuildings building = getBuilding(dto.getBuildingId());

        validateFlatNumberAvailable(
                building.getId(),
                dto.getFlatNumber(),
                null
        );

        EntityUnitStatus status = resolveUnitStatus(dto.getStatusCode());

        EntityFlats flat = EntityFlats.builder()
                .flatNumber(cleanRequired(dto.getFlatNumber(), "Flat number is required"))
                .numberOfRooms(cleanRequired(dto.getNumberOfRooms(), "Number of rooms is required"))
                .numberOfBathrooms(cleanRequired(dto.getNumberOfBathrooms(), "Number of bathrooms is required"))
                .hasLivingRoom(Boolean.TRUE.equals(dto.getHasLivingRoom()))
                .status(status)
                .floorNumber(cleanRequired(dto.getFloorNumber(), "Floor number is required"))
                .description(clean(dto.getDescription()))
                .tenantType(clean(dto.getTenantType()))
                .rentalType(clean(dto.getRentTimeType()))
                .price(dto.getPrice())
                .landlordMobileNumber(clean(dto.getMobileNumber()))
                .landlord(landlord)
                .building(building)
                .active(true)
                .deleted(false)
                .build();

        EntityFlats savedFlat = flatsRepository.save(flat);

        if (pictures != null && !pictures.isEmpty()) {
            propertyMediaService.saveImages(
                    pictures,
                    PropertyType.FLAT,
                    savedFlat.getId()
            );
        }

        EntityFlats loadedFlat = flatsRepository.findDetailsById(savedFlat.getId());

        return toDetailsDTO(loadedFlat);
    }

    @Override
    @Transactional
    public FlatDetailsDTO updateFlat(
            Long flatId,
            FlatUpdateRequestDTO dto,
            List<MultipartFile> newImages,
            List<Long> imageIdsToDelete
    ) throws IOException {

        if (flatId == null || flatId <= 0) {
            throw new IllegalArgumentException("Flat id is required.");
        }

        if (dto == null) {
            throw new IllegalArgumentException("Flat request body is required.");
        }

        EntityFlats flat = flatsRepository.findDetailsById(flatId);

        if (flat == null) {
            throw new EntityNotFoundException("Flat not found with id: " + flatId);
        }

        Long finalBuildingId =
                dto.getBuildingId() != null
                        ? dto.getBuildingId()
                        : flat.getBuilding().getId();

        EntityBuildings building = getBuilding(finalBuildingId);

        validateFlatNumberAvailable(
                building.getId(),
                dto.getFlatNumber(),
                flat.getId()
        );

        flat.setBuilding(building);
        flat.setFlatNumber(cleanRequired(dto.getFlatNumber(), "Flat number is required"));
        flat.setNumberOfRooms(cleanRequired(dto.getNumberOfRooms(), "Number of rooms is required"));
        flat.setNumberOfBathrooms(cleanRequired(dto.getNumberOfBathrooms(), "Number of bathrooms is required"));
        flat.setHasLivingRoom(Boolean.TRUE.equals(dto.getHasLivingRoom()));
        flat.setStatus(resolveUnitStatus(dto.getStatusCode()));
        flat.setFloorNumber(cleanRequired(dto.getFloorNumber(), "Floor number is required"));
        flat.setDescription(clean(dto.getDescription()));
        flat.setTenantType(clean(dto.getTenantType()));
        flat.setRentalType(clean(dto.getRentTimeType()));
        flat.setPrice(dto.getPrice());
        flat.setLandlordMobileNumber(clean(dto.getMobileNumber()));

        EntityFlats savedFlat = flatsRepository.save(flat);

        propertyMediaService.updateImages(
                PropertyType.FLAT,
                savedFlat.getId(),
                imageIdsToDelete,
                newImages
        );

        EntityFlats loadedFlat = flatsRepository.findDetailsById(savedFlat.getId());

        return toDetailsDTO(loadedFlat);
    }

    @Override
    public FlatDetailsDTO getFlatById(Long flatId) {
        if (flatId == null || flatId <= 0) {
            throw new IllegalArgumentException("Flat id is required.");
        }

        EntityFlats flat = flatsRepository.findDetailsById(flatId);

        if (flat == null) {
            throw new EntityNotFoundException("Flat not found with id: " + flatId);
        }

        return toDetailsDTO(flat);
    }

    @Override
    public List<FlatDetailsDTO> getFlatsByBuildingId(Long buildingId) {
        if (buildingId == null || buildingId <= 0) {
            throw new IllegalArgumentException("Building id is required.");
        }

        return flatsRepository.findDetailsByBuildingId(buildingId)
                .stream()
                .map(this::toDetailsDTO)
                .toList();
    }

    @Override
    public List<FlatDetailsDTO> getFlatsByLandlordId(Long landlordId) {
        if (landlordId == null || landlordId <= 0) {
            throw new IllegalArgumentException("Landlord id is required.");
        }

        return flatsRepository.findDetailsByLandlordId(landlordId)
                .stream()
                .map(this::toDetailsDTO)
                .toList();
    }

    @Override
    public List<FlatListDTO> getFlatListByBuildingId(Long buildingId) {
        if (buildingId == null || buildingId <= 0) {
            throw new IllegalArgumentException("Building id is required.");
        }

        return flatsRepository.findDetailsByBuildingId(buildingId)
                .stream()
                .map(this::toListDTO)
                .toList();
    }

    @Override
    public List<FlatDetailsDTO> searchFlats(FlatSearchRequestDTO filter) {
        if (filter == null) {
            filter = new FlatSearchRequestDTO();
        }

        return flatsRepository.searchFlats(filter)
                .stream()
                .map(this::toDetailsDTO)
                .toList();
    }

    @Override
    @Transactional
    public void deleteFlat(Long flatId) {
        if (flatId == null || flatId <= 0) {
            throw new IllegalArgumentException("Flat id is required.");
        }

        EntityFlats flat = flatsRepository.findDetailsById(flatId);

        if (flat == null) {
            throw new EntityNotFoundException("Flat not found with id: " + flatId);
        }

        long roomsCount =
                flat.getRooms() == null
                        ? 0
                        : flat.getRooms().size();

        if (roomsCount > 0) {
            throw new IllegalStateException(
                    "Cannot delete flat because it has rooms. Delete or move rooms first."
            );
        }

        propertyMediaService.softDeleteAllImages(
                PropertyType.FLAT,
                flat.getId()
        );

        flat.setDeleted(true);
        flat.setActive(false);
        flat.setDeletedAt(LocalDateTime.now());

        flatsRepository.save(flat);
    }

    private EntityUser getLandlord(Long landlordId) {
        return userRepository.findById(landlordId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Landlord not found with id: " + landlordId
                ));
    }

    private EntityBuildings getBuilding(Long buildingId) {
        return buildingRepository.findById(buildingId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Building not found with id: " + buildingId
                ));
    }

    private EntityUnitStatus resolveUnitStatus(String statusCode) {
        if (statusCode == null || statusCode.isBlank()) {
            return unitStatusService.getDefaultUnitStatus();
        }

        return unitStatusService.getRequiredStatus(
                StatusDomain.UNIT,
                statusCode
        );
    }

    private void validateCreateRequest(FlatSaveRequestDTO dto, Long landlordId) {
        if (dto == null) {
            throw new IllegalArgumentException("Flat request body is required.");
        }

        if (landlordId == null || landlordId <= 0) {
            throw new IllegalArgumentException("Landlord id is required.");
        }

        if (dto.getBuildingId() == null || dto.getBuildingId() <= 0) {
            throw new IllegalArgumentException("Building id is required.");
        }

        if (isBlank(dto.getFlatNumber())) {
            throw new IllegalArgumentException("Flat number is required.");
        }

        if (isBlank(dto.getNumberOfRooms())) {
            throw new IllegalArgumentException("Number of rooms is required.");
        }

        if (isBlank(dto.getNumberOfBathrooms())) {
            throw new IllegalArgumentException("Number of bathrooms is required.");
        }

        if (isBlank(dto.getFloorNumber())) {
            throw new IllegalArgumentException("Floor number is required.");
        }

        if (dto.getPrice() == null) {
            throw new IllegalArgumentException("Price is required.");
        }

        if (dto.getPrice().signum() < 0) {
            throw new IllegalArgumentException("Price cannot be negative.");
        }
    }

    private void validateFlatNumberAvailable(
            Long buildingId,
            String flatNumber,
            Long currentFlatId
    ) {
        String cleanedFlatNumber = cleanRequired(flatNumber, "Flat number is required");

        boolean exists = flatsRepository
                .existsByBuildingIdAndFlatNumberIgnoreCaseAndDeletedFalse(
                        buildingId,
                        cleanedFlatNumber
                );

        if (!exists) {
            return;
        }

        if (currentFlatId == null) {
            throw new IllegalStateException(
                    "Flat number already exists in this building: " + cleanedFlatNumber
            );
        }

        EntityFlats current = flatsRepository.findDetailsById(currentFlatId);

        if (
                current != null
                        && current.getBuilding() != null
                        && current.getBuilding().getId().equals(buildingId)
                        && current.getFlatNumber().equalsIgnoreCase(cleanedFlatNumber)
        ) {
            return;
        }

        throw new IllegalStateException(
                "Flat number already exists in this building: " + cleanedFlatNumber
        );
    }

    private FlatDetailsDTO toDetailsDTO(EntityFlats flat) {
        EntityBuildings building = flat.getBuilding();
        EntityUser landlord = flat.getLandlord();

        List<String> imageUrls = propertyMediaService
                .getImages(PropertyType.FLAT, flat.getId())
                .stream()
                .map(PropertyMediaDTO::getFileUrl)
                .toList();

        return FlatDetailsDTO.builder()
                .id(flat.getId())
                .flatNumber(flat.getFlatNumber())
                .numberOfRooms(flat.getNumberOfRooms())
                .numberOfBathrooms(flat.getNumberOfBathrooms())
                .hasLivingRoom(flat.isHasLivingRoom())
                .status(toStatusDTO(flat.getStatus()))
                .floorNumber(flat.getFloorNumber())
                .description(flat.getDescription())

                .regionId(
                        building != null && building.getRegion() != null
                                ? building.getRegion().getId()
                                : null
                )
                .region(
                        building != null && building.getRegion() != null
                                ? building.getRegion().getNameEn()
                                : null
                )
                .regionAr(
                        building != null && building.getRegion() != null
                                ? building.getRegion().getNameAr()
                                : null
                )

                .wilayaId(
                        building != null && building.getWilaya() != null
                                ? building.getWilaya().getId()
                                : null
                )
                .wilaya(
                        building != null && building.getWilaya() != null
                                ? building.getWilaya().getNameEn()
                                : null
                )
                .wilayaAr(
                        building != null && building.getWilaya() != null
                                ? building.getWilaya().getNameAr()
                                : null
                )

                .areaId(
                        building != null && building.getArea() != null
                                ? building.getArea().getId()
                                : null
                )
                .area(
                        building != null && building.getArea() != null
                                ? building.getArea().getNameEn()
                                : null
                )
                .areaAr(
                        building != null && building.getArea() != null
                                ? building.getArea().getNameAr()
                                : null
                )

                .tenantType(flat.getTenantType())
                .rentTimeType(flat.getRentalType())
                .price(flat.getPrice())

                .latitude(building != null ? building.getLatitude() : null)
                .longitude(building != null ? building.getLongitude() : null)

                .landlordId(landlord != null ? landlord.getId() : null)
                .landlordName(getLandlordName(landlord))
                .landlordPhoneNumber(
                        landlord != null ? landlord.getPhoneNumber() : null
                )

                .buildingId(building != null ? building.getId() : null)
                .buildingName(building != null ? building.getBuildingName() : null)
                .buildingNumber(building != null ? building.getBuildingNumber() : null)

                .hasParking(building != null ? building.isHasParking() : null)
                .hasGym(building != null ? building.isHasGym() : null)
                .hasSwimmingPool(building != null ? building.isHasSwimmingPool() : null)

                .images(imageUrls != null ? imageUrls : Collections.emptyList())
                .build();
    }

    private FlatListDTO toListDTO(EntityFlats flat) {
        EntityUnitStatus status = flat.getStatus();

        return FlatListDTO.builder()
                .flatId(flat.getId())
                .flatNumber(flat.getFlatNumber())
                .floorNumber(flat.getFloorNumber())
                .build();
    }

    private FlatStatusDTO toStatusDTO(EntityUnitStatus status) {
        if (status == null) {
            return null;
        }

        return FlatStatusDTO.builder()
                .id(status.getId())
                .statusDomain(status.getStatusDomain())
                .code(status.getCode())
                .nameEn(status.getNameEn())
                .nameAr(status.getNameAr())
                .colorCode(status.getColorCode())
                .icon(status.getIcon())
                .build();
    }

    private String getLandlordName(EntityUser landlord) {
        if (landlord == null) {
            return null;
        }

        String firstName = landlord.getFirstName() == null ? "" : landlord.getFirstName().trim();
        String lastName = landlord.getLastName() == null ? "" : landlord.getLastName().trim();

        String fullName = (firstName + " " + lastName).trim();

        return fullName.isEmpty() ? null : fullName;
    }

    private String cleanRequired(String value, String message) {
        String cleaned = clean(value);

        if (cleaned == null) {
            throw new IllegalArgumentException(message);
        }

        return cleaned;
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }

        String cleaned = value.trim();

        return cleaned.isEmpty() ? null : cleaned;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}