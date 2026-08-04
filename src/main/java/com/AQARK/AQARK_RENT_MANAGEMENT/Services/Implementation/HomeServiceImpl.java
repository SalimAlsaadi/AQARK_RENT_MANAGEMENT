package com.AQARK.AQARK_RENT_MANAGEMENT.Services.Implementation;

import com.AQARK.AQARK_RENT_MANAGEMENT.Common_Utilities.PropertyMediaService;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.HomeDTO.*;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.LocationDTO.LocationDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.MediaDTO.PropertyMediaDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.StatusDTO.UnitStatusDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.*;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Enums.HomeRentalStructure;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Enums.PropertyType;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Enums.StatusDomain;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Enums.StatusLevel;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.UnitStatusCountsProjection;
import com.AQARK.AQARK_RENT_MANAGEMENT.Exceptions.BusinessConflictException;
import com.AQARK.AQARK_RENT_MANAGEMENT.Exceptions.FieldValidationException;
import com.AQARK.AQARK_RENT_MANAGEMENT.Repositories.*;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface.HomeServiceInterface;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface.UnitStatusServiceInterface;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

import static org.apache.logging.log4j.util.Strings.isBlank;

@Service
@RequiredArgsConstructor
public class HomeServiceImpl implements HomeServiceInterface {

    private static final String HOME_STATUS_DOMAIN = "HOME";
    private static final String ROOM_STATUS_CODE_RENTED = "RENTED";

    private final OmanLocationServiceImpl omanLocationService;
    /*
     * These statuses are calculated automatically based on rooms.
     * Frontend must not force them manually.
     */
    private static final Set<String> AUTO_MANAGED_HOME_STATUSES = Set.of(
            "AVAILABLE_AS_MULTI_UNITS",
            "PARTIALLY_RENTED_AS_MULTI_UNITS",
            "FULLY_RENTED_AS_MULTI_UNITS",
            "AVAILABLE_AS_ROOMS",
            "PARTIALLY_RENTED_AS_ROOMS",
            "FULLY_RENTED_AS_ROOMS",
            "AVAILABLE_AS_FLATS",
            "PARTIALLY_RENTED_AS_FLATS",
            "FULLY_RENTED_AS_FLATS",
            "AVAILABLE_AS_FLAT_AND_ROOMS"
    );

    private final HomeRepository homeRepository;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final FlatsRepository flatsRepository;

    private final OmanRegionRepository regionRepository;
    private final OmanWilayaRepository wilayaRepository;
    private final OmanAreaRepository areaRepository;
    private final UnitStatusRepository unitStatusRepository;

    private final PropertyMediaService propertyMediaService;
    private final UnitStatusServiceInterface unitStatusService;

    @Override
    @Transactional
    public HomeDetailsDTO createHome(
            HomeCreateRequestDTO dto,
            List<MultipartFile> pictures,
            Long authenticatedLandlordId
    ) throws IOException {

        validateCreateRequest(dto, authenticatedLandlordId);

        EntityUser landlord = getLandlord(authenticatedLandlordId);
        LocationData locationData = validateAndGetLocation(
                dto.getRegionId(), dto.getWilayaId(), dto.getAreaId()
        );

        EntityHomes home = EntityHomes.builder()
                .homeType(dto.getHomeType())
                .rentalStructure(dto.getRentalStructure())
                .homeNumber(cleanRequired(dto.getHomeNumber(), "Home number is required."))
                .numberOfRooms(dto.getNumberOfRooms())
                .numberOfBathrooms(dto.getNumberOfBathrooms())
                .hasLivingRoom(Boolean.TRUE.equals(dto.getHasLivingRoom()))
                .furnished(Boolean.TRUE.equals(dto.getFurnished()))
                .numberOfFloor(dto.getNumberOfFloor())
                .description(clean(dto.getDescription()))
                .region(locationData.region())
                .wilaya(locationData.wilaya())
                .area(locationData.area())
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .tenantType(cleanRequired(dto.getTenantType(), "Tenant type is required."))
                .rentType(cleanRequired(dto.getRentTimeType(), "Rent time type is required."))
                .price(dto.getPrice())
                .status(resolveInitialHomeStatus(dto.getRentalStructure()))
                .landlord(landlord)
                .active(true)
                .deleted(false)
                .build();

        EntityHomes savedHome = homeRepository.saveAndFlush(home);

        if (pictures != null && !pictures.isEmpty()) {
            propertyMediaService.saveImages(pictures, PropertyType.HOME, savedHome.getId());
        }

        EntityHomes loadedHome = homeRepository.findDetailsById(savedHome.getId());
        return toDetailsDTO(loadedHome == null ? savedHome : loadedHome);
    }

    @Override
    @Transactional
    public HomeDetailsDTO updateHome(HomeUpdateRequestDTO dto, List<MultipartFile> newImages, List<Long> imageIdsToDelete, Long authenticatedLandlordId) throws IOException {

        if (dto == null) {
            throw new FieldValidationException(
                    "Home request body is required.",
                    Map.of("data", "Home request body is required.")
            );
        }

        EntityHomes home = getOwnedHome(dto.getId(), authenticatedLandlordId);

        if (dto.getNumberOfRooms() != null) {
            validateHomeRoomCountReduction(home, dto.getNumberOfRooms());
        }

        updateHomeFields(home, dto);

        // Keep the landlord-selected structure. Child creation may automatically
        // upgrade WHOLE_HOME to MULTI_UNITS_HOME, but no operation silently downgrades it.

        EntityHomes savedHome = homeRepository.saveAndFlush(home);

        propertyMediaService.updateImages(
                PropertyType.HOME,
                savedHome.getId(),
                imageIdsToDelete,
                newImages
        );

        return toDetailsDTO(savedHome);
    }

