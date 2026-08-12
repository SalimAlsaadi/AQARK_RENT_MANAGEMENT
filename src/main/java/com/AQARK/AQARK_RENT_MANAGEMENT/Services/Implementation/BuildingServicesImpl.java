package com.AQARK.AQARK_RENT_MANAGEMENT.Services.Implementation;

import com.AQARK.AQARK_RENT_MANAGEMENT.Common_Utilities.PropertyMediaService;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.BuildingDTO.*;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.MediaDTO.PropertyMediaDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.*;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Enums.PropertyType;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Enums.StatusDomain;
import com.AQARK.AQARK_RENT_MANAGEMENT.Exceptions.BusinessConflictException;
import com.AQARK.AQARK_RENT_MANAGEMENT.Exceptions.FieldValidationException;
import com.AQARK.AQARK_RENT_MANAGEMENT.Repositories.*;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface.BuildingServicesInterface;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface.UnitStatusServiceInterface;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.apache.logging.log4j.util.Strings.isBlank;

@Service
@RequiredArgsConstructor
public class BuildingServicesImpl implements BuildingServicesInterface {

    private static final String DEFAULT_BUILDING_STATUS = "ACTIVE";

    private final BuildingRepository buildingRepository;
    private final UserRepository userRepository;

    private final OmanRegionRepository omanRegionRepository;
    private final OmanWilayaRepository omanWilayaRepository;
    private final OmanAreaRepository omanAreaRepository;

    private final UnitStatusServiceInterface unitStatusService;
    private final PropertyMediaService propertyMediaService;

    @Override
    @Transactional
    public BuildingDetailsDTO createBuilding(BuildingSaveRequestDTO dto, List<MultipartFile> pictures, Long landlordId) throws IOException {

        Long finalLandlordId = landlordId != null ? landlordId
                : dto.getLandlordId() != null ? dto.getLandlordId()
                : null;

        validateBuildingSave(dto, finalLandlordId, pictures);

        EntityBuildings building = toEntity(dto, finalLandlordId);

        EntityBuildings savedBuilding = buildingRepository.save(building);

        if (pictures != null && !pictures.isEmpty()) {
            propertyMediaService.saveImages(
                    pictures,
                    PropertyType.BUILDING,
                    savedBuilding.getId()
            );
        }

        EntityBuildings loaded = buildingRepository.findDetailsById(savedBuilding.getId());

        return toDetailsDTO(loaded);
    }

