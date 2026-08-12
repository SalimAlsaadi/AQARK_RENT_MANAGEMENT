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
import com.AQARK.AQARK_RENT_MANAGEMENT.Exceptions.BusinessConflictException;
import com.AQARK.AQARK_RENT_MANAGEMENT.Exceptions.FieldValidationException;
import com.AQARK.AQARK_RENT_MANAGEMENT.Repositories.*;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface.HomeServiceInterface;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface.UnitStatusServiceInterface;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HomeServiceImpl implements HomeServiceInterface {


    private static final String AVAILABLE = "AVAILABLE";
    private static final String RENTED = "RENTED";
    private static final String RESERVED = "RESERVED";
    private static final String INACTIVE = "INACTIVE";
    private static final String DRAFT = "DRAFT";
    private static final String BLOCKED = "BLOCKED";
    private static final String UNDER_MAINTENANCE = "UNDER_MAINTENANCE";
    private static final String AVAILABLE_AS_MULTI_UNITS = "AVAILABLE_AS_MULTI_UNITS";
    private static final String PARTIALLY_RENTED_AS_MULTI_UNITS = "PARTIALLY_RENTED_AS_MULTI_UNITS";
    private static final String FULLY_RENTED_AS_MULTI_UNITS = "FULLY_RENTED_AS_MULTI_UNITS";


    /*
     * Keep legacy automatically-generated statuses because old data may
     * still contain them.
     */
    private static final Set<String> AUTO_MANAGED_HOME_STATUSES = Set.of(
            AVAILABLE_AS_MULTI_UNITS,
            PARTIALLY_RENTED_AS_MULTI_UNITS,
            FULLY_RENTED_AS_MULTI_UNITS,

            "AVAILABLE_AS_ROOMS",
            "PARTIALLY_RENTED_AS_ROOMS",
            "FULLY_RENTED_AS_ROOMS",

            "AVAILABLE_AS_FLATS",
            "PARTIALLY_RENTED_AS_FLATS",
            "FULLY_RENTED_AS_FLATS",

            "AVAILABLE_AS_FLAT_AND_ROOMS"
    );


    /*
     * These statuses explicitly override child inventory calculations.
     *
     * Example:
     * Home manually BLOCKED -> Rooms must not make Home AVAILABLE.
     */
    private static final Set<String> PROTECTED_HOME_STATUSES = Set.of(
            RENTED,
            RESERVED,
            INACTIVE,
            DRAFT,
            BLOCKED,
            UNDER_MAINTENANCE
    );


    /*
     * A Flat with one of these statuses is no longer one whole rentable
     * Flat. Its Rooms become the rentable inventory instead.
     */
    private static final Set<String> FLAT_DIVIDED_INTO_ROOMS = Set.of(
            "AVAILABLE_AS_ROOMS",
            "PARTIALLY_RENTED_AS_ROOMS",
            "FULLY_RENTED_AS_ROOMS"
    );


    // =========================================================
    // DEPENDENCIES
    // =========================================================

    private final HomeRepository homeRepository;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final FlatsRepository flatsRepository;

    private final OmanRegionRepository regionRepository;
    private final OmanWilayaRepository wilayaRepository;
    private final OmanAreaRepository areaRepository;

    private final UnitStatusRepository unitStatusRepository;
    private final UnitStatusServiceInterface unitStatusService;

    private final PropertyMediaService propertyMediaService;


    // =========================================================
    // CREATE
    // =========================================================

    @Override
    @Transactional
    public HomeDetailsDTO createHome(
            HomeCreateRequestDTO dto,
            List<MultipartFile> pictures,
            Long authenticatedLandlordId
    ) throws IOException {

        validateAuthenticatedLandlord(authenticatedLandlordId);

        EntityUser landlord = getLandlord(authenticatedLandlordId);

        validateHomeNumberUnique(authenticatedLandlordId, dto.getHomeNumber(), null);

        LocationData location = getValidLocation(dto.getRegionId(), dto.getWilayaId(), dto.getAreaId());


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
                .region(location.region())
                .wilaya(location.wilaya())
                .area(location.area())
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .tenantType(clean(dto.getTenantType()))
                .rentType(clean(dto.getRentTimeType()))
                .price(dto.getPrice())
                .status(initialHomeStatus(dto.getRentalStructure()))
                .landlord(landlord)
                .active(true)
                .deleted(false)
                .build();


        EntityHomes saved = homeRepository.saveAndFlush(home);


        saveImages(saved.getId(), pictures);


        return toDetailsDTO(saved);
    }


    // =========================================================
    // UPDATE
    // =========================================================

    @Override
    @Transactional
    public HomeDetailsDTO updateHome(
            HomeUpdateRequestDTO dto,
            List<MultipartFile> newImages,
            List<Long> imageIdsToDelete,
            Long authenticatedLandlordId
    ) throws IOException {

        if (dto == null) {
            throw fieldError("data", "Home request body is required.");
        }




        EntityHomes home = getOwnedHome(dto.getId(), authenticatedLandlordId);


        if (dto.getNumberOfRooms() != null) {
            validateRoomCountReduction(home, dto.getNumberOfRooms());
        }

        if (dto.getHomeNumber() != null) {
            validateHomeNumberUnique(authenticatedLandlordId, dto.getHomeNumber(), home.getId());
        }


        applyUpdateFields(home, dto);


        EntityHomes saved = homeRepository.saveAndFlush(home);


        propertyMediaService.updateImages(
                PropertyType.HOME,
                saved.getId(),
                imageIdsToDelete,
                newImages
        );


        refreshHomeState(saved.getId());


        return toDetailsDTO(getOwnedHome(saved.getId(), authenticatedLandlordId));
    }


    // =========================================================
    // CHANGE STATUS
    // =========================================================

    @Override
    @Transactional
    public HomeDetailsDTO changeHomeStatus(
            HomeChangeStatusRequestDTO dto,
            Long authenticatedLandlordId
    ) {

        if (dto == null) {
            throw fieldError(
                    "data",
                    "Home status request is required."
            );
        }

        EntityHomes home =
                getOwnedHome(
                        dto.getHomeId(),
                        authenticatedLandlordId
                );


        EntityUnitStatus requestedStatus =
                getManualHomeStatus(
                        dto.getStatusId()
                );


        validateManualStatusTransition(
                home,
                requestedStatus.getCode()
        );


        home.setStatus(requestedStatus);

        home.setActive(
                !INACTIVE.equalsIgnoreCase(
                        requestedStatus.getCode()
                )
        );


        return toDetailsDTO(
                homeRepository.save(home)
        );
    }


    // =========================================================
    // GET
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public HomeDetailsDTO getHomeById(
            Long homeId,
            Long authenticatedLandlordId
    ) {

        return toDetailsDTO(
                getOwnedHome(
                        homeId,
                        authenticatedLandlordId
                )
        );
    }


    @Override
    @Transactional(readOnly = true)
    public List<HomeListDTO> getAllHomes() {

        List<EntityHomes> homes = homeRepository.findAllDetails();

        if (homes.isEmpty()) {
            return List.of();
        }

        Map<Long, HomeUnitCountsProjection> counts = toCountsMap(homeRepository.getAllHomeUnitCounts());

        return homes.stream()
                .map(home -> toListDTO(home, counts.get(home.getId())))
                .toList();
    }


    @Override
    @Transactional(readOnly = true)
    public List<HomeListDTO> getHomesByLandlord(Long landlordId) {

        validateId(landlordId, "landlordId");

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


    // =========================================================
    // DELETE
    // =========================================================

    @Override
    @Transactional
    public void deleteHome(Long homeId, Long authenticatedLandlordId) {

        EntityHomes home = getOwnedHome(homeId, authenticatedLandlordId);

        HomeUnitCountsDTO counts = getUnitCounts(homeId);

        if (counts.getTotalFlats() + counts.getTotalRooms() > 0) {

            throw new BusinessConflictException(
                    "Cannot delete home because it still has active Flats or Rooms. "
                            + "Delete or move those units first."
            );
        }

        propertyMediaService.softDeleteAllImages(PropertyType.HOME, homeId);

        home.setDeleted(true);
        home.setActive(false);
        home.setDeletedAt(LocalDateTime.now());
        home.setStatus(requiredHomeStatus(INACTIVE));


        homeRepository.save(home);
    }


    // =========================================================
    // HOME STATE CALCULATION
    // =========================================================

    /**
     * Central Home calculator.
     *
     * Called after:
     *
     * - direct Room create/update/delete/move
     * - Flat create/update/delete/move
     * - child Room status change
     * - child Flat status change
     * - Home room-capacity change
     *
     * It is intentionally idempotent.
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


        HomeInventory inventory = calculateInventory(home);


        /*
         * Once real child units exist, Home automatically becomes
         * MULTI_UNITS_HOME.
         *
         * We intentionally do not automatically downgrade after the final
         * child is removed because the landlord may have explicitly selected
         * MULTI_UNITS_HOME.
         */
        if (inventory.hasChildUnits() && home.getRentalStructure() != HomeRentalStructure.MULTI_UNITS_HOME) {

            home.setRentalStructure(HomeRentalStructure.MULTI_UNITS_HOME);
        }


        HomeRentalStructure structure =
                Optional.ofNullable(home.getRentalStructure())
                        .orElse(HomeRentalStructure.WHOLE_HOME);


        /*
         * Explicit manual/business statuses override derived inventory.
         *
         * Example:
         * BLOCKED / RENTED / RESERVED must not suddenly become
         * AVAILABLE_AS_MULTI_UNITS because a child Room changes.
         */
        if (isProtectedStatus(home.getStatus())) {

            homeRepository.save(home);

            return;
        }


        /*
         * WHOLE_HOME cannot keep a stale MULTI-UNIT status.
         */
        if (structure == HomeRentalStructure.WHOLE_HOME) {

            if (isAutoManagedStatus(statusCode(home.getStatus()))) {

                home.setStatus(requiredHomeStatus(AVAILABLE));
            }

            homeRepository.save(home);

            return;
        }


        String targetStatus = calculateMultiUnitStatus(inventory);


        if (!targetStatus.equalsIgnoreCase(
                statusCode(home.getStatus())
        )) {

            home.setStatus(
                    requiredHomeStatus(
                            targetStatus
                    )
            );
        }


        homeRepository.save(home);
    }


    // =========================================================
    // SMART INVENTORY CALCULATION
    // =========================================================

    /**
     * Builds the real rentable inventory of the Home.
     *
     * Important:
     *
     * 1. Direct Home Rooms count individually.
     *
     * 2. Normal Flats count as one rentable Flat.
     *
     * 3. Flat divided into Rooms does NOT count again as one Flat.
     *    Its Rooms count individually instead.
     *
     * 4. If some declared direct Home Rooms have not yet been converted
     *    into independent Room entities, the remaining part of the Home
     *    counts as one AVAILABLE offer.
     */
    private HomeInventory calculateInventory(EntityHomes home) {

        Long homeId = home.getId();


        List<EntityRooms> directRooms = roomRepository.findDirectRoomsByHomeId(homeId)
                                                        .stream()
                                                        .filter(EntityRooms::isActive)
                                                        .filter(room -> !room.isDeleted())
                                                        .toList();


        List<EntityFlats> flats = flatsRepository.findDetailsByHomeId(homeId)
                                                    .stream()
                                                    .filter(EntityFlats::isActive)
                                                    .filter(flat -> !flat.isDeleted())
                                                    .toList();


        List<EntityRooms> roomsInsideFlats = roomRepository.findRoomsInsideHomeFlats(homeId)
                                                            .stream()
                                                            .filter(EntityRooms::isActive)
                                                            .filter(room -> !room.isDeleted())
                                                            .toList();


        /*
         * Store the actual rentable-unit statuses.
         */
        List<String> unitStatuses = new ArrayList<>();


        // Direct Rooms are always individual units.
        directRooms.forEach(
                room -> unitStatuses
                                                .add(
                                                        statusCode(
                                                                room.getStatus()
                                                        )
                                                )
        );


        /*
         * Only Rooms whose Flat is actually split into Rooms should be
         * counted here.
         *
         * Defensive filtering prevents accidentally double counting a
         * normal whole Flat plus its child records.
         */
        roomsInsideFlats.stream()
                .filter(room ->
                        room.getFlat() != null
                                && isFlatDividedIntoRooms(
                                statusCode(
                                        room.getFlat().getStatus()
                                )
                        )
                )
                .forEach(room ->
                        unitStatuses.add(
                                statusCode(
                                        room.getStatus()
                                )
                        )
                );


        /*
         * Whole Flats count as a single rentable unit.
         *
         * Flats divided into Rooms do not.
         */
        flats.stream()
                .filter(flat ->
                        !isFlatDividedIntoRooms(
                                statusCode(
                                        flat.getStatus()
                                )
                        )
                )
                .forEach(flat ->
                        unitStatuses.add(
                                statusCode(
                                        flat.getStatus()
                                )
                        )
                );


        long remainingDirectRoomCapacity =
                Math.max(
                        0L,
                        Optional.ofNullable(
                                home.getNumberOfRooms()
                        ).orElse(0)
                                - directRooms.size()
                );


        /*
         * The remaining portion of the Home is represented as one
         * AVAILABLE offer.
         *
         * Example:
         *
         * declared rooms = 5
         * independent direct Room entities = 2
         *
         * remaining Home portion = 1 additional available offer.
         */
        if (remainingDirectRoomCapacity > 0) {

            unitStatuses.add(AVAILABLE);
        }


        long total = unitStatuses.size();


        long available = unitStatuses.stream()
                        .filter(Objects::nonNull)
                        .filter(AVAILABLE::equalsIgnoreCase)
                        .count();


        long rented = unitStatuses.stream()
                        .filter(Objects::nonNull)
                        .filter(RENTED::equalsIgnoreCase)
                        .count();


        return new HomeInventory(
                total,
                available,
                rented,
                directRooms.size(),
                flats.size()
        );
    }


    /**
     * Existing available Home derived statuses only distinguish:
     *
     * AVAILABLE
     * FULLY_RENTED
     * PARTIALLY_RENTED
     *
     * Therefore RESERVED / BLOCKED / MAINTENANCE / unknown mixtures
     * correctly become PARTIALLY_RENTED_AS_MULTI_UNITS.
     */
    private String calculateMultiUnitStatus(HomeInventory inventory) {

        if (inventory.total() == 0
                || inventory.available() == inventory.total()) {

            return AVAILABLE_AS_MULTI_UNITS;
        }


        if (inventory.rented() == inventory.total()) {

            return FULLY_RENTED_AS_MULTI_UNITS;
        }


        return PARTIALLY_RENTED_AS_MULTI_UNITS;
    }


    // =========================================================
    // STRUCTURE / CAPACITY
    // =========================================================

    @Transactional(readOnly = true)
    public HomeRentalStructure calculateHomeStructure(
            Long homeId
    ) {

        validateId(
                homeId,
                "homeId"
        );


        boolean hasChildren =
                roomRepository
                        .countActiveDirectRoomsByHomeId(
                                homeId,
                                null
                        ) > 0

                        || flatsRepository
                        .countByHomeIdAndActiveTrueAndDeletedFalse(
                                homeId
                        ) > 0;


        if (hasChildren) {

            return HomeRentalStructure.MULTI_UNITS_HOME;
        }


        EntityHomes home =
                homeRepository.findDetailsById(homeId);


        if (home == null) {

            throw new EntityNotFoundException(
                    "Home not found with id: "
                            + homeId
            );
        }


        return Optional.ofNullable(
                home.getRentalStructure()
        ).orElse(
                HomeRentalStructure.WHOLE_HOME
        );
    }


    @Transactional(readOnly = true)
    public long calculateRemainingDirectRoomCapacity(
            EntityHomes home
    ) {

        if (home == null || home.getId() == null) {
            return 0L;
        }


        long assigned =
                roomRepository
                        .countActiveDirectRoomsByHomeId(
                                home.getId(),
                                null
                        );


        long declared =
                Optional.ofNullable(
                        home.getNumberOfRooms()
                ).orElse(0);


        return Math.max(
                0L,
                declared - assigned
        );
    }


    private void validateRoomCountReduction(
            EntityHomes home,
            int newCount
    ) {

        if (newCount <= 0) {

            throw new BusinessConflictException(
                    "Home must have at least one declared room."
            );
        }


        long assigned =
                roomRepository
                        .countActiveDirectRoomsByHomeId(
                                home.getId(),
                                null
                        );


        if (newCount < assigned) {

            throw new BusinessConflictException(
                    "Home room count cannot be reduced to "
                            + newCount
                            + " because "
                            + assigned
                            + " active direct Room(s) are already assigned."
            );
        }
    }


    // =========================================================
    // STATUS RULES
    // =========================================================

    /**
     * Explicit statuses win over calculated inventory.
     *
     * AVAILABLE is intentionally NOT protected.
     *
     * Otherwise an AVAILABLE Home would never become
     * AVAILABLE_AS_MULTI_UNITS after the first child is created.
     */
    private boolean isProtectedStatus(EntityUnitStatus status) {

        if (status == null) {
            return false;
        }


        String code = normalizeStatus(status.getCode());


        if (code != null && PROTECTED_HOME_STATUSES.contains(code)) {

            return true;
        }


        /*
         * Lifecycle statuses are always protected.
         */
        if (status.getStatusLevel() == StatusLevel.L) {

            return true;
        }


        /*
         * Future-safe:
         *
         * unknown Unit-level statuses are protected.
         *
         * AVAILABLE and known auto statuses are allowed to recalculate.
         */
        return status.getStatusLevel() == StatusLevel.U
                && !AVAILABLE.equals(code)
                && !isAutoManagedStatus(code);
    }


    private boolean isAutoManagedStatus(String code) {

        String normalized = normalizeStatus(code);

        return normalized != null
                && AUTO_MANAGED_HOME_STATUSES.contains(
                normalized
        );
    }


    private EntityUnitStatus initialHomeStatus(
            HomeRentalStructure structure
    ) {

        if (structure == null) {

            throw fieldError(
                    "rentalStructure",
                    "Home rental structure is required."
            );
        }


        return requiredHomeStatus(
                structure == HomeRentalStructure.MULTI_UNITS_HOME
                        ? AVAILABLE_AS_MULTI_UNITS
                        : AVAILABLE
        );
    }


    /**
     * One method performs all validation needed for a manually selected
     * HOME status.
     */
    private EntityUnitStatus getManualHomeStatus(
            Long statusId
    ) {

        validateId(
                statusId,
                "statusId"
        );


        EntityUnitStatus status =
                unitStatusRepository
                        .findById(statusId)
                        .orElseThrow(() ->
                                new EntityNotFoundException(
                                        "Status not found with id: "
                                                + statusId
                                )
                        );


        if (!status.isActive()) {

            throw new BusinessConflictException(
                    "Selected HOME status is inactive."
            );
        }


        if (!StatusDomain.HOME.name()
                .equalsIgnoreCase(
                        status.getStatusDomain()
                )) {

            throw new BusinessConflictException(
                    "Selected status does not belong to HOME domain."
            );
        }


        if (status.getStatusLevel() != StatusLevel.S) {

            throw new BusinessConflictException(
                    "Only setup-level HOME statuses can be selected manually."
            );
        }


        if (isAutoManagedStatus(
                status.getCode()
        )) {

            throw new BusinessConflictException(
                    "This HOME status is managed automatically from child inventory."
            );
        }


        return status;
    }


    private void validateManualStatusTransition(
            EntityHomes home,
            String requestedCode
    ) {

        String code =
                normalizeStatus(
                        requestedCode
                );


        if (code == null) {
            return;
        }


        /*
         * The remaining Home portion cannot be rented/reserved if all
         * declared direct Room capacity has already been converted into
         * independent Room entities.
         */
        if ((RENTED.equals(code)
                || RESERVED.equals(code))

                && home.getRentalStructure()
                == HomeRentalStructure.MULTI_UNITS_HOME

                && calculateRemainingDirectRoomCapacity(home)
                <= 0) {

            throw new BusinessConflictException(
                    "The remaining Home cannot be rented or reserved because "
                            + "all declared direct Rooms are already assigned "
                            + "as independent Room units."
            );
        }
    }


    private EntityUnitStatus requiredHomeStatus(
            String code
    ) {

        return unitStatusService
                .getRequiredStatus(
                        StatusDomain.HOME,
                        code
                );
    }


    // =========================================================
    // HOME NUMBER
    // =========================================================

    private void validateHomeNumberUnique(
            Long landlordId,
            String homeNumber,
            Long currentHomeId
    ) {

        String cleaned =
                cleanRequired(
                        homeNumber,
                        "Home number is required."
                );


        boolean exists =
                homeRepository
                        .existsByLandlordIdAndHomeNumberIgnoreCaseAndDeletedFalse(
                                landlordId,
                                cleaned
                        );


        if (!exists) {
            return;
        }


        /*
         * During update the repository sees the current Home itself.
         */
        if (currentHomeId != null) {

            EntityHomes current =
                    homeRepository
                            .findDetailsById(
                                    currentHomeId
                            );


            if (current != null
                    && current.getLandlord() != null
                    && Objects.equals(
                    current.getLandlord().getId(),
                    landlordId
            )
                    && cleaned.equalsIgnoreCase(
                    current.getHomeNumber()
            )) {

                return;
            }
        }


        throw new BusinessConflictException(
                "Home number already exists for this landlord."
        );
    }


    // =========================================================
    // LOCATION
    // =========================================================

    private LocationData getValidLocation(Long regionId, Long wilayaId, Long areaId) {

        validateId(regionId, "regionId");
        validateId(wilayaId, "wilayaId");
        validateId(areaId, "areaId");


        OmanRegionEntity region = regionRepository.findById(regionId)
                                                  .orElseThrow(() -> fieldError("regionId", "Region does not exist."));


        OmanWilayaEntity wilaya = wilayaRepository.findByIdAndRegionId(wilayaId, regionId);


        if (wilaya == null) {

            throw fieldError("wilayaId", "Wilaya does not exist or does not belong to the selected region.");
        }


        OmanAreaEntity area = areaRepository.findByIdAndWilayaId(areaId, wilayaId);


        if (area == null) {

            throw fieldError("areaId", "Area does not exist or does not belong to the selected wilaya.");
        }


        return new LocationData(region, wilaya, area);
    }


    // =========================================================
    // UPDATE FIELDS
    // =========================================================

    private void applyUpdateFields(EntityHomes home, HomeUpdateRequestDTO dto) {

        if (dto.getHomeType() != null) {
            home.setHomeType(dto.getHomeType());
        }

        if (dto.getHomeNumber() != null) {
            home.setHomeNumber(
                    cleanRequired(
                            dto.getHomeNumber(),
                            "Home number is required."
                    )
            );
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
            home.setDescription(clean(dto.getDescription()));
        }


        updateLocationIfRequested(home, dto);


        if (dto.getLatitude() != null) {
            home.setLatitude(
                    dto.getLatitude()
            );
        }


        if (dto.getLongitude() != null) {
            home.setLongitude(
                    dto.getLongitude()
            );
        }


        if (dto.getTenantType() != null) {
            home.setTenantType(
                    clean(dto.getTenantType())
            );
        }


        if (dto.getRentType() != null) {
            home.setRentType(
                    clean(dto.getRentType())
            );
        }


        if (dto.getPrice() != null) {
            home.setPrice(
                    dto.getPrice()
            );
        }
    }


    private void updateLocationIfRequested(EntityHomes home, HomeUpdateRequestDTO dto) {

        if (dto.getRegionId() == null && dto.getWilayaId() == null && dto.getAreaId() == null) {
            return;
        }


        Long regionId =
                dto.getRegionId() != null
                        ? dto.getRegionId()
                        : idOf(home.getRegion());


        Long wilayaId =
                dto.getWilayaId() != null
                        ? dto.getWilayaId()
                        : idOf(home.getWilaya());


        Long areaId =
                dto.getAreaId() != null
                        ? dto.getAreaId()
                        : idOf(home.getArea());


        LocationData location = getValidLocation(regionId, wilayaId, areaId);


        home.setRegion(location.region());

        home.setWilaya(location.wilaya());

        home.setArea(location.area());
    }


    // =========================================================
    // ENTITY LOOKUPS / OWNERSHIP
    // =========================================================

    private EntityHomes getOwnedHome(Long homeId, Long landlordId) {

        validateId(homeId, "homeId");

        validateAuthenticatedLandlord(landlordId);


        EntityHomes home = homeRepository.findDetailsById(homeId);


        if (home == null) {

            throw new EntityNotFoundException("Home not found with id: " + homeId);
        }


        if (home.getLandlord() == null || !Objects.equals(home.getLandlord().getId(), landlordId)) {

            throw new AccessDeniedException("You are not allowed to access this Home.");
        }


        return home;
    }


    private EntityUser getLandlord(Long landlordId) {

        return userRepository.findById(landlordId).orElseThrow(() -> new EntityNotFoundException("Landlord not found with id: " + landlordId));
    }


    // =========================================================
    // COUNTS
    // =========================================================

    private HomeUnitCountsDTO getUnitCounts(Long homeId) {

        validateId(homeId, "homeId");

        return toCountsDTO(homeRepository.getHomeUnitCounts(homeId)
        );
    }


    private HomeUnitCountsDTO toCountsDTO(HomeUnitCountsProjection projection) {

        if (projection == null) {
            return emptyCounts();
        }


        long directRooms = zero(projection.getDirectRooms());
        long roomsInFlat = zero(projection.getRoomsInFlat());

        long totalRooms = directRooms + roomsInFlat;

        long flatsAvailable = zero(projection.getFlatsAvailable());

        long flatsRented = zero(projection.getFlatsRented());

        long flatsReserved = zero(projection.getFlatsReserved());

        long flatsMaintenance = zero(projection.getFlatsUnderMaintenance());

        long flatsBlocked = zero(projection.getFlatsBlocked());

        long roomsAvailable = zero(projection.getRoomsAvailable());

        long roomsRented = zero(projection.getRoomsRented());

        long roomsReserved = zero(projection.getRoomsReserved());

        long roomsMaintenance = zero(projection.getRoomsUnderMaintenance());

        long roomsBlocked = zero(projection.getRoomsBlocked());

        return HomeUnitCountsDTO.builder()

                .totalFlats(zero(projection.getTotalFlats()))
                .flatsAvailable(flatsAvailable)
                .flatsRented(flatsRented)
                .flatsRentedAsRooms(zero(projection.getFlatsRentedAsRooms()))
                .flatsReserved(flatsReserved)
                .flatsUnderMaintenance(flatsMaintenance)
                .flatsBlocked(flatsBlocked)
                .totalRooms(totalRooms)
                .directRooms(directRooms)
                .roomsInFlat(roomsInFlat)
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


    private Map<Long, HomeUnitCountsProjection> toCountsMap(List<HomeUnitCountsProjection> projections) {

        if (projections == null || projections.isEmpty()) {
            return Map.of();
        }


        return projections.stream()

                .filter(projection ->
                        projection.getHomeId() != null
                )

                .collect(
                        Collectors.toMap(
                                HomeUnitCountsProjection::getHomeId,
                                Function.identity(),
                                (first, ignored) -> first,
                                LinkedHashMap::new
                        )
                );
    }


    // =========================================================
    // DETAILS DTO
    // =========================================================

    private HomeDetailsDTO toDetailsDTO(
            EntityHomes home
    ) {

        EntityUser landlord =
                home.getLandlord();


        return HomeDetailsDTO.builder()

                .id(home.getId())

                .homeType(
                        home.getHomeType()
                )

                .rentalStructure(
                        home.getRentalStructure()
                )

                .homeNumber(
                        home.getHomeNumber()
                )

                .numberOfRooms(
                        home.getNumberOfRooms()
                )

                .numberOfBathrooms(
                        home.getNumberOfBathrooms()
                )

                .hasLivingRoom(
                        home.isHasLivingRoom()
                )

                .furnished(
                        home.isFurnished()
                )

                .numberOfFloor(
                        home.getNumberOfFloor()
                )

                .description(
                        home.getDescription()
                )

                .location(
                        toLocationDTO(home)
                )

                .tenantType(
                        home.getTenantType()
                )

                .rentType(
                        home.getRentType()
                )

                .price(
                        home.getPrice()
                )

                .status(
                        toStatusDTO(
                                home.getStatus()
                        )
                )

                .unitCounts(
                        getUnitCounts(
                                home.getId()
                        )
                )

                .landlordId(
                        landlord != null
                                ? landlord.getId()
                                : null
                )

                .landlordName(
                        landlordName(
                                landlord
                        )
                )

                .landlordMobileNumber(
                        landlord != null
                                ? landlord.getPhoneNumber()
                                : null
                )

                .active(
                        home.isActive()
                )

                .deleted(
                        home.isDeleted()
                )

                .createdAt(
                        home.getCreatedAt()
                )

                .updatedAt(
                        home.getUpdatedAt()
                )

                .images(
                        getImages(
                                home.getId()
                        )
                )

                .build();
    }


    // =========================================================
    // LIST DTO
    // =========================================================

    private HomeListDTO toListDTO(
            EntityHomes home,
            HomeUnitCountsProjection projection
    ) {

        HomeUnitCountsDTO counts =
                toCountsDTO(projection);

        EntityUnitStatus status =
                home.getStatus();


        return HomeListDTO.builder()

                .id(home.getId())

                .homeType(
                        home.getHomeType()
                )

                .homeNumber(
                        home.getHomeNumber()
                )

                .rentalStructure(
                        home.getRentalStructure()
                )

                .region(
                        home.getRegion() != null
                                ? home.getRegion().getNameEn()
                                : null
                )

                .wilaya(
                        home.getWilaya() != null
                                ? home.getWilaya().getNameEn()
                                : null
                )

                .area(
                        home.getArea() != null
                                ? home.getArea().getNameEn()
                                : null
                )

                .statusCode(
                        statusCode(status)
                )

                .statusNameEn(
                        status != null
                                ? status.getNameEn()
                                : null
                )

                .statusNameAr(
                        status != null
                                ? status.getNameAr()
                                : null
                )

                .statusColorCode(
                        status != null
                                ? status.getColorCode()
                                : null
                )

                .statusIcon(
                        status != null
                                ? status.getIcon()
                                : null
                )

                .numberOfFloors(
                        home.getNumberOfFloor()
                )

                .declaredNumberOfRooms(
                        home.getNumberOfRooms()
                )

                .numberOfBathrooms(
                        home.getNumberOfBathrooms()
                )

                .price(
                        home.getPrice()
                )

                .rentType(
                        home.getRentType()
                )

                .totalFlats(
                        counts.getTotalFlats()
                )

                .totalRooms(
                        counts.getTotalRooms()
                )

                .availableUnits(
                        counts.getAvailableUnits()
                )

                .rentedUnits(
                        counts.getRentedUnits()
                )

                .maintenanceUnits(
                        counts.getMaintenanceUnits()
                )

                .mainImageUrls(
                        getPictureUrls(
                                home.getId()
                        )
                )

                .build();
    }


    // =========================================================
    // SMALL DTO MAPPERS
    // =========================================================

    private UnitStatusDTO toStatusDTO(
            EntityUnitStatus status
    ) {

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

        OmanRegionEntity region = home.getRegion();
        OmanWilayaEntity wilaya = home.getWilaya();
        OmanAreaEntity area = home.getArea();


        return LocationDTO.builder()

                .regionId(idOf(region))
                .regionNameEn(region != null ? region.getNameEn() : null)
                .regionNameAr(region != null ? region.getNameAr() : null)
                .wilayaId(idOf(wilaya))
                .wilayaNameEn(wilaya != null ? wilaya.getNameEn() : null)
                .wilayaNameAr(wilaya != null ? wilaya.getNameAr() : null)
                .areaId(idOf(area))
                .areaNameEn(area != null ? area.getNameEn() : null)
                .areaNameAr(area != null ? area.getNameAr() : null)
                .latitude(home.getLatitude())
                .longitude(home.getLongitude())
                .build();
    }


    // =========================================================
    // MEDIA
    // =========================================================

    private void saveImages(Long homeId, List<MultipartFile> pictures) throws IOException {

        if (pictures == null || pictures.isEmpty()) {
            return;
        }


        propertyMediaService.saveImages(
                pictures,
                PropertyType.HOME,
                homeId
        );
    }


    private List<PropertyMediaDTO> getImages(Long homeId) {

        List<PropertyMediaDTO> images = propertyMediaService.getImages(PropertyType.HOME, homeId);


        return images == null ? List.of()
                      : images;
    }


    private List<String> getPictureUrls(Long homeId) {

        return getImages(homeId)
                .stream()
                .map(PropertyMediaDTO::getFileUrl)
                .toList();
    }


    // =========================================================
    // COMMON UTILITIES
    // =========================================================

    private boolean isFlatDividedIntoRooms(String code) {

        String normalized = normalizeStatus(code);


        return normalized != null && FLAT_DIVIDED_INTO_ROOMS.contains(normalized);
    }


    private String statusCode(EntityUnitStatus status) {

        return status == null ? null
                      : status.getCode();
    }


    private String normalizeStatus(String value) {

        String cleaned = clean(value);


        return cleaned == null ? null
                        : cleaned.toUpperCase(
                        Locale.ROOT
        );
    }


    private String clean(String value) {

        if (value == null) {
            return null;
        }


        String cleaned = value.trim();


        return cleaned.isEmpty()
                ? null
                : cleaned;
    }


    private String cleanRequired(String value, String message) {

        String cleaned = clean(value);


        if (cleaned == null) {

            throw new FieldValidationException(message, Map.of("value", message));
        }


        return cleaned;
    }


    private void validateAuthenticatedLandlord(Long landlordId) {

        if (landlordId == null || landlordId <= 0) {

            throw new AccessDeniedException("Authenticated landlord id is required.");
        }
    }


    private void validateId(Long id, String field) {

        if (id == null || id <= 0) {

            throw fieldError(field, field + " is required and must be greater than zero.");
        }
    }


    private long zero(Long value) {

        return value == null
                ? 0L
                : Math.max(
                value,
                0L
        );
    }


    private String landlordName(
            EntityUser landlord
    ) {

        if (landlord == null) {
            return null;
        }


        String first =
                clean(
                        landlord.getFirstName()
                );

        String last =
                clean(
                        landlord.getLastName()
                );


        return (
                (first == null ? "" : first)
                        + " "
                        + (last == null ? "" : last)
        ).trim();
    }


    private FieldValidationException fieldError(String field, String message) {

        return new FieldValidationException(message, Map.of(field, message));
    }


    private Long idOf(OmanRegionEntity entity) {

        return entity == null
                ? null
                : entity.getId();
    }


    private Long idOf(OmanWilayaEntity entity) {

        return entity == null
                ? null
                : entity.getId();
    }


    private Long idOf(OmanAreaEntity entity) {

        return entity == null
                ? null
                : entity.getId();
    }


    // =========================================================
    // SMALL INTERNAL RECORDS
    // =========================================================

    /**
     * Same service class; no additional Java file is needed.
     */
    private record LocationData(
            OmanRegionEntity region,
            OmanWilayaEntity wilaya,
            OmanAreaEntity area
    ) {}


    /**
     * Real rentable inventory snapshot.
     */
    private record HomeInventory(
            long total,
            long available,
            long rented,
            long directRooms,
            long flats
    ) {

        boolean hasChildUnits() {

            return directRooms > 0 || flats > 0;
        }
    }
}