    @Override
    @Transactional
    public HomeDetailsDTO changeHomeStatus(HomeChangeStatusRequestDTO dto, Long authenticatedLandlordId) {

        EntityHomes home = getOwnedHome(dto.getHomeId(), authenticatedLandlordId);

        EntityUnitStatus requestedStatus = getHomeStatusById(dto.getStatusId());
        validateSetupStatus(requestedStatus);

        String requestedCode = requestedStatus.getCode();

        if (AUTO_MANAGED_HOME_STATUSES.contains(requestedCode)) {
            throw new IllegalStateException(
                    "This status is managed automatically based on home rooms."
            );
        }

        validateManualStatusTransition(home, requestedCode);

        home.setStatus(requestedStatus);

        if ("INACTIVE".equalsIgnoreCase(requestedCode)) {
            home.setActive(false);
        } else {
            home.setActive(true);
        }

        EntityHomes savedHome = homeRepository.save(home);

        return toDetailsDTO(savedHome);
    }

    @Override
    @Transactional(readOnly = true)
    public HomeDetailsDTO getHomeById(
            Long homeId,
            Long authenticatedLandlordId
    ) {
        validateId(homeId, "homeId", "Home id is required.");
        validateId(authenticatedLandlordId, "landlordId", "Authenticated landlord id is required.");

        EntityHomes home = homeRepository.findDetailsById(homeId);

        if (home == null) {
            throw new EntityNotFoundException(
                    "Home not found with id: " + homeId
            );
        }

        validateOwnership(home, authenticatedLandlordId);

        return toDetailsDTO(home);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HomeListDTO> getAllHomes() {

        List<EntityHomes> homes = homeRepository.findAllDetails();

        if (homes.isEmpty()) {
            return List.of();
        }

        Map<Long, HomeUnitCountsProjection> counts =
                toCountsMap(homeRepository.getAllHomeUnitCounts());

        return homes.stream()
                .map(home -> toListDTO(
                        home,
                        counts.get(home.getId())
                ))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<HomeListDTO> getHomesByLandlord(Long landlordId) {
        validateId(landlordId, "landlordId", "Landlord id is required.");

        List<EntityHomes> homes = homeRepository.findDetailsByLandlordId(landlordId);

        if (homes.isEmpty()) {
            return List.of();
        }

        Map<Long, HomeUnitCountsProjection> counts = toCountsMap(homeRepository.getHomeUnitCountsByLandlordId(landlordId));

        return homes
                .stream()
                .map(home -> toListDTO(home, counts.get(home.getId())))
                .toList();
    }


    @Override
    @Transactional
    public void deleteHome(Long homeId, Long authenticatedLandlordId) {
        EntityHomes home = getOwnedHome(homeId, authenticatedLandlordId);

        HomeUnitCountsDTO counts = getUnitCounts(homeId);

        long totalChildUnits =
                counts.getTotalFlats()
                        + counts.getTotalRooms();


        if (totalChildUnits > 0) {
            throw new BusinessConflictException("Cannot delete home because it has related flats, rooms, or shops. Deactivate it instead.");
        }

        propertyMediaService.softDeleteAllImages(
                PropertyType.HOME,
                homeId
        );

        home.setDeleted(true);
        home.setActive(false);
        home.setDeletedAt(LocalDateTime.now());

        /*
         * Set INACTIVE only when this status exists
         * in the HOME domain.
         */
        home.setStatus(getHomeStatusByCode("INACTIVE"));

        homeRepository.save(home);
    }


    private void validateCreateRequest(HomeCreateRequestDTO dto, Long authenticatedLandlordId) {
        Map<String, String> errors = new LinkedHashMap<>();

        if (dto == null) {
            throw new FieldValidationException("Home request body is required.", Map.of("data", "Home request body is required."));
        }

        if (authenticatedLandlordId == null || authenticatedLandlordId <= 0) {
            errors.put(
                    "landlordId",
                    "Authenticated landlord id is not valid or not found."
            );
        }

        if (dto.getRentalStructure() == null) {
            errors.put("rentalStructure", "Home rental structure is required.");
        }

        if (isBlank(dto.getHomeType().toString())) {
            errors.put(
                    "homeType",
                    "Home type is required."
            );
        }

        if (isBlank(dto.getHomeNumber())) {
            errors.put(
                    "homeNumber",
                    "Home number is required."
            );
        }

        if (dto.getNumberOfRooms() == null || dto.getNumberOfRooms() <= 0) {
            errors.put(
                    "numberOfRooms",
                    "Number of rooms must be at least 1."
            );
        }

        if (dto.getNumberOfRooms() != null && dto.getNumberOfRooms() > 100) {
            errors.put(
                    "numberOfRooms",
                    "Number of rooms cannot exceed 100."
            );
        }

        if (dto.getNumberOfBathrooms() == null || dto.getNumberOfBathrooms() <= 0) {
            errors.put(
                    "numberOfBathrooms",
                    "Number of bathrooms must be at least 1."
            );
        }

        if (dto.getNumberOfBathrooms() != null && dto.getNumberOfBathrooms() > 100) {
            errors.put(
                    "numberOfBathrooms",
                    "Number of bathrooms cannot exceed 100."
            );
        }

        if (dto.getNumberOfFloor() == null || dto.getNumberOfFloor() < 0) {
            errors.put(
                    "numberOfFloor",
                    "Number of floors is required and cannot be negative."
            );
        }

        if (isBlank(dto.getDescription())) {
            errors.put(
                    "description",
                    "Description is required."
            );
        }

        if (dto.getRegionId() == null || dto.getRegionId() <= 0) {
            errors.put(
                    "regionId",
                    "Region is required."
            );
        }

        if (dto.getWilayaId() == null || dto.getWilayaId() <= 0) {
            errors.put(
                    "wilayaId",
                    "Wilaya is required."
            );
        }

        if (dto.getAreaId() == null || dto.getAreaId() <= 0) {
            errors.put(
                    "areaId",
                    "Area is required."
            );
        }

        if (dto.getLatitude() == null) {
            errors.put(
                    "latitude",
                    "Latitude is required."
            );
        } else if (dto.getLatitude().compareTo((double) -90) < 0
                || dto.getLatitude().compareTo(90.0) > 0) {
            errors.put(
                    "latitude",
                    "Latitude must be between -90 and 90."
            );
        }

        if (dto.getLongitude() == null) {
            errors.put(
                    "longitude",
                    "Longitude is required."
            );
        } else if (dto.getLongitude().compareTo((double) -180) < 0
                || dto.getLongitude().compareTo(180.0) > 0) {
            errors.put(
                    "longitude",
                    "Longitude must be between -180 and 180."
            );
        }


        /*
         * Whole-Home rental fields.
         *
         * For ROOMS, FLATS and FLATS_AND_ROOMS,
         * price and rental details are stored on child units.
         */
        validateRentalStructureFields(dto, errors);


        if (!errors.isEmpty()) {
            throw new FieldValidationException(
                    "Please complete all required home fields correctly.",
                    errors
            );
        }


        /*
         * Duplicate Home number check should run after
         */
        if (homeRepository.existsByLandlordIdAndHomeNumberIgnoreCaseAndDeletedFalse(authenticatedLandlordId, dto.getHomeNumber())) {
            throw new BusinessConflictException(
                    "Home number already exists for this landlord."
            );
        }

    }


    private EntityHomes getOwnedHome(
            Long homeId,
            Long authenticatedLandlordId
    ) {
        validateId(homeId, "homeId", "Home id is required.");

        if (authenticatedLandlordId == null
                || authenticatedLandlordId <= 0) {
            throw new AccessDeniedException(
                    "Authenticated landlord id is required."
            );
        }

        EntityHomes home =
                homeRepository.findDetailsById(homeId);

        if (home == null) {
            throw new EntityNotFoundException(
                    "Home not found with id: " + homeId
            );
        }

        validateOwnership(home, authenticatedLandlordId);

        return home;
    }

    private EntityUser getLandlord(Long landlordId) {

        if (landlordId == null || landlordId <= 0) {
            throw new AccessDeniedException("Authenticated landlord id is required.");
        }

        return userRepository.findById(landlordId).orElseThrow(() -> new EntityNotFoundException("Landlord not found with ID: " + landlordId));
    }

    private LocationData validateAndGetLocation(Long regionId, Long wilayaId, Long areaId) {
        Map<String, String> errors = new LinkedHashMap<>();

        if (regionId == null || regionId <= 0) {
            errors.put("regionId", "Region is required.");
        }

        if (wilayaId == null || wilayaId <= 0) {
            errors.put("wilayaId", "Wilaya is required.");
        }

        if (areaId == null || areaId <= 0) {
            errors.put("areaId", "Area is required.");
        }

        if (!errors.isEmpty()) {
            throw new FieldValidationException("Please complete the location fields correctly.", errors);
        }

        OmanRegionEntity region = regionRepository.findById(regionId).orElseThrow(() -> new FieldValidationException(
                "Selected region is invalid.", Map.of("regionId", "Region does not exist.")));

        OmanWilayaEntity wilaya = wilayaRepository.findByIdAndRegionId(wilayaId, regionId);

        if (wilaya == null) {
            throw new FieldValidationException(
                    "Selected wilaya is invalid.",
                    Map.of("wilayaId", "Wilaya does not exist or does not belong to the selected region."));
        }

        OmanAreaEntity area = areaRepository.findByIdAndWilayaId(areaId, wilayaId);

        if (area == null) {
            throw new FieldValidationException(
                    "Selected area is invalid.", Map.of("areaId", "Area does not exist or does not belong to the selected wilaya."));
        }

        return new LocationData(region, wilaya, area);
    }


    //in future i will update based on new transaction
    private EntityUnitStatus resolveInitialHomeStatus(HomeRentalStructure rentalStructure) {


        String statusCode = switch (rentalStructure) {
            case WHOLE_HOME -> "AVAILABLE";

            case MULTI_UNITS_HOME -> "AVAILABLE_AS_MULTI_UNITS";


        };

        return unitStatusRepository.findByStatusDomainAndCodeAndActiveTrue(HOME_STATUS_DOMAIN, statusCode)
                .orElseThrow(() -> new IllegalStateException("Required HOME status is not configured: " + statusCode));
    }

    private EntityUnitStatus getHomeStatusById(Long statusId) {

        EntityUnitStatus status = unitStatusRepository.findById(statusId).orElseThrow(() -> new IllegalArgumentException("Status not found or inactive with ID: " + statusId));

        validateHomeStatusDomain(status);

        return status;
    }


    private EntityUnitStatus getHomeStatusByCode(String code) {

        return unitStatusRepository.findByStatusDomainAndCodeAndActiveTrue(HOME_STATUS_DOMAIN, code)
                .orElseThrow(() -> new IllegalStateException("HOME status is not configured: " + code));
    }


    private void validateSetupStatus(EntityUnitStatus status) {
        if (status == null || status.getStatusLevel() != StatusLevel.S) {
            throw new BusinessConflictException(
                    "Only setup-level HOME statuses can be selected manually."
            );
        }
    }

    private boolean isHomeRefreshProtected(EntityUnitStatus status) {
        if (status == null || status.getStatusLevel() == null) {
            return false;
        }

        if (status.getStatusLevel() == StatusLevel.L) {
            return true;
        }

        return status.getStatusLevel() == StatusLevel.U && !AUTO_MANAGED_HOME_STATUSES.contains((status.getCode()));
    }

    private void validateHomeStatusDomain(EntityUnitStatus status) {

        if (!HOME_STATUS_DOMAIN.equalsIgnoreCase(status.getStatusDomain())) {
            throw new IllegalArgumentException("Selected status does not belong to HOME domain.");
        }
    }


    //i will imrove it, to enable home rent rooms and whole home at the same time. because sometimes rooms in al molhaq home
    private void validateRentalStructureFields(
            HomeCreateRequestDTO dto,
            Map<String, String> errors
    ) {
        if (isBlank(dto.getTenantType())) {
            errors.put("tenantType", "Tenant type is required.");
        }
        if (isBlank(dto.getRentTimeType())) {
            errors.put("rentTimeType", "Rent time type is required.");
        }
        if (dto.getPrice() == null) {
            errors.put("price", "Home price is required.");
        } else if (dto.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            errors.put("price", "Home price cannot be negative.");
        }
    }

    private void validateHomeRoomCountReduction(EntityHomes home, int newCount) {
        if (newCount <= 0) {
            throw new BusinessConflictException("Home must have at least one declared direct room.");
        }
        long assigned = roomRepository.countActiveDirectRoomsByHomeId(home.getId(), null);
        if (newCount < assigned) {
            throw new BusinessConflictException(
                    "Home room count cannot be reduced to " + newCount
                            + " because " + assigned + " active direct Room(s) are already assigned."
            );
        }
    }

    private void validateManualStatusTransition(EntityHomes home, String requestedCode) {
        if (requestedCode == null) return;

        String normalized = requestedCode.trim().toUpperCase(Locale.ROOT);
        if (AUTO_MANAGED_HOME_STATUSES.contains(normalized)) {
            throw new BusinessConflictException(
                    "This Home status is managed automatically from child inventory."
            );
        }

        if (("RENTED".equals(normalized) || "RESERVED".equals(normalized))
                && home.getRentalStructure() == HomeRentalStructure.MULTI_UNITS_HOME
                && calculateRemainingDirectRoomCapacity(home) <= 0) {
            throw new BusinessConflictException(
                    "The remaining Home cannot be rented or reserved because all declared direct rooms "
                            + "have already been assigned as independent Room units."
            );
        }
    }

    private HomeUnitCountsDTO getHomeUnitCounts(Long homeId) {

        List<EntityFlats> flats = flatsRepository
                .findDetailsByHomeId(homeId)
                .stream()
                .filter(EntityFlats::isActive)
                .filter(flat -> !flat.isDeleted())
                .toList();

        List<EntityRooms> directRooms = roomRepository
                .findDirectRoomsByHomeId(homeId)
                .stream()
                .filter(EntityRooms::isActive)
                .filter(room -> !room.isDeleted())
                .toList();

        List<EntityRooms> flatRooms = roomRepository
                .findRoomsInsideHomeFlats(homeId)
                .stream()
                .filter(EntityRooms::isActive)
                .filter(room -> !room.isDeleted())
                .toList();


        long flatsAvailable = countFlatsWithStatuses(flats, Set.of("AVAILABLE"));

        long flatsRented = countFlatsWithStatuses(flats, Set.of("RENTED"));

        long flatsReserved = countFlatsWithStatuses(flats, Set.of("RESERVED"));

        long flatsMaintenance = countFlatsWithStatuses(flats, Set.of("UNDER_MAINTENANCE"));

        long flatsBlocked = countFlatsWithStatuses(flats, Set.of("BLOCKED"));

        List<EntityRooms> allRooms = new ArrayList<>();

        allRooms.addAll(directRooms);
        allRooms.addAll(flatRooms);

        long roomsAvailable = countRoomsWithStatus(allRooms, "AVAILABLE");

        long roomsRented = countRoomsWithStatus(allRooms, "RENTED");

        long roomsReserved = countRoomsWithStatus(allRooms, "RESERVED");

        long roomsMaintenance = countRoomsWithStatus(allRooms, "UNDER_MAINTENANCE");

        long roomsBlocked = countRoomsWithStatus(allRooms, "BLOCKED");

        /*
         * Flats divided into rooms are not whole rentable flat units.
         */
        long wholeFlatUnits = flats
                .stream()
                .filter(flat -> !isFlatDividedIntoRooms(statusCode(flat.getStatus())))
                .count();

        long totalRooms = allRooms.size();

        return HomeUnitCountsDTO.builder()
                .totalFlats(flats.size())
                .flatsAvailable(flatsAvailable)
                .flatsRented(flatsRented)
                .flatsReserved(flatsReserved)
                .flatsUnderMaintenance(flatsMaintenance)
                .flatsBlocked(flatsBlocked)
                .directRooms(directRooms.size())
                .flatsRentedAsRooms(flatRooms.size())
                .totalRooms(totalRooms)
                .roomsAvailable(roomsAvailable)
                .roomsRented(roomsRented)
                .roomsReserved(roomsReserved)
                .roomsUnderMaintenance(roomsMaintenance)
                .roomsBlocked(roomsBlocked)
                .availableUnits(flatsAvailable + roomsAvailable)
                .rentedUnits(flatsRented + roomsRented)
                .reservedUnits(flatsReserved + roomsReserved)
                .maintenanceUnits(flatsMaintenance + roomsMaintenance)
                .blockedUnits(flatsBlocked + roomsBlocked)
                .build();
    }

    private long countFlatsWithStatuses(List<EntityFlats> flats, Set<String> statusCodes) {
        return flats.stream()
                .filter(flat -> flat.getStatus() != null)
                .filter(flat -> statusCodes.contains(flat.getStatus().getCode().toUpperCase(Locale.ROOT)))
                .count();
    }

    private long countRoomsWithStatus(List<EntityRooms> rooms, String statusCode) {
        return rooms.stream()
                .filter(room -> room.getStatus() != null)
                .filter(room -> statusCode.equalsIgnoreCase(room.getStatus().getCode()))
                .count();
    }

    private boolean isFlatDividedIntoRooms(String code) {
        return "AVAILABLE_AS_ROOMS".equalsIgnoreCase(code)
                || "PARTIALLY_RENTED_AS_ROOMS".equalsIgnoreCase(code)
                || "FULLY_RENTED_AS_ROOMS".equalsIgnoreCase(code);
    }

    private String statusCode(EntityUnitStatus status) {
        return status == null ? null : status.getCode();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }

    private String cleanRequired(String value, String message) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(message);
        }

        return value.trim();
    }


