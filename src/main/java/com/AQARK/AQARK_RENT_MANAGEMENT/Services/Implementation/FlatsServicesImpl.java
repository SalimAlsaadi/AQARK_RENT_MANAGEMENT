package com.AQARK.AQARK_RENT_MANAGEMENT.Services.Implementation;

import com.AQARK.AQARK_RENT_MANAGEMENT.Common_Utilities.PropertyMediaService;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.FlatDTO.*;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.MediaDTO.PropertyMediaDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.RoomDTO.RoomSaveRequestDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.*;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Enums.StatusDomain;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Enums.StatusLevel;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Enums.PropertyType;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.UnitStatusCountsProjection;
import com.AQARK.AQARK_RENT_MANAGEMENT.Exceptions.BusinessConflictException;
import com.AQARK.AQARK_RENT_MANAGEMENT.Exceptions.FieldValidationException;
import com.AQARK.AQARK_RENT_MANAGEMENT.Repositories.*;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface.FlatsServicesInterface;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface.UnitStatusServiceInterface;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class FlatsServicesImpl implements FlatsServicesInterface {

    private final FlatsRepository flatsRepository;
    private final UserRepository userRepository;
    private final BuildingRepository buildingRepository;
    private final RoomRepository roomRepository;
    private final HomeRepository homeRepository;
    private final PropertyMediaService propertyMediaService;
    private final UnitStatusServiceInterface unitStatusService;
    private final HomeServiceImpl homeService;

    private static final String AVAILABLE = "AVAILABLE";
    private static final String RENTED = "RENTED";
    private static final String RESERVED = "RESERVED";
    private static final String UNDER_MAINTENANCE = "UNDER_MAINTENANCE";
    private static final String BLOCKED = "BLOCKED";

    private static final String AVAILABLE_AS_ROOMS = "AVAILABLE_AS_ROOMS";
    private static final String PARTIALLY_RENTED_AS_ROOMS = "PARTIALLY_RENTED_AS_ROOMS";
    private static final String FULLY_RENTED_AS_ROOMS = "FULLY_RENTED_AS_ROOMS";


    @Override
    @Transactional
    public FlatDetailsDTO createFlat(FlatSaveRequestDTO dto, List<MultipartFile> pictures, Long landlordId) throws IOException {

        if (dto == null) {
            throw new FieldValidationException("Flat request is required.",
                    Map.of("data", "Flat request is required."));
        }

        Long finalLandlordId = landlordId != null ? landlordId
                : dto.getLandlordId();

        validateCreateRequest(dto, finalLandlordId);

        EntityUser landlord = userRepository.findById(finalLandlordId).orElseThrow(() -> new EntityNotFoundException("Landlord not found with id: " + finalLandlordId));

        EntityBuildings building = null;
        EntityHomes home = null;

        if (dto.getBuildingId() != null) {

            building = buildingRepository.findById(dto.getBuildingId()).orElseThrow(() -> new EntityNotFoundException("Building not found with id: " + dto.getBuildingId()));

            validateBuildingOwnership(building, finalLandlordId);

            validateBuildingAcceptsFlats(building);

            validateFlatNumberAvailableInBuilding(building.getId(), dto.getFlatNumber(), null);

        } else {

            home = homeRepository.findByIdForUpdate(dto.getHomeId()).orElse(null);

            if (home == null) {
                throw new EntityNotFoundException("Home not found with id: " + dto.getHomeId());
            }

            validateHomeOwnership(home, finalLandlordId);

            validateHomeAcceptsFlats(home);

            validateFlatNumberAvailableInHome(home.getId(), dto.getFlatNumber(), null);
        }

        EntityFlats flat = EntityFlats.builder()
                .flatNumber(cleanRequired(dto.getFlatNumber(), "Flat number is required."))
                .numberOfRooms(dto.getNumberOfRooms())
                .numberOfBathrooms(cleanRequired(dto.getNumberOfBathrooms(), "Number of bathrooms is required."))
                .hasLivingRoom(Boolean.TRUE.equals(dto.getHasLivingRoom()))
                .hasAC(Boolean.TRUE.equals(dto.getHasAC()))
                .hasWIFI(Boolean.TRUE.equals(dto.getHasWIFI()))
                .isFurnished(Boolean.TRUE.equals(dto.getIsFurnished()))
                .status(resolveUnitStatus(dto.getStatusCode()))
                .floorNumber(cleanRequired(dto.getFloorNumber(), "Floor number is required."))
                .description(clean(dto.getDescription()))
                .tenantType(clean(dto.getTenantType()))
                .rentalType(clean(dto.getRentTimeType()))
                .price(dto.getPrice())
                .landlordMobileNumber(clean(dto.getMobileNumber()))
                .landlord(landlord)
                .building(building)
                .home(home)
                .active(true)
                .deleted(false)
                .build();

        EntityFlats savedFlat = flatsRepository.saveAndFlush(flat);

        refreshAffectedHomes(
                null,
                savedFlat.getHome() == null ? null : savedFlat.getHome().getId()
        );

        if (pictures != null && !pictures.isEmpty()) {
            propertyMediaService.saveImages(
                    pictures,
                    PropertyType.FLAT,
                    savedFlat.getId()
            );
        }

        return toDetailsDTO(flatsRepository.findDetailsById(savedFlat.getId()));
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

        Long oldHomeId = flat.getHome() == null ? null : flat.getHome().getId();
        Long landlordId = flat.getLandlord() == null ? null : flat.getLandlord().getId();
        if (landlordId == null) {
            throw new AccessDeniedException("Flat does not have a valid landlord.");
        }

        boolean parentProvided = dto.getBuildingId() != null || dto.getHomeId() != null;
        Long targetBuildingId = parentProvided ? dto.getBuildingId()
                : flat.getBuilding() == null ? null : flat.getBuilding().getId();
        Long targetHomeId = parentProvided ? dto.getHomeId()
                : flat.getHome() == null ? null : flat.getHome().getId();

        if ((targetBuildingId == null) == (targetHomeId == null)) {
            throw new FieldValidationException(
                    "Flat parent is invalid.",
                    Map.of("parent", "Flat must belong to exactly one Building or one Home.")
            );
        }

        validateFlatRoomCountChange(flat, dto.getNumberOfRooms());

        if (targetBuildingId != null) {
            EntityBuildings building = getBuilding(targetBuildingId);
            validateBuildingOwnership(building, landlordId);
            validateBuildingAcceptsFlats(building);
            validateFlatNumberAvailableInBuilding(
                    building.getId(), dto.getFlatNumber(), flat.getId()
            );
            flat.setBuilding(building);
            flat.setHome(null);
        } else {
            EntityHomes home = homeRepository.findByIdForUpdate(targetHomeId).orElse(null);
            if (home == null) {
                throw new EntityNotFoundException("Home not found with id: " + targetHomeId);
            }
            validateHomeOwnership(home, landlordId);
            validateHomeAcceptsFlats(home);
            validateFlatNumberAvailableInHome(
                    home.getId(), dto.getFlatNumber(), flat.getId()
            );
            flat.setHome(home);
            flat.setBuilding(null);
        }

        flat.setFlatNumber(cleanRequired(dto.getFlatNumber(), "Flat number is required"));
        flat.setNumberOfRooms(dto.getNumberOfRooms());
        flat.setNumberOfBathrooms(cleanRequired(dto.getNumberOfBathrooms(), "Number of bathrooms is required"));
        flat.setHasLivingRoom(Boolean.TRUE.equals(dto.getHasLivingRoom()));
        flat.setHasAC(dto.isHasAC());
        flat.setHasWIFI(dto.isHasWIFI());
        flat.setFurnished(dto.isFurnished());
        flat.setStatus(resolveUnitStatus(dto.getStatusCode()));
        flat.setFloorNumber(cleanRequired(dto.getFloorNumber(), "Floor number is required"));
        flat.setDescription(clean(dto.getDescription()));
        flat.setTenantType(clean(dto.getTenantType()));
        flat.setRentalType(clean(dto.getRentTimeType()));
        flat.setPrice(dto.getPrice());
        flat.setLandlordMobileNumber(clean(dto.getMobileNumber()));

        EntityFlats savedFlat = flatsRepository.saveAndFlush(flat);

        refreshAffectedHomes(
                oldHomeId,
                savedFlat.getHome() == null ? null : savedFlat.getHome().getId()
        );

        propertyMediaService.updateImages(
                PropertyType.FLAT, savedFlat.getId(), imageIdsToDelete, newImages
        );

        return toDetailsDTO(flatsRepository.findDetailsById(savedFlat.getId()));
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
    public List<FlatDetailsDTO> getFlatsByHomeId(Long homeId){
        if(homeId == null || homeId<=0){
            throw  new IllegalArgumentException("Home id is required");
        }

        return flatsRepository.findDetailsByHomeId(homeId)
                .stream()
                .map(this::toDetailsDTO)
                .toList();
    }

    @Override
    public List<FlatListDTO> getFlatListByHomeId(Long homeId){
        if(homeId == null || homeId<=0){
            throw new IllegalArgumentException("Home id is required");
        }
        return flatsRepository.findDetailsByHomeId(homeId)
                .stream()
                .map(this::toListDTO)
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

        Long oldHomeId = flat.getHome() == null ? null : flat.getHome().getId();

        propertyMediaService.softDeleteAllImages(
                PropertyType.FLAT,
                flat.getId()
        );

        flat.setDeleted(true);
        flat.setActive(false);
        flat.setDeletedAt(LocalDateTime.now());

        flatsRepository.saveAndFlush(flat);
        refreshAffectedHomes(oldHomeId, null);
    }

    private void validateFlatRoomCountChange(EntityFlats flat, Integer newCount) {
        if (newCount == null || newCount <= 0) {
            throw new BusinessConflictException("Flat must have at least one declared room.");
        }
        long assigned = roomRepository.countActiveRoomsByFlatId(flat.getId(), null);
        if (newCount < assigned) {
            throw new BusinessConflictException(
                    "Flat room count cannot be reduced to " + newCount
                            + " because " + assigned + " active Room(s) are already assigned."
            );
        }
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

    private boolean isBuildingFlatRequest(FlatSaveRequestDTO dto) {
        return dto.getBuildingId() != null && dto.getHomeId() == null;
    }

    private boolean isHomeFlatRequest(FlatSaveRequestDTO dto) {
        return dto.getBuildingId() == null && dto.getHomeId() != null;
    }

    private EntityUnitStatus resolveUnitStatus(String statusCode) {
        if (statusCode == null || statusCode.isBlank()) {
            return unitStatusService.getDefaultStatus(StatusDomain.FLAT);
        }

        // Property registration and direct edit screens may only submit setup statuses.
        return unitStatusService.getRequiredSetupStatus(StatusDomain.FLAT, statusCode);
    }

    private void validateCreateRequest(FlatSaveRequestDTO dto, Long landlordId) {

        Map<String, String> errors = new LinkedHashMap<>();

        if (landlordId == null || landlordId <= 0) {
            errors.put("landlordId", "Authenticated landlord id is invalid.");
        }

        boolean hasBuilding = dto.getBuildingId() != null;

        boolean hasHome = dto.getHomeId() != null;

        if (hasBuilding == hasHome) {
            errors.put("parent", "Select exactly one building or one home.");
        }

        if (hasBuilding && dto.getBuildingId() <= 0) {
            errors.put("buildingId", "Building id must be greater than zero.");
        }

        if (hasHome && dto.getHomeId() <= 0) {
            errors.put("homeId", "Home id must be greater than zero.");
        }

        if (isBlank(dto.getFlatNumber())) {
            errors.put("flatNumber", "Flat number is required.");
        }

        if (dto.getNumberOfRooms() == null || dto.getNumberOfRooms() <= 0) {
            errors.put("numberOfRooms", "Number of rooms must be at least 1.");
        }

        if (isBlank(dto.getNumberOfBathrooms())) {
            errors.put("numberOfBathrooms", "Number of bathrooms is required.");
        }

        if (isBlank(dto.getFloorNumber())) {
            errors.put("floorNumber", "Floor number is required.");
        }

        if (isBlank(dto.getStatusCode())) {
            errors.put("statusCode", "Flat status is required.");
        }

        if (isBlank(dto.getTenantType())) {
            errors.put("tenantType", "Tenant type is required.");
        }

        if (isBlank(dto.getRentTimeType())) {
            errors.put("rentTimeType", "Rent time type is required.");
        }

        if (dto.getPrice() == null) {
            errors.put("price", "Price is required.");
        } else if (dto.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            errors.put("price", "Price cannot be negative.");
        }

        if (isBlank(dto.getMobileNumber())) {
            errors.put("mobileNumber", "Mobile number is required.");
        } else if (!dto.getMobileNumber().matches("^9\\d{7}$")
        ) {
            errors.put("mobileNumber", "Mobile number must be 8 digits and start with 9.");
        }

        if (!errors.isEmpty()) {
            throw new FieldValidationException("Please complete all required flat fields correctly.", errors);
        }
    }

    private void validateBuildingAcceptsFlats(EntityBuildings building) {

        String statusCode = building.getStatus() == null ? null
                : building.getStatus().getCode();

        if ("BLOCKED".equalsIgnoreCase(statusCode) || "UNDER_MAINTENANCE".equalsIgnoreCase(statusCode)) {
            throw new BusinessConflictException("Cannot add a flat because building status is " + statusCode + ".");
        }
    }

    private void validateHomeAcceptsFlats(EntityHomes home) {

        String statusCode = home.getStatus() == null ? null
                : home.getStatus().getCode();

        if ("BLOCKED".equalsIgnoreCase(statusCode) || "UNDER_MAINTENANCE".equalsIgnoreCase(statusCode)) {
            throw new BusinessConflictException("Cannot add a flat because home status is " + statusCode + ".");
        }
    }

    private void validateFlatNumberAvailableInBuilding(Long buildingId, String flatNumber, Long currentFlatId) {

        String cleaned = cleanRequired(flatNumber, "Flat number is required.");

        boolean exists = flatsRepository.existsByBuildingIdAndFlatNumberIgnoreCaseAndDeletedFalse(buildingId, cleaned);

        if (!exists) {
            return;
        }

        EntityFlats current = currentFlatId == null ? null
                : flatsRepository.findDetailsById(currentFlatId);

        boolean sameFlat =
                current != null
                        && current.getBuilding() != null
                        && Objects.equals(
                        current.getBuilding().getId(),
                        buildingId
                )
                        && cleaned.equalsIgnoreCase(
                        current.getFlatNumber()
                );

        if (!sameFlat) {
            throw new BusinessConflictException("Flat number already exists in this building.");
        }
    }

    private void validateFlatNumberAvailableInHome(Long homeId, String flatNumber, Long currentFlatId) {

        String cleaned = cleanRequired(flatNumber, "Flat number is required.");

        boolean exists = flatsRepository.existsByHomeIdAndFlatNumberIgnoreCaseAndDeletedFalse(homeId, cleaned);

        if (!exists) {
            return;
        }

        EntityFlats current = currentFlatId == null ? null
                : flatsRepository.findDetailsById(currentFlatId);

        boolean sameFlat =
                current != null
                        && current.getHome() != null
                        && Objects.equals(
                        current.getHome().getId(),
                        homeId
                )
                        && cleaned.equalsIgnoreCase(
                        current.getFlatNumber()
                );

        if (!sameFlat) {
            throw new BusinessConflictException("Flat number already exists in this home.");
        }
    }

    private void validateBuildingOwnership(EntityBuildings building, Long landlordId) {

        if (building.getLandlord() == null || !Objects.equals(building.getLandlord().getId(), landlordId)) {
            throw new AccessDeniedException("You are not allowed to add a flat to this building.");
        }
    }

    private void validateHomeOwnership(
            EntityHomes home,
            Long landlordId
    ) {

        if (home.getLandlord() == null
                || !Objects.equals(
                home.getLandlord().getId(),
                landlordId
        )) {
            throw new AccessDeniedException(
                    "You are not allowed to add a flat to this home."
            );
        }
    }


    private void validateFlatNumberAvailable(Long buildingId, String flatNumber, Long currentFlatId) {

        String cleanedFlatNumber = cleanRequired(flatNumber, "Flat number is required");

        boolean exists = flatsRepository.existsByBuildingIdAndFlatNumberIgnoreCaseAndDeletedFalse(buildingId, cleanedFlatNumber);

        if (!exists) {
            return;
        }

        if (currentFlatId == null) {
            throw new BusinessConflictException("Flat number already exists in this building.");
        }

        EntityFlats current = flatsRepository.findDetailsById(currentFlatId);

        if (current != null
                && current.getBuilding() != null
                && current.getBuilding().getId().equals(buildingId)
                && current.getFlatNumber().equalsIgnoreCase(cleanedFlatNumber)
        ) {
            return;
        }

        throw new BusinessConflictException("Flat number already exists in this building.");
    }

    private FlatDetailsDTO toDetailsDTO(EntityFlats flat) {
        EntityBuildings building = flat.getBuilding();
        EntityHomes home=flat.getHome();

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
                .hasAC(flat.isHasAC())
                .hasWIFI(flat.isHasWIFI())
                .isFurnished(flat.isFurnished())
                .status(toStatusDTO(flat.getStatus()))
                .floorNumber(flat.getFloorNumber())
                .description(flat.getDescription())


                .regionId(resolveRegion(flat).getId())
                .region(resolveRegion(flat).getNameEn())
                .regionAr(resolveRegion(flat).getNameAr())
                .wilayaId(resolveWilaya(flat).getId())
                .wilaya(resolveWilaya(flat).getNameEn())
                .wilayaAr(resolveWilaya(flat).getNameAr())
                .areaId(resolveArea(flat).getId())
                .area(resolveArea(flat).getNameEn())
                .areaAr(resolveArea(flat).getNameAr())

                .tenantType(flat.getTenantType())
                .rentTimeType(flat.getRentalType())
                .price(flat.getPrice())

                .latitude(resolveLatitude(flat))
                .longitude(resolveLongitude(flat))

                .landlordId(landlord != null ? landlord.getId() : null)
                .landlordName(getLandlordName(landlord))
                .landlordPhoneNumber(
                        landlord != null ? landlord.getPhoneNumber() : null
                )

                .buildingId(building != null ? building.getId() : null)
                .buildingName(building != null ? building.getBuildingName() : null)
                .buildingNumber(building != null ? building.getBuildingNumber() : null)

                .homeId(home != null ? home.getId() : null)
                .homeNumber(home != null ? home.getHomeNumber() : null)
                .homeLabel(home != null ?  home.getHomeType()+"."+home.getHomeNumber() : null)
                .homeType(home!=null ? home.getHomeType().toString() : null)

                .hasParking(resolveHasParking(flat))
                .hasGym(resolveHasGym(flat))
                .hasSwimmingPool(resolveHasSwimmingPool(flat))

                .images(imageUrls != null ? imageUrls : Collections.emptyList())
                .build();
    }

    private OmanRegionEntity resolveRegion(EntityFlats flat) {

        if (flat.getHome() != null) {
            return flat.getHome().getRegion();
        }

        if (flat.getBuilding() != null) {
            return flat.getBuilding().getRegion();
        }

        return null;
    }

    private OmanWilayaEntity resolveWilaya(EntityFlats flat) {

        if (flat.getHome() != null) {
            return flat.getHome().getWilaya();
        }

        if (flat.getBuilding() != null) {
            return flat.getBuilding().getWilaya();
        }

        return null;
    }

    private OmanAreaEntity resolveArea(EntityFlats flat) {

        if (flat.getHome() != null) {
            return flat.getHome().getArea();
        }

        if (flat.getBuilding() != null) {
            return flat.getBuilding().getArea();
        }

        return null;
    }

    private Double resolveLatitude(EntityFlats flat) {

        if (flat.getHome() != null) {
            return flat.getHome().getLatitude();
        }

        if (flat.getBuilding() != null) {
            return flat.getBuilding().getLatitude();
        }

        return null;
    }

    private Double resolveLongitude(EntityFlats flat) {

        if (flat.getHome() != null) {
            return flat.getHome().getLongitude();
        }

        if (flat.getBuilding() != null) {
            return flat.getBuilding().getLongitude();
        }

        return null;
    }

    private Boolean resolveHasParking(EntityFlats flat) {
        if (flat.getBuilding() != null) {
            return flat.getBuilding().isHasParking();
        }

        if (flat.getHome() != null) {
            return true;
        }

        return null;
    }

    private Boolean resolveHasGym(EntityFlats flat) {
        if (flat.getBuilding() != null) {
            return flat.getBuilding().isHasGym();
        }

        if (flat.getHome() != null) {
            return false;
        }

        return null;
    }

    private Boolean resolveHasSwimmingPool(EntityFlats flat) {
        if (flat.getBuilding() != null) {
            return flat.getBuilding().isHasSwimmingPool();
        }

        if (flat.getHome() != null) {
            return false;
        }

        return null;
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



    /** Recalculates a Flat status from its active Room inventory. */
    @Transactional
    public void refreshFlatState(Long flatId) {
        if (flatId == null || flatId <= 0) {
            return;
        }

        EntityFlats flat = flatsRepository.findByIdForUpdate(flatId).orElse(null);
        if (flat == null || flat.isDeleted()) {
            return;
        }

        String currentCode = flat.getStatus() == null ? null : flat.getStatus().getCode();
        if (isProtectedFlatStatus(flat.getStatus())) {
            refreshOwningHome(flat);
            return;
        }

        UnitStatusCountsProjection counts =
                roomRepository.getActiveRoomStatusCountsByFlatId(flatId);
        long total = safeCount(counts == null ? null : counts.getTotal());

        if (total == 0) {
            if (isDerivedFlatRoomStatus(currentCode)) {
                flat.setStatus(unitStatusService.getRequiredStatus(StatusDomain.FLAT, AVAILABLE));
                flatsRepository.save(flat);
            }
            refreshOwningHome(flat);
            return;
        }

        long available = safeCount(counts.getAvailable());
        long rented = safeCount(counts.getRented());

        String target = available == total
                ? AVAILABLE_AS_ROOMS
                : rented == total
                ? FULLY_RENTED_AS_ROOMS
                : PARTIALLY_RENTED_AS_ROOMS;

        flat.setStatus(unitStatusService.getRequiredStatus(StatusDomain.FLAT, target));
        flatsRepository.save(flat);
        refreshOwningHome(flat);
    }

    @Transactional()
    public void validateFlatRoomCapacity(Long flatId, Long excludedRoomId) {
        EntityFlats flat = flatsRepository.findDetailsById(flatId);
        if (flat == null) {
            throw new EntityNotFoundException("Flat not found with id: " + flatId);
        }

        int allowed = Optional.ofNullable(flat.getNumberOfRooms()).orElse(0);
        long assigned = roomRepository.countActiveRoomsByFlatId(flatId, excludedRoomId);

        if (allowed <= 0 || assigned >= allowed) {
            throw new BusinessConflictException(
                    "Cannot add another room. Flat " + flat.getFlatNumber()
                            + " allows only " + allowed + " room(s)."
            );
        }
    }

    private void refreshAffectedHomes(Long oldHomeId, Long newHomeId) {
        Set<Long> ids = new LinkedHashSet<>();
        if (oldHomeId != null && oldHomeId > 0) ids.add(oldHomeId);
        if (newHomeId != null && newHomeId > 0) ids.add(newHomeId);
        ids.forEach(homeService::refreshHomeState);
    }

    private void refreshOwningHome(EntityFlats flat) {
        if (flat != null && flat.getHome() != null) {
            homeService.refreshHomeState(flat.getHome().getId());
        }
    }

    private boolean isProtectedFlatStatus(EntityUnitStatus status) {
        if (status == null || status.getStatusLevel() == null) {
            return false;
        }

        if (status.getStatusLevel() == StatusLevel.L) {
            return true;
        }

        return status.getStatusLevel() == StatusLevel.U
                && !isDerivedFlatRoomStatus(status.getCode());
    }

    private boolean isDerivedFlatRoomStatus(String code) {
        if (code == null) return false;
        return Set.of(
                AVAILABLE_AS_ROOMS,
                PARTIALLY_RENTED_AS_ROOMS,
                FULLY_RENTED_AS_ROOMS
        ).contains(code.trim().toUpperCase(Locale.ROOT));
    }

    private long safeCount(Long value) {
        return value == null ? 0L : Math.max(0L, value);
    }

}
