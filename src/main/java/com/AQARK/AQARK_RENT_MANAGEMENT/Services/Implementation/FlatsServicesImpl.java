package com.AQARK.AQARK_RENT_MANAGEMENT.Services.Implementation;

import com.AQARK.AQARK_RENT_MANAGEMENT.Common_Utilities.PropertyMediaService;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.FlatDTO.*;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.MediaDTO.PropertyMediaDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.UnitStatusCountsProjection;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.*;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Enums.PropertyType;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Enums.StatusDomain;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Enums.StatusLevel;
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
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class FlatsServicesImpl implements FlatsServicesInterface {



    private static final String AVAILABLE = "AVAILABLE";
    private static final String RENTED = "RENTED";
    private static final String RESERVED = "RESERVED";
    private static final String UNDER_MAINTENANCE = "UNDER_MAINTENANCE";
    private static final String BLOCKED = "BLOCKED";

    private static final String AVAILABLE_AS_ROOMS = "AVAILABLE_AS_ROOMS";
    private static final String PARTIALLY_RENTED_AS_ROOMS = "PARTIALLY_RENTED_AS_ROOMS";
    private static final String FULLY_RENTED_AS_ROOMS = "FULLY_RENTED_AS_ROOMS";




    private final FlatsRepository flatsRepository;
    private final UserRepository userRepository;
    private final BuildingRepository buildingRepository;
    private final HomeRepository homeRepository;
    private final RoomRepository roomRepository;

    private final PropertyMediaService propertyMediaService;
    private final UnitStatusServiceInterface unitStatusService;

    private final HomeServiceImpl homeService;



    @Override
    @Transactional
    public FlatDetailsDTO createFlat(
            FlatSaveRequestDTO dto,
            List<MultipartFile> pictures,
            Long landlordId
    ) throws IOException {

        validateId(landlordId, "landlordId");
        validateParent(dto.getBuildingId(), dto.getHomeId());

        EntityUser landlord = getLandlord(landlordId);

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
                .active(true)
                .deleted(false)
                .build();

        assignParent(
                flat,
                dto.getBuildingId(),
                dto.getHomeId(),
                landlordId
        );

        EntityFlats savedFlat = flatsRepository.saveAndFlush(flat);

        saveImages(savedFlat.getId(), pictures);

        refreshAffectedHomes(null, homeIdOf(savedFlat));

        return toDetailsDTO(savedFlat);
    }


    @Override
    @Transactional
    public FlatDetailsDTO updateFlat(
            Long flatId,
            FlatUpdateRequestDTO dto,
            List<MultipartFile> newImages,
            List<Long> imageIdsToDelete
    ) throws IOException {

        validateId(flatId, "flatId");

        if (dto == null) {
            throw fieldError("data", "Flat request body is required.");
        }

        EntityFlats flat = getFlat(flatId);

        Long landlordId = flat.getLandlord() == null ? null
                        : flat.getLandlord().getId();

        if (landlordId == null) {
            throw new AccessDeniedException("Flat does not have a valid landlord.");
        }

        Long oldHomeId = homeIdOf(flat);


        validateRoomCountChange(flat, dto.getNumberOfRooms());


        Long targetBuildingId;
        Long targetHomeId;

        boolean parentProvided = dto.getBuildingId() != null || dto.getHomeId() != null;

        if (parentProvided) {

            validateParent(dto.getBuildingId(), dto.getHomeId());

            targetBuildingId = dto.getBuildingId();

            targetHomeId = dto.getHomeId();

        } else {

            targetBuildingId = flat.getBuilding() == null ? null
                            : flat.getBuilding().getId();

            targetHomeId = flat.getHome() == null ? null
                            : flat.getHome().getId();

            validateParent(targetBuildingId, targetHomeId);
        }



        applyUpdateFields(flat, dto);



        assignParent(
                flat,
                targetBuildingId,
                targetHomeId,
                landlordId
        );


        EntityFlats savedFlat = flatsRepository.saveAndFlush(flat);


        propertyMediaService.updateImages(
                PropertyType.FLAT,
                savedFlat.getId(),
                imageIdsToDelete,
                newImages
        );


        refreshAffectedHomes(oldHomeId, homeIdOf(savedFlat));


        return toDetailsDTO(savedFlat);
    }


    @Override
    public FlatDetailsDTO getFlatById(Long flatId) {

        validateId(flatId, "flatId");

        return toDetailsDTO(getFlat(flatId));
    }


    @Override
    public List<FlatDetailsDTO> getFlatsByBuildingId(Long buildingId) {

        validateId(buildingId, "buildingId");

        return flatsRepository
                .findDetailsByBuildingId(buildingId)
                .stream()
                .map(this::toDetailsDTO)
                .toList();
    }


    @Override
    public List<FlatDetailsDTO> getFlatsByHomeId(Long homeId) {

        validateId(homeId, "homeId");

        return flatsRepository
                .findDetailsByHomeId(homeId)
                .stream()
                .map(this::toDetailsDTO)
                .toList();
    }


    @Override
    public List<FlatListDTO> getFlatListByHomeId(Long homeId) {

        validateId(homeId, "homeId");

        return flatsRepository
                .findDetailsByHomeId(homeId)
                .stream()
                .map(this::toListDTO)
                .toList();
    }


    @Override
    public List<FlatDetailsDTO> getFlatsByLandlordId(Long landlordId) {

        validateId(landlordId, "landlordId");

        return flatsRepository
                .findDetailsByLandlordId(landlordId)
                .stream()
                .map(this::toDetailsDTO)
                .toList();
    }


    @Override
    public List<FlatListDTO> getFlatListByBuildingId(Long buildingId) {

        validateId(buildingId, "buildingId");

        return flatsRepository
                .findDetailsByBuildingId(buildingId)
                .stream()
                .map(this::toListDTO)
                .toList();
    }


    @Override
    public List<FlatDetailsDTO> searchFlats(FlatSearchRequestDTO filter) {

        FlatSearchRequestDTO safeFilter =
                filter == null ? new FlatSearchRequestDTO()
                        : filter;

        return flatsRepository
                .searchFlats(safeFilter)
                .stream()
                .map(this::toDetailsDTO)
                .toList();
    }



    @Override
    @Transactional
    public void deleteFlat(Long flatId) {

        validateId(flatId, "flatId");

        EntityFlats flat = getFlat(flatId);


        long activeRooms = roomRepository.countActiveRoomsByFlatId(flat.getId(), null);

        if (activeRooms > 0) {

            throw new BusinessConflictException(
                    "Cannot delete flat because it has "
                            + activeRooms
                            + " active Room(s). "
                            + "Delete or move the rooms first."
            );
        }

        Long oldHomeId = homeIdOf(flat);

        propertyMediaService.softDeleteAllImages(PropertyType.FLAT, flat.getId());

        flat.setDeleted(true);
        flat.setActive(false);
        flat.setDeletedAt(LocalDateTime.now());

        flatsRepository.saveAndFlush(flat);

        refreshAffectedHomes(oldHomeId, null);
    }



    private void validateParent(Long buildingId, Long homeId) {

        boolean hasBuilding = buildingId != null;

        boolean hasHome = homeId != null;

        if (hasBuilding == hasHome) {

            throw fieldError("parent", "Flat must belong to exactly one Building or one Home.");
        }

        if (hasBuilding) {
            validateId(buildingId, "buildingId");
        }

        if (hasHome) {
            validateId(homeId, "homeId");
        }
    }


    private void assignParent(
            EntityFlats flat,
            Long buildingId,
            Long homeId,
            Long landlordId
    ) {

        String flatNumber = cleanRequired(flat.getFlatNumber(), "Flat number is required.");

        if (buildingId != null) {

            EntityBuildings building = getBuilding(buildingId);

            validateOwnership(
                    building.getLandlord(),
                    landlordId,
                    "building"
            );

            validateParentAcceptsFlats(
                    building.getStatus(),
                    "building"
            );

            validateFlatNumberUnique(
                    buildingId,
                    null,
                    flatNumber,
                    flat.getId()
            );

            flat.setBuilding(building);
            flat.setHome(null);

        } else {

            EntityHomes home = getHomeForUpdate(homeId);

            validateOwnership(
                    home.getLandlord(),
                    landlordId,
                    "home"
            );

            validateParentAcceptsFlats(
                    home.getStatus(),
                    "home"
            );

            validateFlatNumberUnique(
                    null,
                    homeId,
                    flatNumber,
                    flat.getId()
            );

            flat.setHome(home);
            flat.setBuilding(null);
        }

        flat.setFlatNumber(flatNumber);
    }


    private void validateFlatNumberUnique(
            Long buildingId,
            Long homeId,
            String flatNumber,
            Long currentFlatId
    ) {

        String cleaned = cleanRequired(flatNumber, "Flat number is required.");

        boolean exists =
                buildingId != null ? flatsRepository.existsByBuildingIdAndFlatNumberIgnoreCaseAndDeletedFalse(buildingId, cleaned)
                             : flatsRepository.existsByHomeIdAndFlatNumberIgnoreCaseAndDeletedFalse(homeId, cleaned);


        if (!exists) {
            return;
        }


        /*
         * CREATE:
         *
         * currentFlatId == null
         *
         * Therefore any existing value is a duplicate.
         */
        if (currentFlatId == null) {

            throw duplicateFlatNumberException(
                    buildingId
            );
        }


        EntityFlats current =
                flatsRepository.findDetailsById(
                        currentFlatId
                );


        if (current != null
                && cleaned.equalsIgnoreCase(
                current.getFlatNumber()
        )) {

            if (buildingId != null
                    && current.getBuilding() != null
                    && Objects.equals(
                    current.getBuilding().getId(),
                    buildingId
            )) {

                return;
            }


            if (homeId != null
                    && current.getHome() != null
                    && Objects.equals(
                    current.getHome().getId(),
                    homeId
            )) {

                return;
            }
        }


        throw duplicateFlatNumberException(
                buildingId
        );
    }


    private BusinessConflictException duplicateFlatNumberException(
            Long buildingId
    ) {

        return new BusinessConflictException(
                buildingId != null
                        ? "Flat number already exists in this building."
                        : "Flat number already exists in this home."
        );
    }


    // =========================================================
    // ROOM CAPACITY
    // =========================================================

    /**
     * Prevent changing:
     *
     * numberOfRooms = 5
     *
     * to:
     *
     * numberOfRooms = 2
     *
     * when 3 active Room entities already exist.
     */
    private void validateRoomCountChange(
            EntityFlats flat,
            Integer newCount
    ) {

        if (newCount == null || newCount <= 0) {

            throw new BusinessConflictException(
                    "Flat must have at least one declared room."
            );
        }

        long assigned =
                roomRepository.countActiveRoomsByFlatId(
                        flat.getId(),
                        null
                );

        if (newCount < assigned) {

            throw new BusinessConflictException(
                    "Flat room count cannot be reduced to "
                            + newCount
                            + " because "
                            + assigned
                            + " active Room(s) are already assigned."
            );
        }
    }


    /**
     * Public because Room service or other future services may use it
     * before creating/moving a Room.
     */
    @Transactional
    public void validateFlatRoomCapacity(
            Long flatId,
            Long excludedRoomId
    ) {

        validateId(
                flatId,
                "flatId"
        );

        EntityFlats flat =
                getFlat(flatId);

        int allowed =
                Optional.ofNullable(
                        flat.getNumberOfRooms()
                ).orElse(0);

        long assigned =
                roomRepository.countActiveRoomsByFlatId(
                        flatId,
                        excludedRoomId
                );

        if (allowed <= 0 || assigned >= allowed) {

            throw new BusinessConflictException(
                    "Cannot add another room. Flat "
                            + flat.getFlatNumber()
                            + " allows only "
                            + allowed
                            + " room(s)."
            );
        }
    }


    // =========================================================
    // DERIVED FLAT STATUS FROM ROOMS
    // =========================================================

    /**
     * Called whenever one of the Flat's child Rooms:
     *
     * - is created
     * - is deleted
     * - changes status
     * - moves into/out of this Flat
     *
     * Result:
     *
     * 0 Rooms
     *      -> AVAILABLE
     *
     * all AVAILABLE
     *      -> AVAILABLE_AS_ROOMS
     *
     * all RENTED
     *      -> FULLY_RENTED_AS_ROOMS
     *
     * any mixed state
     *      -> PARTIALLY_RENTED_AS_ROOMS
     *
     * Protected whole-Flat states are not overwritten.
     */
    @Transactional
    public void refreshFlatState(Long flatId) {

        if (flatId == null || flatId <= 0) {
            return;
        }

        EntityFlats flat =
                flatsRepository
                        .findByIdForUpdate(flatId)
                        .orElse(null);

        if (flat == null || flat.isDeleted()) {
            return;
        }


        /*
         * Example protected states:
         *
         * RENTED as whole Flat
         * RESERVED as whole Flat
         * BLOCKED
         * UNDER_MAINTENANCE
         *
         * Lifecycle statuses are also protected.
         */
        if (isProtectedFlatStatus(flat.getStatus())) {

            refreshOwningHome(flat);

            return;
        }


        UnitStatusCountsProjection counts =
                roomRepository
                        .getActiveRoomStatusCountsByFlatId(
                                flatId
                        );


        long total =
                safeCount(
                        counts == null
                                ? null
                                : counts.getTotal()
                );


        /*
         * No child Rooms remain.
         *
         * Only remove a derived Room status.
         * Do not overwrite an unrelated Flat state.
         */
        if (total == 0) {

            if (isDerivedRoomStatus(
                    statusCode(flat.getStatus())
            )) {

                setFlatStatus(
                        flat,
                        AVAILABLE
                );
            }

            refreshOwningHome(flat);

            return;
        }


        long available =
                safeCount(
                        counts.getAvailable()
                );

        long rented =
                safeCount(
                        counts.getRented()
                );


        String calculatedStatus =
                calculateStatusFromRooms(
                        total,
                        available,
                        rented
                );


        if (!calculatedStatus.equalsIgnoreCase(
                statusCode(flat.getStatus())
        )) {

            setFlatStatus(
                    flat,
                    calculatedStatus
            );
        }


        refreshOwningHome(flat);
    }


    /**
     * All Room-status calculation is centralized here.
     */
    private String calculateStatusFromRooms(
            long total,
            long available,
            long rented
    ) {

        if (available == total) {
            return AVAILABLE_AS_ROOMS;
        }

        if (rented == total) {
            return FULLY_RENTED_AS_ROOMS;
        }

        /*
         * Includes any mixture of:
         *
         * AVAILABLE
         * RENTED
         * RESERVED
         * UNDER_MAINTENANCE
         * BLOCKED
         *
         * based on the currently available derived Flat statuses.
         */
        return PARTIALLY_RENTED_AS_ROOMS;
    }


    /**
     * Do not allow Room inventory calculation to overwrite an explicit
     * whole-Flat or lifecycle state.
     *
     * IMPORTANT:
     *
     * AVAILABLE is intentionally NOT protected.
     *
     * Otherwise an AVAILABLE Flat would never become
     * AVAILABLE_AS_ROOMS after adding its first Room.
     */
    private boolean isProtectedFlatStatus(
            EntityUnitStatus status
    ) {

        if (status == null) {
            return false;
        }


        if (status.getStatusLevel() == StatusLevel.L) {
            return true;
        }


        String code =
                statusCode(status);


        if (RENTED.equalsIgnoreCase(code)
                || RESERVED.equalsIgnoreCase(code)
                || BLOCKED.equalsIgnoreCase(code)
                || UNDER_MAINTENANCE.equalsIgnoreCase(code)) {

            return true;
        }


        /*
         * Future-safe:
         *
         * Protect unknown Unit-level statuses,
         * but allow AVAILABLE and Room-derived statuses.
         */
        return status.getStatusLevel() == StatusLevel.U
                && !AVAILABLE.equalsIgnoreCase(code)
                && !isDerivedRoomStatus(code);
    }


    private boolean isDerivedRoomStatus(
            String code
    ) {

        if (code == null) {
            return false;
        }

        return AVAILABLE_AS_ROOMS.equalsIgnoreCase(code)
                || PARTIALLY_RENTED_AS_ROOMS.equalsIgnoreCase(code)
                || FULLY_RENTED_AS_ROOMS.equalsIgnoreCase(code);
    }


    private void setFlatStatus(
            EntityFlats flat,
            String code
    ) {

        flat.setStatus(
                unitStatusService.getRequiredStatus(
                        StatusDomain.FLAT,
                        code
                )
        );

        flatsRepository.save(flat);
    }


    // =========================================================
    // PARENT BUSINESS VALIDATION
    // =========================================================

    /**
     * Same operational rule for Building and Home.
     */
    private void validateParentAcceptsFlats(
            EntityUnitStatus status,
            String parentName
    ) {

        String code =
                statusCode(status);

        if (BLOCKED.equalsIgnoreCase(code)
                || UNDER_MAINTENANCE.equalsIgnoreCase(code)) {

            throw new BusinessConflictException(
                    "Cannot add a flat because "
                            + parentName
                            + " status is "
                            + code
                            + "."
            );
        }
    }


    /**
     * One ownership method replaces Building/Home versions.
     */
    private void validateOwnership(
            EntityUser owner,
            Long landlordId,
            String parentName
    ) {

        if (owner == null
                || !Objects.equals(
                owner.getId(),
                landlordId
        )) {

            throw new AccessDeniedException(
                    "You are not allowed to modify a flat in this "
                            + parentName
                            + "."
            );
        }
    }


    // =========================================================
    // UPDATE FIELDS
    // =========================================================

    /**
     * Current Flat update behaves like a full update/PUT:
     * required values are replaced every time.
     */
    private void applyUpdateFields(
            EntityFlats flat,
            FlatUpdateRequestDTO dto
    ) {

        flat.setFlatNumber(
                cleanRequired(
                        dto.getFlatNumber(),
                        "Flat number is required."
                )
        );

        flat.setNumberOfRooms(
                dto.getNumberOfRooms()
        );

        flat.setNumberOfBathrooms(
                cleanRequired(
                        dto.getNumberOfBathrooms(),
                        "Number of bathrooms is required."
                )
        );

        flat.setHasLivingRoom(
                Boolean.TRUE.equals(
                        dto.getHasLivingRoom()
                )
        );

        flat.setHasAC(
                dto.isHasAC()
        );

        flat.setHasWIFI(
                dto.isHasWIFI()
        );

        flat.setFurnished(
                dto.isFurnished()
        );

        flat.setStatus(
                resolveUnitStatus(
                        dto.getStatusCode()
                )
        );

        flat.setFloorNumber(
                cleanRequired(
                        dto.getFloorNumber(),
                        "Floor number is required."
                )
        );

        flat.setDescription(
                clean(dto.getDescription())
        );

        flat.setTenantType(
                clean(dto.getTenantType())
        );

        flat.setRentalType(
                clean(dto.getRentTimeType())
        );

        flat.setPrice(
                dto.getPrice()
        );

        flat.setLandlordMobileNumber(
                clean(dto.getMobileNumber())
        );
    }


    // =========================================================
    // STATUS
    // =========================================================

    private EntityUnitStatus resolveUnitStatus(
            String statusCode
    ) {

        String cleaned =
                clean(statusCode);

        if (cleaned == null) {

            /*
             * Keep this only if Flat status is optional on create/update.
             *
             * If DTO uses @NotBlank(statusCode), you can instead remove
             * this default behavior.
             */
            return unitStatusService
                    .getDefaultStatus(
                            StatusDomain.FLAT
                    );
        }


        return unitStatusService
                .getRequiredSetupStatus(
                        StatusDomain.FLAT,
                        cleaned.toUpperCase(Locale.ROOT)
                );
    }


    private String statusCode(
            EntityUnitStatus status
    ) {

        return status == null
                ? null
                : status.getCode();
    }


    // =========================================================
    // ENTITY LOOKUPS
    // =========================================================

    private EntityFlats getFlat(Long flatId) {

        EntityFlats flat =
                flatsRepository.findDetailsById(
                        flatId
                );

        if (flat == null) {

            throw new EntityNotFoundException(
                    "Flat not found with id: "
                            + flatId
            );
        }

        return flat;
    }


    private EntityBuildings getBuilding(
            Long buildingId
    ) {

        validateId(
                buildingId,
                "buildingId"
        );

        return buildingRepository
                .findById(buildingId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Building not found with id: "
                                        + buildingId
                        )
                );
    }


    private EntityHomes getHomeForUpdate(
            Long homeId
    ) {

        validateId(
                homeId,
                "homeId"
        );

        return homeRepository
                .findByIdForUpdate(homeId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Home not found with id: "
                                        + homeId
                        )
                );
    }


    private EntityUser getLandlord(
            Long landlordId
    ) {

        return userRepository
                .findById(landlordId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Landlord not found with id: "
                                        + landlordId
                        )
                );
    }


    // =========================================================
    // HOME REFRESH
    // =========================================================

    private void refreshAffectedHomes(
            Long oldHomeId,
            Long newHomeId
    ) {

        Set<Long> homeIds =
                new LinkedHashSet<>();

        addValidId(
                homeIds,
                oldHomeId
        );

        addValidId(
                homeIds,
                newHomeId
        );

        homeIds.forEach(
                homeService::refreshHomeState
        );
    }


    private void refreshOwningHome(
            EntityFlats flat
    ) {

        if (flat != null
                && flat.getHome() != null) {

            homeService.refreshHomeState(
                    flat.getHome().getId()
            );
        }
    }


    // =========================================================
    // MEDIA
    // =========================================================

    private void saveImages(
            Long flatId,
            List<MultipartFile> pictures
    ) throws IOException {

        if (pictures == null
                || pictures.isEmpty()) {

            return;
        }

        propertyMediaService.saveImages(
                pictures,
                PropertyType.FLAT,
                flatId
        );
    }


    // =========================================================
    // DETAILS DTO
    // =========================================================

    /**
     * Resolve parent/location ONCE.
     *
     * The old implementation repeatedly called:
     *
     * resolveRegion(flat)
     * resolveWilaya(flat)
     * resolveArea(flat)
     * resolveLatitude(flat)
     * resolveLongitude(flat)
     * resolveHasParking(flat)
     * resolveHasGym(flat)
     * resolveHasSwimmingPool(flat)
     *
     * This version removes all of those methods.
     */
    private FlatDetailsDTO toDetailsDTO(
            EntityFlats flat
    ) {

        EntityBuildings building =
                flat.getBuilding();

        EntityHomes home =
                flat.getHome();

        EntityUser landlord =
                flat.getLandlord();


        OmanRegionEntity region =
                home != null
                        ? home.getRegion()
                        : building != null
                        ? building.getRegion()
                        : null;


        OmanWilayaEntity wilaya =
                home != null
                        ? home.getWilaya()
                        : building != null
                        ? building.getWilaya()
                        : null;


        OmanAreaEntity area =
                home != null
                        ? home.getArea()
                        : building != null
                        ? building.getArea()
                        : null;


        Double latitude =
                home != null
                        ? home.getLatitude()
                        : building != null
                        ? building.getLatitude()
                        : null;


        Double longitude =
                home != null
                        ? home.getLongitude()
                        : building != null
                        ? building.getLongitude()
                        : null;


        Boolean hasParking =
                building != null
                        ? building.isHasParking()
                        : home != null
                        ? true
                        : null;


        Boolean hasGym =
                building != null
                        ? building.isHasGym()
                        : home != null
                        ? false
                        : null;


        Boolean hasSwimmingPool =
                building != null
                        ? building.isHasSwimmingPool()
                        : home != null
                        ? false
                        : null;


        List<String> imageUrls =
                propertyMediaService
                        .getImages(
                                PropertyType.FLAT,
                                flat.getId()
                        )
                        .stream()
                        .map(PropertyMediaDTO::getFileUrl)
                        .toList();


        return FlatDetailsDTO.builder()

                .id(flat.getId())

                .flatNumber(
                        flat.getFlatNumber()
                )

                .numberOfRooms(
                        flat.getNumberOfRooms()
                )

                .numberOfBathrooms(
                        flat.getNumberOfBathrooms()
                )

                .hasLivingRoom(
                        flat.isHasLivingRoom()
                )

                .hasAC(
                        flat.isHasAC()
                )

                .hasWIFI(
                        flat.isHasWIFI()
                )

                .isFurnished(
                        flat.isFurnished()
                )

                .status(
                        toStatusDTO(
                                flat.getStatus()
                        )
                )

                .floorNumber(
                        flat.getFloorNumber()
                )

                .description(
                        flat.getDescription()
                )


                // ---------------------------------------------
                // LOCATION
                // ---------------------------------------------

                .regionId(
                        region != null
                                ? region.getId()
                                : null
                )

                .region(
                        region != null
                                ? region.getNameEn()
                                : null
                )

                .regionAr(
                        region != null
                                ? region.getNameAr()
                                : null
                )

                .wilayaId(
                        wilaya != null
                                ? wilaya.getId()
                                : null
                )

                .wilaya(
                        wilaya != null
                                ? wilaya.getNameEn()
                                : null
                )

                .wilayaAr(
                        wilaya != null
                                ? wilaya.getNameAr()
                                : null
                )

                .areaId(
                        area != null
                                ? area.getId()
                                : null
                )

                .area(
                        area != null
                                ? area.getNameEn()
                                : null
                )

                .areaAr(
                        area != null
                                ? area.getNameAr()
                                : null
                )

                .latitude(latitude)
                .longitude(longitude)


                // ---------------------------------------------
                // RENTAL
                // ---------------------------------------------

                .tenantType(
                        flat.getTenantType()
                )

                .rentTimeType(
                        flat.getRentalType()
                )

                .price(
                        flat.getPrice()
                )


                // ---------------------------------------------
                // LANDLORD
                // ---------------------------------------------

                .landlordId(
                        landlord != null
                                ? landlord.getId()
                                : null
                )

                .landlordName(
                        landlordName(landlord)
                )

                .landlordPhoneNumber(
                        landlord != null
                                ? landlord.getPhoneNumber()
                                : null
                )


                // ---------------------------------------------
                // BUILDING
                // ---------------------------------------------

                .buildingId(
                        building != null
                                ? building.getId()
                                : null
                )

                .buildingName(
                        building != null
                                ? building.getBuildingName()
                                : null
                )

                .buildingNumber(
                        building != null
                                ? building.getBuildingNumber()
                                : null
                )


                // ---------------------------------------------
                // HOME
                // ---------------------------------------------

                .homeId(
                        home != null
                                ? home.getId()
                                : null
                )

                .homeNumber(
                        home != null
                                ? home.getHomeNumber()
                                : null
                )

                .homeLabel(
                        home != null
                                ? home.getHomeType()
                                + "."
                                + home.getHomeNumber()
                                : null
                )

                .homeType(
                        home != null
                                && home.getHomeType() != null
                                ? home.getHomeType().toString()
                                : null
                )


                // ---------------------------------------------
                // PARENT FEATURES
                // ---------------------------------------------

                .hasParking(
                        hasParking
                )

                .hasGym(
                        hasGym
                )

                .hasSwimmingPool(
                        hasSwimmingPool
                )


                .images(
                        imageUrls == null
                                ? Collections.emptyList()
                                : imageUrls
                )

                .build();
    }


    private FlatListDTO toListDTO(
            EntityFlats flat
    ) {

        return FlatListDTO.builder()
                .flatId(flat.getId())
                .flatNumber(flat.getFlatNumber())
                .floorNumber(flat.getFloorNumber())
                .build();
    }


    private FlatStatusDTO toStatusDTO(
            EntityUnitStatus status
    ) {

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


    // =========================================================
    // COMMON UTILITIES
    // =========================================================

    private Long homeIdOf(
            EntityFlats flat
    ) {

        return flat == null
                || flat.getHome() == null
                ? null
                : flat.getHome().getId();
    }


    private String landlordName(
            EntityUser landlord
    ) {

        if (landlord == null) {
            return null;
        }

        String firstName =
                clean(landlord.getFirstName());

        String lastName =
                clean(landlord.getLastName());

        String fullName =
                String.join(
                        " ",
                        firstName == null ? "" : firstName,
                        lastName == null ? "" : lastName
                ).trim();

        return fullName.isEmpty()
                ? null
                : fullName;
    }


    private String cleanRequired(
            String value,
            String message
    ) {

        String cleaned =
                clean(value);

        if (cleaned == null) {

            throw new FieldValidationException(
                    message,
                    Map.of(
                            "value",
                            message
                    )
            );
        }

        return cleaned;
    }


    private String clean(
            String value
    ) {

        if (value == null) {
            return null;
        }

        String cleaned =
                value.trim();

        return cleaned.isEmpty()
                ? null
                : cleaned;
    }


    private long safeCount(
            Long value
    ) {

        return value == null
                ? 0L
                : Math.max(
                value,
                0L
        );
    }


    private void addValidId(
            Set<Long> ids,
            Long id
    ) {

        if (id != null && id > 0) {
            ids.add(id);
        }
    }


    private void validateId(
            Long id,
            String field
    ) {

        if (id == null || id <= 0) {

            throw fieldError(
                    field,
                    field
                            + " is required and must be greater than zero."
            );
        }
    }


    private FieldValidationException fieldError(
            String field,
            String message
    ) {

        return new FieldValidationException(
                message,
                Map.of(
                        field,
                        message
                )
        );
    }
}