    private void updateHomeFields(EntityHomes home, HomeUpdateRequestDTO dto) {

        if (dto.getHomeType() != null) {
            home.setHomeType(dto.getHomeType());
        }

        if (dto.getHomeNumber() != null) {
            home.setHomeNumber(dto.getHomeNumber());
        }

        if (dto.getNumberOfRooms() != null) {
            home.setNumberOfRooms(dto.getNumberOfRooms());
        }

        if (dto.getNumberOfBathrooms() != null) {
            home.setNumberOfBathrooms(dto.getNumberOfBathrooms());
        }

        if (dto.getHasLivingRoom() != null) {
            home.setHasLivingRoom(dto.getHasLivingRoom());
        }

        if (dto.getFurnished() != null) {
            home.setFurnished(dto.getFurnished());
        }

        if (dto.getNumberOfFloor() != null) {
            home.setNumberOfFloor(dto.getNumberOfFloor());
        }

        if (dto.getDescription() != null) {
            home.setDescription(dto.getDescription());
        }

        if (dto.getRegionId() != null || dto.getWilayaId() != null || dto.getAreaId() != null) {

            Long regionId = dto.getRegionId() != null
                    ? dto.getRegionId()
                    : home.getRegion().getId();

            Long wilayaId = dto.getWilayaId() != null
                    ? dto.getWilayaId()
                    : home.getWilaya().getId();

            Long areaId = dto.getAreaId() != null
                    ? dto.getAreaId()
                    : home.getArea().getId();

            LocationData locationData = validateAndGetLocation(regionId, wilayaId, areaId);

            home.setRegion(locationData.region());
            home.setWilaya(locationData.wilaya());
            home.setArea(locationData.area());
        }

        if (dto.getLatitude() != null) {
            home.setLatitude(dto.getLatitude());
        }

        if (dto.getLongitude() != null) {
            home.setLongitude(dto.getLongitude());
        }

        if (dto.getTenantType() != null) {
            home.setTenantType(dto.getTenantType());
        }

        if (dto.getRentType() != null) {
            home.setRentType(dto.getRentType());
        }

        if (dto.getPrice() != null) {
            home.setPrice(dto.getPrice());
        }
    }


