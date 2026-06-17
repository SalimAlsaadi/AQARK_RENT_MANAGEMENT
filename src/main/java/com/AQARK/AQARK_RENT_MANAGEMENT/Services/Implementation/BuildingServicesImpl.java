package com.AQARK.AQARK_RENT_MANAGEMENT.Services.Implementation;

import com.AQARK.AQARK_RENT_MANAGEMENT.Common_Utilities.PropertyMediaService;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.BuildingDTO.*;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.MediaDTO.PropertyMediaDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.*;
import com.AQARK.AQARK_RENT_MANAGEMENT.Repositories.*;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface.BuildingServicesInterface;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface.UnitStatusServiceInterface;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

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

        Long finalLandlordId = landlordId != null ? landlordId : dto.getLandlordId();

        if (finalLandlordId == null) {
            throw new IllegalArgumentException("Landlord id is required.");
        }

        validateBuildingSave(dto, finalLandlordId);

        EntityBuildings building = toEntity(dto, finalLandlordId);

        EntityBuildings savedBuilding = buildingRepository.save(building);

        if (pictures != null && !pictures.isEmpty()) {
            propertyMediaService.saveImages(
                    pictures,
                    MediaOwnerType.BUILDING,
                    savedBuilding.getId()
            );
        }

        EntityBuildings loaded = buildingRepository.findDetailsById(savedBuilding.getId());

        return toDetailsDTO(loaded);
    }

    @Override
    public BuildingDetailsDTO getBuildingById(Long id) {
        EntityBuildings building = buildingRepository.findDetailsById(id);

        if (building == null) {
            throw new EntityNotFoundException("Building not found with id: " + id);
        }

        return toDetailsDTO(building);
    }

    @Override
    public List<BuildingListDTO> getBuildingsByLandlordId(Long landlordId) {

        if (landlordId == null || landlordId <= 0) {
            throw new IllegalArgumentException("Invalid landlord id.");
        }

        return buildingRepository.findDetailsByLandlordId(landlordId)
                .stream()
                .map(this::toListDTO)
                .toList();
    }

    @Override
    public List<BuildingListDTO> getAllBuildings() {
        return buildingRepository.findAllByDeletedFalseOrderByIdDesc()
                .stream()
                .map(this::toListDTO)
                .toList();
    }

    @Override
    @Transactional
    public BuildingDetailsDTO updateBuilding(BuildingUpdateRequestDTO dto, List<Long> picturesToDelete, List<MultipartFile> newPictures) throws IOException {

        EntityBuildings existing = buildingRepository.findDetailsById(dto.getId());

        if (existing == null) {
            throw new EntityNotFoundException("Building not found with id: " + dto.getId());
        }

        validateBuildingUpdate(dto, existing.getLandlord().getId());

        OmanRegionEntity region = getRegion(dto.getRegionId());
        OmanWilayaEntity wilaya = getWilaya(dto.getWilayaId());
        OmanAreaEntity area = getArea(dto.getAreaId());

        EntityUnitStatus status = unitStatusService.getRequiredStatus( StatusDomain.BUILDING, normalizeStatus(dto.getStatusCode()));

        existing.setBuildingName(cleanRequired(dto.getBuildingName(), "Building name is required"));
        existing.setBuildingNumber(cleanRequired(dto.getBuildingNumber(), "Building number is required"));
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
                MediaOwnerType.BUILDING,
                existing.getId(),
                picturesToDelete,
                newPictures
        );

        EntityBuildings updated = buildingRepository.save(existing);

        return toDetailsDTO(updated);
    }

    @Override
    @Transactional
    public void softDeleteBuilding(Long buildingId) {
        EntityBuildings building = buildingRepository.findDetailsById(buildingId);

        if (building == null) {
            throw new EntityNotFoundException("Building not found with id: " + buildingId);
        }

        BuildingUnitCountsDTO counts = getUnitCounts(building.getId());

        long totalUnits =
                counts.getTotalFlats()
                        + counts.getTotalRooms()
                        + counts.getTotalShops();

        if (totalUnits > 0) {
            throw new IllegalStateException(
                    "Cannot delete building because it has related units. Deactivate it instead."
            );
        }

        building.setDeleted(true);
        building.setActive(false);
        building.setDeletedAt(LocalDateTime.now());

        buildingRepository.save(building);
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

    private void validateBuildingSave(BuildingSaveRequestDTO dto, Long landlordId) {
        if (dto == null) {
            throw new IllegalArgumentException("Building request is required.");
        }

        validateLocation(dto.getRegionId(), dto.getWilayaId(), dto.getAreaId());

        if (buildingRepository.existsByLandlordIdAndBuildingNumberIgnoreCaseAndDeletedFalse(
                landlordId,
                cleanRequired(dto.getBuildingNumber(), "Building number is required")
        )) {
            throw new IllegalStateException("Building number already exists for this landlord.");
        }

        validateCoordinates(dto.getLatitude(), dto.getLongitude());
    }

    private void validateBuildingUpdate(BuildingUpdateRequestDTO dto, Long landlordId) {
        if (dto == null) {
            throw new IllegalArgumentException("Building request is required.");
        }

        validateLocation(dto.getRegionId(), dto.getWilayaId(), dto.getAreaId());

        validateCoordinates(dto.getLatitude(), dto.getLongitude());
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

    private void validateCoordinates(Double latitude, Double longitude) {
        if ((latitude == null && longitude != null) || (latitude != null && longitude == null)) {
            throw new IllegalArgumentException("Latitude and longitude must be provided together.");
        }

        if (latitude != null && (latitude < -90 || latitude > 90)) {
            throw new IllegalArgumentException("Invalid latitude.");
        }

        if (longitude != null && (longitude < -180 || longitude > 180)) {
            throw new IllegalArgumentException("Invalid longitude.");
        }
    }

    private OmanRegionEntity getRegion(Long id) {
        return omanRegionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Region not found with id: " + id));
    }

    private OmanWilayaEntity getWilaya(Long id) {
        return omanWilayaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Wilaya not found with id: " + id));
    }

    private OmanAreaEntity getArea(Long id) {
        return omanAreaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Area not found with id: " + id));
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
                .landlordMobileNumber(
                        building.getLandlord() != null ? building.getLandlord().getPhoneNumber() : null
                )
                .pictureUrls(getPictureUrls(building.getId()))
                .build();
    }

    private BuildingListDTO toListDTO(EntityBuildings building) {
        BuildingUnitCountsDTO counts = getUnitCounts(building.getId());

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

        return BuildingListDTO.builder()
                .id(building.getId())
                .buildingName(building.getBuildingName())
                .buildingNumber(building.getBuildingNumber())
                .region(building.getRegion() != null ? building.getRegion().getNameEn() : null)
                .wilaya(building.getWilaya() != null ? building.getWilaya().getNameEn() : null)
                .area(building.getArea() != null ? building.getArea().getNameEn() : null)
                .statusCode(building.getStatus().getCode())
                .statusNameEn(building.getStatus().getNameEn())
                .statusNameAr(building.getStatus().getNameAr())
                .statusColorCode(building.getStatus().getColorCode())
                .statusIcon(building.getStatus().getIcon())
                .numberOfFloors(building.getNumberOfFloors())
                .totalFlats(counts.getTotalFlats())
                .totalRooms(counts.getTotalRooms())
                .totalShops(counts.getTotalShops())
                .availableUnits(availableUnits)
                .rentedUnits(rentedUnits)
                .maintenanceUnits(maintenanceUnits)
                .mainImageUrl(images.isEmpty() ? null : images.get(0))
                .build();
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
        BuildingUnitCountsProjection projection =
                buildingRepository.getBuildingUnitCounts(buildingId);

        if (projection == null) {
            return emptyCounts();
        }

        return BuildingUnitCountsDTO.builder()
                .totalFlats(nullToZero(projection.getTotalFlats()))
                .flatsAvailable(nullToZero(projection.getFlatsAvailable()))
                .flatsRented(nullToZero(projection.getFlatsRented()))
                .flatsReserved(nullToZero(projection.getFlatsReserved()))
                .flatsUnderMaintenance(nullToZero(projection.getFlatsUnderMaintenance()))
                .flatsBlocked(nullToZero(projection.getFlatsBlocked()))
                .totalRooms(nullToZero(projection.getTotalRooms()))
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
                .flatsReserved(0)
                .flatsUnderMaintenance(0)
                .flatsBlocked(0)
                .totalRooms(0)
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
                .getImages(MediaOwnerType.BUILDING, buildingId)
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

    private String cleanRequired(String value, String errorMessage) {
        String cleaned = clean(value);

        if (cleaned == null) {
            throw new IllegalArgumentException(errorMessage);
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