    @Override
    @Transactional(readOnly = true)
    public BuildingDetailsDTO getBuildingById(Long id) {
        validateId(id, "id", "Building id is required.");

        EntityBuildings building = buildingRepository.findDetailsById(id);

        if (building == null) {
            throw new EntityNotFoundException("Building not found with id: " + id);
        }

        return toDetailsDTO(building);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BuildingListDTO> getBuildingsByLandlordId(Long landlordId) {
        validateId(
                landlordId,
                "landlordId",
                "Landlord id is required."
        );

        List<EntityBuildings> buildings =
                buildingRepository.findDetailsByLandlordId(landlordId);

        if (buildings.isEmpty()) {
            return List.of();
        }

        Map<Long, BuildingUnitCountsProjection> counts =
                toCountsMap(
                        buildingRepository
                                .getBuildingUnitCountsByLandlordId(landlordId)
                );

        return buildings.stream()
                .map(building -> toListDTO(
                        building,
                        counts.get(building.getId())
                ))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BuildingListDTO> getAllBuildings() {
        List<EntityBuildings> buildings =
                buildingRepository.findAllByDeletedFalseOrderByIdDesc();

        if (buildings.isEmpty()) {
            return List.of();
        }

        Map<Long, BuildingUnitCountsProjection> counts =
                toCountsMap(
                        buildingRepository.getAllBuildingUnitCounts()
                );

        return buildings.stream()
                .map(building -> toListDTO(
                        building,
                        counts.get(building.getId())
                ))
                .toList();
    }

    @Override
    @Transactional
    public BuildingDetailsDTO updateBuilding(
            BuildingUpdateRequestDTO dto,
            List<Long> picturesToDelete,
            List<MultipartFile> newPictures
    ) throws IOException {

        if (dto == null) {
            throw new FieldValidationException("Building request is required.", Map.of("data", "Building request is required."));
        }

        validateId(dto.getId(), "id", "Building id is required.");

        EntityBuildings existing = buildingRepository.findDetailsById(dto.getId());

        if (existing == null) {
            throw new EntityNotFoundException("Building not found with id: " + dto.getId());
        }

        Long landlordId = existing.getLandlord() != null ? existing.getLandlord().getId() : null;

        validateBuildingUpdate(dto, landlordId);

        OmanRegionEntity region = getRegion(dto.getRegionId());
        OmanWilayaEntity wilaya = getWilaya(dto.getWilayaId());
        OmanAreaEntity area = getArea(dto.getAreaId());

        EntityUnitStatus status = unitStatusService.getRequiredStatus(StatusDomain.BUILDING, normalizeStatus(dto.getStatusCode()));

        existing.setBuildingName( cleanRequired(dto.getBuildingName(), "Building name is required."));

        existing.setBuildingNumber( cleanRequired(dto.getBuildingNumber(), "Building number is required."));

        existing.setRegion(region);
        existing.setWilaya(wilaya);
        existing.setArea(area);
        existing.setNumberOfFloors(dto.getNumberOfFloors());
        existing.setStatus(status);
        existing.setHasParking(Boolean.TRUE.equals(dto.getHasParking()));
        existing.setHasGym(Boolean.TRUE.equals(dto.getHasGym()));
        existing.setHasSwimmingPool(Boolean.TRUE.equals(dto.getHasSwimmingPool()));
        existing.setDescription(clean(dto.getDescription()));
        existing.setLatitude(dto.getLatitude());
        existing.setLongitude(dto.getLongitude());

        propertyMediaService.updateImages(
                PropertyType.BUILDING,
                existing.getId(),
                picturesToDelete,
                newPictures
        );

        EntityBuildings updated = buildingRepository.save(existing);

        EntityBuildings loaded = buildingRepository.findDetailsById(updated.getId());

        return toDetailsDTO(loaded);
    }


    @Override
    @Transactional
    public void softDeleteBuilding(Long buildingId) {
        validateId(buildingId, "buildingId", "Building id is required.");

        EntityBuildings building = buildingRepository.findDetailsById(buildingId);

        if (building == null) {
            throw new EntityNotFoundException("Building not found with id: " + buildingId);
        }

        BuildingUnitCountsDTO counts = getUnitCounts(building.getId());

        long totalUnits = counts.getTotalFlats() + counts.getTotalRooms() + counts.getTotalShops();

        if (totalUnits > 0) {
            throw new BusinessConflictException("Cannot delete building because it has related units. Deactivate it instead."
            );
        }

        propertyMediaService.softDeleteAllImages(
                PropertyType.BUILDING,
                building.getId()
        );

        building.setDeleted(true);
        building.setActive(false);
        building.setDeletedAt(LocalDateTime.now());

        buildingRepository.save(building);
    }

    private void validateId(Long id, String fieldName, String message) {
        if (id == null || id <= 0) {
            throw new FieldValidationException(message, Map.of(fieldName, message));
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }


    private EntityBuildings toEntity(BuildingSaveRequestDTO dto, Long landlordId) {
        EntityUser landlord = userRepository.findById(landlordId)
                .orElseThrow(() -> new EntityNotFoundException("Landlord not found with id: " + landlordId));

        OmanRegionEntity region = getRegion(dto.getRegionId());
        OmanWilayaEntity wilaya = getWilaya(dto.getWilayaId());
        OmanAreaEntity area = getArea(dto.getAreaId());

        EntityUnitStatus status = unitStatusService.getRequiredStatus(
                StatusDomain.BUILDING,
                normalizeStatus(dto.getStatusCode())
        );

        return EntityBuildings.builder()
                .buildingName(cleanRequired(dto.getBuildingName(), "Building name is required"))
                .buildingNumber(cleanRequired(dto.getBuildingNumber(), "Building number is required"))
                .region(region)
                .wilaya(wilaya)
                .area(area)
                .numberOfFloors(dto.getNumberOfFloors())
                .status(status)
                .hasParking(Boolean.TRUE.equals(dto.getHasParking()))
                .hasGym(Boolean.TRUE.equals(dto.getHasGym()))
                .hasSwimmingPool(Boolean.TRUE.equals(dto.getHasSwimmingPool()))
                .description(clean(dto.getDescription()))
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .landlord(landlord)
                .active(true)
                .deleted(false)
                .build();
    }

    private void validateBuildingSave(BuildingSaveRequestDTO dto, Long landlordId, List<MultipartFile> pictures) {

        Map<String, String> errors = new LinkedHashMap<>();

        if (dto == null) {
            throw new FieldValidationException("Building request is required.", Map.of("data", "Building request is required."));
        }

        if (landlordId == null || landlordId <= 0) {
            errors.put("landlordId", "Authenticated landlord id is not valid or not found.");
        }

        if (isBlank(dto.getBuildingName())) {
            errors.put("buildingName", "Building name is required.");

        } else if (dto.getBuildingName().trim().length() > 200) {
            errors.put("buildingName", "Building name must not exceed 200 characters.");
        }

        if (isBlank(dto.getBuildingNumber())) {
            errors.put("buildingNumber", "Building number is required.");

        } else if (dto.getBuildingNumber().trim().length() > 100) {
            errors.put("buildingNumber", "Building number must not exceed 100 characters.");
        }

        if (dto.getRegionId() == null || dto.getRegionId() <= 0) {
            errors.put("regionId", "Region is required.");
        }

        if (dto.getWilayaId() == null || dto.getWilayaId() <= 0) {
            errors.put("wilayaId", "Wilaya is required.");
        }

        if (dto.getAreaId() == null || dto.getAreaId() <= 0) {
            errors.put("areaId", "Area is required.");
        }

        if (isBlank(dto.getNumberOfFloors())) {
            errors.put("numberOfFloors", "Number of floors is required.");
        }

        if (isBlank(dto.getStatusCode())) {
            errors.put("statusCode", "Building status is required.");
        }

        if (dto.getDescription() != null
                && dto.getDescription().trim().length() > 1000) {
            errors.put(
                    "description",
                    "Description must not exceed 1000 characters."
            );
        }

        if(pictures==null || pictures.isEmpty()){
            errors.put("pictures", "pictures required");
        }

        validateCoordinates(
                dto.getLatitude(),
                dto.getLongitude(),
                errors
        );

        if (!errors.isEmpty()) {
            throw new FieldValidationException("Please complete all required building fields correctly.", errors);
        }

        String buildingNumber = cleanRequired(
                dto.getBuildingNumber(),
                "Building number is required."
        );

        if (buildingRepository.existsByLandlordIdAndBuildingNumberIgnoreCaseAndDeletedFalse(
                landlordId,
                buildingNumber
        )) {
            throw new FieldValidationException(
                    "Building number already exists.", Map.of("buildingNumber", "Building number already exists for this landlord.")
            );
        }
    }

    private void validateBuildingUpdate(BuildingUpdateRequestDTO dto, Long landlordId) {

        Map<String, String> errors = new LinkedHashMap<>();

        if (dto == null) {
            throw new FieldValidationException(
                    "Building request is required.", Map.of("data", "Building request is required."));
        }

        if (dto.getId() == null || dto.getId() <= 0) {
            errors.put("id", "Building id is required.");
        }

        if (landlordId == null || landlordId <= 0) {
            errors.put("landlordId", "Landlord id is not valid.");
        }

        if (isBlank(dto.getBuildingName())) {
            errors.put("buildingName", "Building name is required.");
        }

        if (isBlank(dto.getBuildingNumber())) {
            errors.put("buildingNumber", "Building number is required.");
        }

        if (dto.getRegionId() == null || dto.getRegionId() <= 0) {
            errors.put("regionId", "Region is required.");
        }

        if (dto.getWilayaId() == null || dto.getWilayaId() <= 0) {
            errors.put("wilayaId", "Wilaya is required.");
        }

        if (dto.getAreaId() == null || dto.getAreaId() <= 0) {
            errors.put("areaId", "Area is required.");
        }

        if (isBlank(dto.getNumberOfFloors())) {
            errors.put("numberOfFloors", "Number of floors is required.");
        }

        if (isBlank(dto.getStatusCode())) {
            errors.put("statusCode", "Building status is required.");
        }

        validateCoordinates(
                dto.getLatitude(),
                dto.getLongitude(),
                errors
        );

        if (!errors.isEmpty()) {
            throw new FieldValidationException("Please complete all required building fields correctly.", errors);
        }
    }

    private void validateLocation(Long regionId, Long wilayaId, Long areaId) {
        if (regionId == null) {
            throw new IllegalArgumentException("Region is required.");
        }

        if (wilayaId == null) {
            throw new IllegalArgumentException("Wilaya is required.");
        }

        if (areaId == null) {
            throw new IllegalArgumentException("Area is required.");
        }
    }

    private void validateCoordinates(Double latitude, Double longitude, Map<String, String> errors) {
        if (latitude == null) {
            errors.put("latitude", "Latitude is required.");

        } else if (latitude < -90 || latitude > 90) {
            errors.put("latitude", "Latitude must be between -90 and 90.");
        }

        if (longitude == null) {
            errors.put("longitude", "Longitude is required.");

        } else if (longitude < -180 || longitude > 180) {
            errors.put("longitude", "Longitude must be between -180 and 180.");
        }
    }

    private OmanRegionEntity getRegion(Long id) {
        if (id == null || id <= 0) {
            throw new FieldValidationException(
                    "Region is required.",
                    Map.of("regionId", "Region is required.")
            );
        }

        return omanRegionRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Region not found with id: " + id));
    }

    private OmanWilayaEntity getWilaya(Long id) {
        if (id == null || id <= 0) {
            throw new FieldValidationException("Wilaya is required.", Map.of("wilayaId", "Wilaya is required."));
        }

        return omanWilayaRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Wilaya not found with id: " + id));
    }

    private OmanAreaEntity getArea(Long id) {
        if (id == null || id <= 0) {
            throw new FieldValidationException("Area is required.", Map.of("areaId", "Area is required."));
        }

        return omanAreaRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Area not found with id: " + id));
    }

    private String normalizeStatus(String statusCode) {
        if (statusCode == null || statusCode.trim().isEmpty()) {
            return DEFAULT_BUILDING_STATUS;
        }

        return statusCode.trim().toUpperCase();
    }

    private BuildingDetailsDTO toDetailsDTO(EntityBuildings building) {
        return BuildingDetailsDTO.builder()
                .id(building.getId())
                .buildingName(building.getBuildingName())
                .buildingNumber(building.getBuildingNumber())
                .numberOfFloors(building.getNumberOfFloors())
                .status(toStatusDTO(building.getStatus()))
                .location(toLocationDTO(building))
                .features(toFeaturesDTO(building))
                .unitCounts(getUnitCounts(building.getId()))
                .description(building.getDescription())
                .landlordId(building.getLandlord() != null ? building.getLandlord().getId() : null)
                .landlordName(getLandlordName(building.getLandlord()))
                .landlordMobileNumber(building.getLandlord() != null ? building.getLandlord().getPhoneNumber() : null)
                .pictureUrls(getPictureUrls(building.getId()))
                .build();
    }

    private BuildingListDTO toListDTO(
            EntityBuildings building,
            BuildingUnitCountsProjection projection
    ) {
        BuildingUnitCountsDTO counts = toCountsDTO(projection);

        long availableUnits =
                counts.getFlatsAvailable()
                        + counts.getRoomsAvailable()
                        + counts.getShopsAvailable();

        long rentedUnits =
                counts.getFlatsRented()
                        + counts.getRoomsRented()
                        + counts.getShopsRented();

        long maintenanceUnits =
                counts.getFlatsUnderMaintenance()
                        + counts.getRoomsUnderMaintenance()
                        + counts.getShopsUnderMaintenance();

        List<String> images = getPictureUrls(building.getId());

        EntityUnitStatus status = building.getStatus();

        return BuildingListDTO.builder()
                .id(building.getId())
                .buildingName(building.getBuildingName())
                .buildingNumber(building.getBuildingNumber())
                .region(getRegionName(building))
                .wilaya(getWilayaName(building))
                .area(getAreaName(building))
                .statusCode(status != null ? status.getCode() : null)
                .statusNameEn(status != null ? status.getNameEn() : null)
                .statusNameAr(status != null ? status.getNameAr() : null)
                .statusColorCode(
                        status != null ? status.getColorCode() : null
                )
                .statusIcon(status != null ? status.getIcon() : null)
                .numberOfFloors(building.getNumberOfFloors())
                .totalFlats(counts.getTotalFlats())
                .totalRooms(counts.getTotalRooms())
                .totalShops(counts.getTotalShops())
                .availableUnits(availableUnits)
                .rentedUnits(rentedUnits)
                .maintenanceUnits(maintenanceUnits)
                .mainImageUrl(images.isEmpty() ? null : images)
                .build();
    }

    private Map<Long, BuildingUnitCountsProjection> toCountsMap(
            List<BuildingUnitCountsProjection> projections
    ) {
        if (projections == null || projections.isEmpty()) {
            return Map.of();
        }

        return projections.stream()
                .filter(projection -> projection.getBuildingId() != null)
                .collect(Collectors.toMap(
                        BuildingUnitCountsProjection::getBuildingId,
                        Function.identity(),
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ));
    }

    private String getRegionName(EntityBuildings building) {
        return building.getRegion() != null
                ? building.getRegion().getNameEn()
                : null;
    }

    private String getWilayaName(EntityBuildings building) {
        return building.getWilaya() != null
                ? building.getWilaya().getNameEn()
                : null;
    }

    private String getAreaName(EntityBuildings building) {
        return building.getArea() != null
                ? building.getArea().getNameEn()
                : null;
    }

    private BuildingStatusDTO toStatusDTO(EntityUnitStatus status) {
        if (status == null) {
            return null;
        }

        return BuildingStatusDTO.builder()
                .id(status.getId())
                .code(status.getCode())
                .nameEn(status.getNameEn())
                .nameAr(status.getNameAr())
                .colorCode(status.getColorCode())
                .icon(status.getIcon())
                .active(status.isActive())
                .build();
    }

    private BuildingLocationDTO toLocationDTO(EntityBuildings building) {
        return BuildingLocationDTO.builder()
                .regionId(building.getRegion() != null ? building.getRegion().getId() : null)
                .regionNameEn(building.getRegion() != null ? building.getRegion().getNameEn() : null)
                .regionNameAr(building.getRegion() != null ? building.getRegion().getNameAr() : null)
                .wilayaId(building.getWilaya() != null ? building.getWilaya().getId() : null)
                .wilayaNameEn(building.getWilaya() != null ? building.getWilaya().getNameEn() : null)
                .wilayaNameAr(building.getWilaya() != null ? building.getWilaya().getNameAr() : null)
                .areaId(building.getArea() != null ? building.getArea().getId() : null)
                .areaNameEn(building.getArea() != null ? building.getArea().getNameEn() : null)
                .areaNameAr(building.getArea() != null ? building.getArea().getNameAr() : null)
                .latitude(building.getLatitude())
                .longitude(building.getLongitude())
                .build();
    }

    private BuildingFeaturesDTO toFeaturesDTO(EntityBuildings building) {
        return BuildingFeaturesDTO.builder()
                .hasParking(building.isHasParking())
                .hasGym(building.isHasGym())
                .hasSwimmingPool(building.isHasSwimmingPool())
                .build();
    }

    private BuildingUnitCountsDTO getUnitCounts(Long buildingId) {
        validateId(buildingId, "buildingId", "Building id is required.");

        return toCountsDTO(buildingRepository.getBuildingUnitCounts(buildingId));
    }

    private BuildingUnitCountsDTO toCountsDTO( BuildingUnitCountsProjection projection) {

        if (projection == null) {
            return emptyCounts();
        }

        long directRooms = nullToZero(projection.getDirectRooms());

        long roomsInFlat = nullToZero(projection.getRoomsInFlat());

        long totalRooms = nullToZero(projection.getTotalRooms());

        if (totalRooms != directRooms + roomsInFlat) {
            totalRooms = directRooms + roomsInFlat;
        }

        return BuildingUnitCountsDTO.builder()
                .totalFlats(nullToZero(projection.getTotalFlats()))
                .flatsAvailable(nullToZero(projection.getFlatsAvailable()))
                .flatsRented(nullToZero(projection.getFlatsRented()))
                .flatsRentedAsRooms(nullToZero(projection.getFlatsRentedAsRooms()))
                .flatsReserved(nullToZero(projection.getFlatsReserved()))
                .flatsUnderMaintenance(nullToZero(projection.getFlatsUnderMaintenance()))
                .flatsBlocked(nullToZero(projection.getFlatsBlocked()))
                .totalRooms(totalRooms)
                .directRooms(directRooms)
                .roomsInFlat(roomsInFlat)
                .roomsAvailable(nullToZero(projection.getRoomsAvailable()))
                .roomsRented(nullToZero(projection.getRoomsRented()))
                .roomsReserved(nullToZero(projection.getRoomsReserved()))
                .roomsUnderMaintenance(nullToZero(projection.getRoomsUnderMaintenance()))
                .roomsBlocked(nullToZero(projection.getRoomsBlocked()))
                .totalShops(nullToZero(projection.getTotalShops()))
                .shopsAvailable(nullToZero(projection.getShopsAvailable()))
                .shopsRented(nullToZero(projection.getShopsRented()))
                .shopsReserved(nullToZero(projection.getShopsReserved()))
                .shopsUnderMaintenance(nullToZero(projection.getShopsUnderMaintenance()))
                .shopsBlocked(nullToZero(projection.getShopsBlocked()))
                .build();
    }

    private BuildingUnitCountsDTO emptyCounts() {
        return BuildingUnitCountsDTO.builder()
                .totalFlats(0)
                .flatsAvailable(0)
                .flatsRented(0)
                .flatsRentedAsRooms(0)
                .flatsReserved(0)
                .flatsUnderMaintenance(0)
                .flatsBlocked(0)
                .totalRooms(0)
                .directRooms(0L)
                .roomsInFlat(0L)
                .roomsAvailable(0)
                .roomsRented(0)
                .roomsReserved(0)
                .roomsUnderMaintenance(0)
                .roomsBlocked(0)
                .totalShops(0)
                .shopsAvailable(0)
                .shopsRented(0)
                .shopsReserved(0)
                .shopsUnderMaintenance(0)
                .shopsBlocked(0)
                .build();
    }

    private long nullToZero(Long value) {
        return value == null ? 0 : value;
    }

    private List<String> getPictureUrls(Long buildingId) {
        return propertyMediaService
                .getImages(PropertyType.BUILDING, buildingId)
                .stream()
                .map(PropertyMediaDTO::getFileUrl)
                .toList();
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

    private String cleanRequired(
            String value,
            String errorMessage
    ) {
        String cleaned = clean(value);

        if (cleaned == null) {
            throw new FieldValidationException(
                    errorMessage,
                    Map.of("value", errorMessage)
            );
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
}