    private HomeDetailsDTO toDetailsDTO(EntityHomes home) {

        EntityUser landlord = home.getLandlord();

        return HomeDetailsDTO.builder()
                .id(home.getId())
                .homeType(home.getHomeType())
                .rentalStructure(home.getRentalStructure())
                .homeNumber(home.getHomeNumber())
                .numberOfRooms(home.getNumberOfRooms())
                .numberOfBathrooms(home.getNumberOfBathrooms())
                .hasLivingRoom(home.isHasLivingRoom())
                .furnished(home.isFurnished())
                .numberOfFloor(home.getNumberOfFloor())
                .description(home.getDescription())
                .location(toLocationDTO(home))
                .tenantType(home.getTenantType())
                .rentType(home.getRentType())
                .price(home.getPrice())
                .status(toStatusDTO(home.getStatus()))
                .unitCounts(getUnitCounts(home.getId()))
                .landlordId(landlord != null ? landlord.getId() : null)
                .landlordName(getLandlordFullName(landlord))
                .landlordMobileNumber(
                        landlord != null
                                ? landlord.getPhoneNumber()
                                : null
                )
                .active(home.isActive())
                .deleted(home.isDeleted())
                .createdAt(home.getCreatedAt())
                .updatedAt(home.getUpdatedAt())
                .images(getImages(home.getId()))
                .build();
    }

    private HomeListDTO toListDTO(EntityHomes home, HomeUnitCountsProjection projection) {
        HomeUnitCountsDTO counts = toCountsDTO(projection);

        EntityUnitStatus status = home.getStatus();

        return HomeListDTO.builder()
                .id(home.getId())
                .homeType(home.getHomeType())
                .homeNumber(home.getHomeNumber())
                .rentalStructure(home.getRentalStructure())
                .region(home.getRegion() != null ? home.getRegion().getNameEn() : null)
                .wilaya(home.getWilaya() != null ? home.getWilaya().getNameEn() : null)
                .area(home.getArea() != null ? home.getArea().getNameEn() : null)
                .statusCode(status != null ? status.getCode() : null)
                .statusNameEn(status != null ? status.getNameEn() : null)
                .statusNameAr(status != null ? status.getNameAr() : null)
                .statusColorCode(status != null ? status.getColorCode() : null)
                .statusIcon(status != null ? status.getIcon() : null)
                .numberOfFloors(home.getNumberOfFloor())
                .declaredNumberOfRooms(home.getNumberOfRooms())
                .numberOfBathrooms(home.getNumberOfBathrooms())
                .price(home.getPrice())
                .rentType(home.getRentType())
                .totalFlats(counts.getTotalFlats())
                .totalRooms(counts.getTotalRooms())
                .availableUnits(counts.getAvailableUnits())
                .rentedUnits(counts.getRentedUnits())
                .maintenanceUnits(counts.getMaintenanceUnits())
                .mainImageUrls(getPictureUrls(home.getId()))
                .build();
    }


    private HomeUnitCountsDTO getUnitCounts(Long homeId) {
        validateId(homeId, "homeId", "Home id is required.");

        return toCountsDTO(
                homeRepository.getHomeUnitCounts(homeId)
        );
    }

    private HomeUnitCountsDTO toCountsDTO( HomeUnitCountsProjection projection) {
        if (projection == null) {
            return emptyCounts();
        }

        long directRooms = nullToZero(projection.getDirectRooms());

        long roomsInFlat = nullToZero(projection.getRoomsInFlat());

        long totalRooms = nullToZero(projection.getTotalRooms());

        /*
         * Defensive consistency check.
         */
        if (totalRooms != directRooms + roomsInFlat) {
            totalRooms = directRooms + roomsInFlat;
        }

        long flatsAvailable = nullToZero(projection.getFlatsAvailable());

        long flatsRented = nullToZero(projection.getFlatsRented());

        long flatsReserved = nullToZero(projection.getFlatsReserved());

        long flatsUnderMaintenance = nullToZero(projection.getFlatsUnderMaintenance());

        long flatsBlocked = nullToZero(projection.getFlatsBlocked());

        long roomsAvailable = nullToZero(projection.getRoomsAvailable());

        long roomsRented = nullToZero(projection.getRoomsRented());

        long roomsReserved = nullToZero(projection.getRoomsReserved());

        long roomsUnderMaintenance = nullToZero(projection.getRoomsUnderMaintenance());

        long roomsBlocked = nullToZero(projection.getRoomsBlocked());

        return HomeUnitCountsDTO.builder()
                .totalFlats(nullToZero(projection.getTotalFlats()))
                .flatsAvailable(flatsAvailable)
                .flatsRented(flatsRented)
                .flatsRentedAsRooms(nullToZero(projection.getFlatsRentedAsRooms()))
                .flatsReserved(flatsReserved)
                .flatsUnderMaintenance(flatsUnderMaintenance)
                .flatsBlocked(flatsBlocked)
                .totalRooms(totalRooms)
                .directRooms(directRooms)
                .roomsInFlat(roomsInFlat)
                .roomsAvailable(roomsAvailable)
                .roomsRented(roomsRented)
                .roomsReserved(roomsReserved)
                .roomsUnderMaintenance(roomsUnderMaintenance)
                .roomsBlocked(roomsBlocked)

                .availableUnits(flatsAvailable + roomsAvailable)
                .rentedUnits(flatsRented + roomsRented)
                .maintenanceUnits(flatsUnderMaintenance + roomsUnderMaintenance)



                .build();
    }


    private HomeUnitCountsDTO emptyCounts() {
        return HomeUnitCountsDTO.builder()
                .totalFlats(0)
                .flatsAvailable(0)
                .flatsRented(0)
                .flatsRentedAsRooms(0)
                .flatsReserved(0)
                .flatsUnderMaintenance(0)
                .flatsBlocked(0)

                .totalRooms(0)
                .directRooms(0)
                .roomsInFlat(0)
                .roomsAvailable(0)
                .roomsRented(0)
                .roomsReserved(0)
                .roomsUnderMaintenance(0)
                .roomsBlocked(0)

                .availableUnits(0)
                .rentedUnits(0)
                .reservedUnits(0)
                .maintenanceUnits(0)
                .blockedUnits(0)
                .build();
    }


    private Map<Long, HomeUnitCountsProjection> toCountsMap(
            List<HomeUnitCountsProjection> projections
    ) {
        if (projections == null || projections.isEmpty()) {
            return Map.of();
        }

        return projections.stream()
                .filter(projection -> projection.getHomeId() != null)
                .collect(Collectors.toMap(
                        HomeUnitCountsProjection::getHomeId,
                        Function.identity(),
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ));
    }


    private long nullToZero(Long value) {
        return value == null ? 0L : value;
    }

    private void validateId(
            Long id,
            String fieldName,
            String message
    ) {
        if (id == null || id <= 0) {
            throw new FieldValidationException(
                    message,
                    Map.of(fieldName, message)
            );
        }
    }

    private List<PropertyMediaDTO> getImages(Long homeId) {
        List<PropertyMediaDTO> images =
                propertyMediaService.getImages(
                        PropertyType.HOME,
                        homeId
                );

        return images == null
                ? List.of()
                : images;
    }

    private List<String> getPictureUrls(Long homeId) {
        return getImages(homeId)
                .stream()
                .map(PropertyMediaDTO::getFileUrl)
                .toList();
    }

    private UnitStatusDTO toStatusDTO(EntityUnitStatus status) {
        if (status == null) {
            return null;
        }

        return UnitStatusDTO.builder()
                .id(status.getId())
                .code(status.getCode())
                .nameEn(status.getNameEn())
                .nameAr(status.getNameAr())
                .colorCode(status.getColorCode())
                .icon(status.getIcon())
                .active(status.isActive())
                .build();
    }


    private LocationDTO toLocationDTO(EntityHomes home) {
        return LocationDTO.builder()
                .regionId(
                        home.getRegion() != null
                                ? home.getRegion().getId()
                                : null
                )
                .regionNameEn(
                        home.getRegion() != null
                                ? home.getRegion().getNameEn()
                                : null
                )
                .regionNameAr(
                        home.getRegion() != null
                                ? home.getRegion().getNameAr()
                                : null
                )
                .wilayaId(
                        home.getWilaya() != null
                                ? home.getWilaya().getId()
                                : null
                )
                .wilayaNameEn(
                        home.getWilaya() != null
                                ? home.getWilaya().getNameEn()
                                : null
                )
                .wilayaNameAr(
                        home.getWilaya() != null
                                ? home.getWilaya().getNameAr()
                                : null
                )
                .areaId(
                        home.getArea() != null
                                ? home.getArea().getId()
                                : null
                )
                .areaNameEn(
                        home.getArea() != null
                                ? home.getArea().getNameEn()
                                : null
                )
                .areaNameAr(
                        home.getArea() != null
                                ? home.getArea().getNameAr()
                                : null
                )
                .latitude(home.getLatitude())
                .longitude(home.getLongitude())
                .build();
    }


    private void validateOwnership(
            EntityHomes home,
            Long authenticatedLandlordId
    ) {
        if (home.getLandlord() == null
                || !authenticatedLandlordId.equals(
                home.getLandlord().getId()
        )) {

            throw new AccessDeniedException(
                    "You are not allowed to access this home."
            );
        }
    }


    private UnitStatusDTO convertStatusDTO(EntityUnitStatus status) {

        return UnitStatusDTO.builder()
                .id(status.getId())
                .code(status.getCode())
                .nameEn(status.getNameEn())
                .nameAr(status.getNameAr())
                .colorCode(status.getColorCode())
                .icon(status.getIcon())
                .build();
    }

    private LocationDTO convertLocationDTO(EntityHomes home) {

        return LocationDTO.builder()
                .regionId(home.getRegion().getId())
                .regionNameEn(home.getRegion().getNameEn())
                .regionNameAr(home.getRegion().getNameAr())
                .wilayaId(home.getWilaya().getId())
                .wilayaNameEn(home.getWilaya().getNameEn())
                .wilayaNameAr(home.getWilaya().getNameAr())
                .areaId(home.getArea().getId())
                .areaNameEn(home.getArea().getNameEn())
                .areaNameAr(home.getArea().getNameAr())
                .build();
    }

    private String getLandlordFullName(EntityUser landlord) {

        if (landlord == null) {
            return null;
        }

        return (
                (landlord.getFirstName() == null
                        ? ""
                        : landlord.getFirstName())
                        + " "
                        + (landlord.getLastName() == null
                        ? ""
                        : landlord.getLastName())
        ).trim();
    }

    private record LocationData(
            OmanRegionEntity region,
            OmanWilayaEntity wilaya,
            OmanAreaEntity area
    ){}


    /**
     * Recalculates Home structure and aggregate status after direct Room or Flat changes.
     * This method is intentionally idempotent and safe to call more than once in one transaction.
     */
    @Transactional
    public void refreshHomeState(Long homeId) {
        if (homeId == null || homeId <= 0) {
            return;
        }

        EntityHomes home = homeRepository.findByIdForUpdate(homeId).orElse(null);
        if (home == null || home.isDeleted()) {
            return;
        }

        long directRooms = roomRepository.countActiveDirectRoomsByHomeId(homeId, null);
        long flats = flatsRepository.countByHomeIdAndActiveTrueAndDeletedFalse(homeId);

        boolean hasChildUnits = directRooms > 0 || flats > 0;

        // A child Flat or direct Room always upgrades the Home to multi-unit.
        // Deleting the final child does not silently downgrade the landlord's selected model.
        if (hasChildUnits && home.getRentalStructure() != HomeRentalStructure.MULTI_UNITS_HOME) {
            home.setRentalStructure(HomeRentalStructure.MULTI_UNITS_HOME);
        }

        HomeRentalStructure structure = Optional.ofNullable(home.getRentalStructure())
                .orElse(HomeRentalStructure.WHOLE_HOME);

        String currentCode = statusCode(home.getStatus());
        if (isHomeRefreshProtected(home.getStatus())) {
            homeRepository.save(home);
            return;
        }

        if (structure == HomeRentalStructure.WHOLE_HOME) {
            if (isAutoManagedHomeStatus(currentCode)) {
                home.setStatus(requiredHomeStatus("AVAILABLE"));
            }
            homeRepository.save(home);
            return;
        }

        UnitStatusCountsProjection directRoomCounts =
                roomRepository.getDirectHomeRoomStatusCounts(homeId);
        UnitStatusCountsProjection flatCounts =
                flatsRepository.getHomeFlatStatusCounts(homeId);

        long total = safeCount(directRoomCounts == null ? null : directRoomCounts.getTotal())
                + safeCount(flatCounts == null ? null : flatCounts.getTotal());
        long available = safeCount(directRoomCounts == null ? null : directRoomCounts.getAvailable())
                + safeCount(flatCounts == null ? null : flatCounts.getAvailable());
        long rented = safeCount(directRoomCounts == null ? null : directRoomCounts.getRented())
                + safeCount(flatCounts == null ? null : flatCounts.getRented());

        // The Home entity itself represents the remaining Home offer while at least
        // one declared direct Home room is not assigned as an independent Room.
        if (home.getNumberOfRooms() != null && home.getNumberOfRooms() > directRooms) {
            total++;
            available++;
        }

        String targetCode;
        if (total == 0 || available == total) {
            targetCode = "AVAILABLE_AS_MULTI_UNITS";
        } else if (rented == total) {
            targetCode = "FULLY_RENTED_AS_MULTI_UNITS";
        } else {
            targetCode = "PARTIALLY_RENTED_AS_MULTI_UNITS";
        }

        home.setStatus(requiredHomeStatus(targetCode));
        homeRepository.save(home);
    }

    @Transactional(readOnly = true)
    public HomeRentalStructure calculateHomeStructure(Long homeId) {
        if (homeId == null || homeId <= 0) {
            throw new IllegalArgumentException("Home id is required.");
        }

        long directRooms = roomRepository.countActiveDirectRoomsByHomeId(homeId, null);
        long flats = flatsRepository.countByHomeIdAndActiveTrueAndDeletedFalse(homeId);

        if (directRooms > 0 || flats > 0) {
            return HomeRentalStructure.MULTI_UNITS_HOME;
        }

        EntityHomes home = homeRepository.findDetailsById(homeId);
        return home == null || home.getRentalStructure() == null
                ? HomeRentalStructure.WHOLE_HOME
                : home.getRentalStructure();
    }



    @Transactional(readOnly = true)
    public long calculateRemainingDirectRoomCapacity(EntityHomes home) {
        if (home == null || home.getId() == null) {
            return 0L;
        }

        long assigned = roomRepository.countActiveDirectRoomsByHomeId(home.getId(), null);
        int declared = home.getNumberOfRooms() == null ? 0 : home.getNumberOfRooms();
        return Math.max(0L, declared - assigned);
    }

    private boolean isProtectedHomeStatus(String code) {
        if (code == null) return false;
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        return Set.of("DRAFT", "INACTIVE", "BLOCKED", "UNDER_MAINTENANCE")
                .contains(normalized);
    }

    private boolean isAutoManagedHomeStatus(String code) {
        return code != null && AUTO_MANAGED_HOME_STATUSES
                .contains(code.trim().toUpperCase(Locale.ROOT));
    }

    private EntityUnitStatus requiredHomeStatus(String code) {
        return unitStatusService.getRequiredStatus(StatusDomain.HOME, code);
    }


    private long safeCount(Long value) {
        return value == null ? 0L : Math.max(0L, value);
    }



}
