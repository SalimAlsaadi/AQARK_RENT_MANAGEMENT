package com.AQARK.AQARK_RENT_MANAGEMENT.Services.Implementation;

import com.AQARK.AQARK_RENT_MANAGEMENT.Common_Utilities.PropertyMediaService;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.MediaDTO.PropertyMediaDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.RoomDTO.*;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.*;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Enums.PropertyType;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Enums.StatusDomain;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Enums.StatusLevel;
import com.AQARK.AQARK_RENT_MANAGEMENT.Exceptions.BusinessConflictException;
import com.AQARK.AQARK_RENT_MANAGEMENT.Exceptions.FieldValidationException;
import com.AQARK.AQARK_RENT_MANAGEMENT.Repositories.*;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface.RoomsServicesInterface;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface.UnitStatusServiceInterface;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.Null;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RoomsServicesImpl implements RoomsServicesInterface {


    private static final String AVAILABLE = "AVAILABLE";
    private static final String UNDER_MAINTENANCE = "UNDER_MAINTENANCE";
    private static final String BLOCKED = "BLOCKED";

    private static final String AVAILABLE_AS_ROOMS = "AVAILABLE_AS_ROOMS";
    private static final String PARTIALLY_RENTED_AS_ROOMS = "PARTIALLY_RENTED_AS_ROOMS";
    private static final String FULLY_RENTED_AS_ROOMS = "FULLY_RENTED_AS_ROOMS";

    private final RoomRepository roomRepository;
    private final FlatsRepository flatsRepository;
    private final HomeRepository homeRepository;
    private final BuildingRepository buildingRepository;

    private final PropertyMediaService propertyMediaService;
    private final UnitStatusServiceInterface unitStatusService;

    private final FlatsServicesImpl flatsService;
    private final HomeServiceImpl homeService;



    @Override
    @Transactional
    public RoomDetailsDTO createRoom(
            RoomCreateRequestDTO dto,
            List<MultipartFile> images,
            Long landlordId
    ) throws IOException {

        validateAuthenticatedLandlord(landlordId);


        RoomParentType parentType = resolveParentType(dto.getBuildingId(), dto.getFlatId(), dto.getHomeId());

        requireValidParent(parentType);

        EntityRooms room = new EntityRooms();

        applyCreateFields(room, dto);

        room.setStatus(resolveRoomStatus(dto.getStatusCode(), StatusLevel.U));

        assignParent(
                room,
                parentType,
                dto.getBuildingId(),
                dto.getFlatId(),
                dto.getHomeId(),
                landlordId
        );

        EntityRooms savedRoom = roomRepository.saveAndFlush(room);

        saveRoomImages(savedRoom.getId(), images);

        refreshAffectedParents(
       savedRoom.getFlat().getId(), savedRoom.getHome().getId()
        );

        return toDTO(savedRoom);
    }


    // =========================================================
    // UPDATE
    // =========================================================

    @Override
    @Transactional
    public RoomDetailsDTO updateRoom(
            Long roomId,
            RoomUpdateRequestDTO dto,
            List<MultipartFile> newImages,
            List<Long> imageIdsToDelete,
            Long landlordId
    ) throws IOException {

        validateId(roomId, "roomId");
        validateAuthenticatedLandlord(landlordId);

        if (dto == null) {
            throw fieldError("data", "Room request is required.");
        }

        EntityRooms room = getRoom(roomId);

        validateRoomOwnerAccess(room, landlordId);

        /*
         * Only submitted fields are changed.
         */
        applyUpdateFields(room, dto);

        /*
         * Status is optional during update.
         *
         * If provided, it must still be a valid setup-level Room status.
         */
        if (dto.getStatusCode() != null) {
            room.setStatus(resolveRoomStatus(dto.getStatusCode(), StatusLevel.U));
        }

        RoomParentType requestedParentType = resolveParentType(
                dto.getBuildingId(),
                dto.getFlatId(),
                dto.getHomeId()
        );

        if (requestedParentType == RoomParentType.INVALID) {
            throw invalidParentException();
        }

        /*
         * NONE on update means:
         *
         * "Do not = the current parent."
         *
         * Any other valid combination means the caller wants to assign/move
         * the Room to that parent.
         */
        if (requestedParentType != RoomParentType.NONE) {

            assignParent(
                    room,
                    requestedParentType,
                    dto.getBuildingId(),
                    dto.getFlatId(),
                    dto.getHomeId(),
                    landlordId
            );

        } else if (dto.getRoomNumber() != null) {

            validateRoomNumberAgainstCurrentParent(room);
        }

        EntityRooms savedRoom = roomRepository.saveAndFlush(room);

        propertyMediaService.updateImages(
                PropertyType.ROOM,
                savedRoom.getId(),
                imageIdsToDelete,
                newImages
        );

        Long flatId = savedRoom.getFlat() != null
                        ? savedRoom.getFlat().getId()
                        : null;


        Long homeId = savedRoom.getHome() != null
                        ? savedRoom.getHome().getId()
                        : null;

        refreshAffectedParents(flatId, homeId);

        return toDTO(savedRoom);
    }


    // =========================================================
    // GET
    // =========================================================

    @Override
    public RoomDetailsDTO getRoomById(Long roomId) {

        validateId(roomId, "roomId");

        return toDTO(getRoom(roomId));
    }


    @Override
    public List<RoomDetailsDTO> getAllRooms() {

        return roomRepository
                .findAllDetails()
                .stream()
                .map(this::toDTO)
                .toList();
    }


    @Override
    public List<RoomDetailsDTO> getRoomsByBuildingId(Long buildingId) {

        validateId(buildingId, "buildingId");

        return roomRepository
                .findDetailsByBuildingId(buildingId)
                .stream()
                .map(this::toDTO)
                .toList();
    }


    @Override
    public List<RoomDetailsDTO> getRoomsByFlatId(Long flatId) {

        validateId(flatId, "flatId");

        return roomRepository
                .findDetailsByFlatId(flatId)
                .stream()
                .map(this::toDTO)
                .toList();
    }


    @Override
    public List<RoomDetailsDTO> getRoomsByHomeId(Long homeId) {

        validateId(homeId, "homeId");

        return roomRepository
                .findDetailsByHomeId(homeId)
                .stream()
                .map(this::toDTO)
                .toList();
    }


    // =========================================================
    // DELETE
    // =========================================================

    @Override
    @Transactional
    public void deleteRoom(Long roomId) {

        validateId(roomId, "roomId");

        EntityRooms room = getRoom(roomId);

        Long oldFlatId = idOf(room.getFlat());
        Long oldHomeId = resolveHomeId(room);

        propertyMediaService.softDeleteAllImages(
                PropertyType.ROOM,
                room.getId()
        );

        room.setDeleted(true);
        room.setActive(false);
        room.setDeletedAt(LocalDateTime.now());

        roomRepository.saveAndFlush(room);

        /*
         * A deleted Room may change its Flat/Home derived status.
         */
        refreshAffectedParents(room.getFlat().getId(), room.getHome().getId());
    }


    // =========================================================
    // PARENT TYPE
    // =========================================================

    /**
     * Converts the three nullable parent IDs into one clear RoomParentType.
     *
     * Supported:
     *
     * building only              -> DIRECT_BUILDING
     * building + flat            -> BUILDING_FLAT
     * home only                  -> DIRECT_HOME
     * home + flat                -> HOME_FLAT
     *
     * none                       -> NONE
     *
     * everything else           -> INVALID
     */
    private RoomParentType resolveParentType(
            Long buildingId,
            Long flatId,
            Long homeId
    ) {

        boolean hasBuilding = buildingId != null;
        boolean hasFlat = flatId != null;
        boolean hasHome = homeId != null;

        if (!hasBuilding && !hasFlat && !hasHome) {
            return RoomParentType.NONE;
        }

        if (hasBuilding && !hasFlat && !hasHome) {
            return RoomParentType.DIRECT_BUILDING;
        }

        if (hasBuilding && hasFlat && !hasHome) {
            return RoomParentType.BUILDING_FLAT;
        }

        if (!hasBuilding && !hasFlat && hasHome) {
            return RoomParentType.DIRECT_HOME;
        }

        if (!hasBuilding && hasFlat && hasHome) {
            return RoomParentType.HOME_FLAT;
        }

        return RoomParentType.INVALID;
    }



    private void requireValidParent(RoomParentType parentType) {

        if (parentType == RoomParentType.NONE || parentType == RoomParentType.INVALID) {

            throw invalidParentException();
        }
    }


    private FieldValidationException invalidParentException() {

        return fieldError("parent", "Room must belong directly to a building, " + "a building flat, a home directly, or a home flat.");
    }


    // =========================================================
    // ASSIGN PARENT
    // =========================================================

    /**
     * Central method for assigning/moving a Room.
     *
     * This replaces:
     *
     * assignDirectBuildingRoom()
     * assignBuildingFlatRoom()
     * assignDirectHomeRoom()
     * assignHomeFlatRoom()
     *
     * All ownership, capacity, status and duplicate checks happen here.
     */
    private void assignParent(
            EntityRooms room,
            RoomParentType parentType,
            Long buildingId,
            Long flatId,
            Long homeId,
            Long landlordId
    ) {

        String roomNumber = cleanRoomNumber(room.getRoomNumber());

        switch (parentType) {

            // -------------------------------------------------
            // DIRECT BUILDING
            // -------------------------------------------------

            case DIRECT_BUILDING -> {

                EntityBuildings building = getBuilding(buildingId);

                validateBuildingOwnership(building, landlordId);

                validateBuildingAcceptsRooms(building);

                validateRoomNumberUnique(
                        room,
                        RoomParentType.DIRECT_BUILDING,
                        building.getId(),
                        null,
                        null,
                        roomNumber
                );

                setParent(
                        room,
                        building,
                        null,
                        null
                );
            }


            // -------------------------------------------------
            // BUILDING -> FLAT -> ROOM
            // -------------------------------------------------

            case BUILDING_FLAT -> {

                EntityBuildings building = getBuilding(buildingId);

                EntityFlats flat = getFlat(flatId);

                validateBuildingOwnership(building, landlordId);

                validateFlatOwnership(flat, landlordId);

                validateFlatBelongsToBuilding(flat, building);

                validateFlatCanBeUsedAsRooms(flat);

                validateFlatRoomCapacity(flat, room.getId());

                validateRoomNumberUnique(
                        room,
                        RoomParentType.BUILDING_FLAT,
                        null,
                        flat.getId(),
                        null,
                        roomNumber
                );

                setParent(
                        room,
                        building,
                        flat,
                        null
                );
            }


            // -------------------------------------------------
            // DIRECT HOME
            // -------------------------------------------------

            case DIRECT_HOME -> {

                EntityHomes home = getHome(homeId);

                validateHomeOwnership(home, landlordId);

                validateHomeAcceptsRooms(home);

                validateHomeDirectRoomCapacity(home, room.getId());

                validateRoomNumberUnique(
                        room,
                        RoomParentType.DIRECT_HOME,
                        null,
                        null,
                        home.getId(),
                        roomNumber
                );

                setParent(
                        room,
                        null,
                        null,
                        home
                );
            }


            // -------------------------------------------------
            // HOME -> FLAT -> ROOM
            // -------------------------------------------------

            case HOME_FLAT -> {

                EntityHomes home = getHome(homeId);

                EntityFlats flat = getFlat(flatId);

                validateHomeOwnership(home, landlordId);

                validateFlatOwnership(flat, landlordId);

                validateFlatBelongsToHome(flat, home);

                validateFlatCanBeUsedAsRooms(flat);

                validateFlatRoomCapacity(flat, room.getId());

                validateRoomNumberUnique(
                        room,
                        RoomParentType.HOME_FLAT,
                        null,
                        flat.getId(),
                        null,
                        roomNumber
                );

                setParent(
                        room,
                        null,
                        flat,
                        home
                );
            }


            default -> throw invalidParentException();
        }

        room.setRoomNumber(roomNumber);

        activateRoom(room);
    }


    /**
     * Setting all three references together is important.
     *
     * It automatically clears the previous parent when moving a Room.
     */
    private void setParent(
            EntityRooms room,
            EntityBuildings building,
            EntityFlats flat,
            EntityHomes home
    ) {

        room.setBuilding(building);
        room.setFlat(flat);
        room.setHome(home);
    }


    // =========================================================
    // PARENT RELATIONSHIP VALIDATION
    // =========================================================

    private void validateFlatBelongsToBuilding(
            EntityFlats flat,
            EntityBuildings building
    ) {

        if (flat.getBuilding() == null
                || !Objects.equals(
                flat.getBuilding().getId(),
                building.getId()
        )) {

            throw fieldError(
                    "flatId",
                    "Selected flat does not belong to the selected building."
            );
        }
    }


    private void validateFlatBelongsToHome(
            EntityFlats flat,
            EntityHomes home
    ) {

        if (flat.getHome() == null
                || !Objects.equals(
                flat.getHome().getId(),
                home.getId()
        )) {

            throw fieldError(
                    "flatId",
                    "Selected flat does not belong to the selected home."
            );
        }
    }


    // =========================================================
    // CAPACITY VALIDATION
    // =========================================================

    /**
     * Flat numberOfRooms determines the maximum number of active Room
     * entities that may be created from that Flat.
     */
    private void validateFlatRoomCapacity(
            EntityFlats flat,
            Long currentRoomId
    ) {

        int allowedRooms =
                Optional.ofNullable(flat.getNumberOfRooms())
                        .orElse(0);

        if (allowedRooms <= 0) {

            throw new BusinessConflictException(
                    "Flat does not have a valid room capacity."
            );
        }

        /*
         * currentRoomId is excluded by repository logic during update.
         */
        long usedRooms =
                roomRepository.countActiveRoomsByFlatId(
                        flat.getId(),
                        currentRoomId
                );

        if (usedRooms >= allowedRooms) {

            throw new BusinessConflictException(
                    "Cannot add another room. Flat "
                            + flat.getFlatNumber()
                            + " allows only "
                            + allowedRooms
                            + " room(s)."
            );
        }
    }


    /**
     * Direct Rooms of a Home must not exceed Home.numberOfRooms.
     */
    private void validateHomeDirectRoomCapacity(
            EntityHomes home,
            Long currentRoomId
    ) {

        int allowedRooms =
                Optional.ofNullable(home.getNumberOfRooms())
                        .orElse(0);

        if (allowedRooms <= 0) {

            throw new BusinessConflictException(
                    "Home does not have a valid direct-room capacity."
            );
        }

        long usedRooms =
                roomRepository.countActiveDirectRoomsByHomeId(
                        home.getId(),
                        currentRoomId
                );

        if (usedRooms >= allowedRooms) {

            throw new BusinessConflictException(
                    "Cannot add another direct room. "
                            + usedRooms
                            + " of "
                            + allowedRooms
                            + " direct Home room(s) are already assigned."
            );
        }
    }


    // =========================================================
    // PARENT STATUS BUSINESS VALIDATION
    // =========================================================

    /**
     * A Flat can contain independently managed Rooms while:
     *
     * AVAILABLE
     * AVAILABLE_AS_ROOMS
     * PARTIALLY_RENTED_AS_ROOMS
     * FULLY_RENTED_AS_ROOMS
     *
     * Capacity validation will still prevent creating additional Rooms
     * when the configured numberOfRooms has already been reached.
     */
    private void validateFlatCanBeUsedAsRooms(EntityFlats flat) {

        String code =
                statusCode(flat.getStatus());

        if (code == null) {

            throw new BusinessConflictException(
                    "Flat status is missing."
            );
        }

        if (AVAILABLE.equalsIgnoreCase(code)
                || AVAILABLE_AS_ROOMS.equalsIgnoreCase(code)
                || PARTIALLY_RENTED_AS_ROOMS.equalsIgnoreCase(code)
                || FULLY_RENTED_AS_ROOMS.equalsIgnoreCase(code)) {

            return;
        }

        throw new BusinessConflictException(
                "Cannot add rooms because flat status is "
                        + code
                        + "."
        );
    }


    /**
     * Building can receive Rooms unless its operational state prevents it.
     */
    private void validateBuildingAcceptsRooms(
            EntityBuildings building
    ) {

        String code =
                statusCode(building.getStatus());

        if (UNDER_MAINTENANCE.equalsIgnoreCase(code)
                || BLOCKED.equalsIgnoreCase(code)) {

            throw new BusinessConflictException(
                    "Cannot add a room because building status is "
                            + code
                            + "."
            );
        }
    }


    /**
     * AVAILABLE, RENTED and RESERVED Home scenarios are allowed.
     *
     * A Home can therefore contain independently managed Rooms even when
     * its own whole-home rental state is RENTED/RESERVED, according to the
     * current business rule.
     *
     * Only states that make the property unusable prevent Room operations.
     */
    private void validateHomeAcceptsRooms(
            EntityHomes home
    ) {

        String code =
                statusCode(home.getStatus());

        if (UNDER_MAINTENANCE.equalsIgnoreCase(code)
                || BLOCKED.equalsIgnoreCase(code)) {

            throw new BusinessConflictException(
                    "Cannot add a room because home status is "
                            + code
                            + "."
            );
        }
    }


    // =========================================================
    // ROOM NUMBER UNIQUENESS
    // =========================================================

    /**
     * One duplicate validator replaces three almost identical methods.
     *
     * Uniqueness scope:
     *
     * DIRECT_BUILDING -> within direct Rooms of that Building
     *
     * DIRECT_HOME     -> within direct Rooms of that Home
     *
     * BUILDING_FLAT /
     * HOME_FLAT       -> within that Flat
     */
    private void validateRoomNumberUnique(
            EntityRooms room,
            RoomParentType parentType,
            Long buildingId,
            Long flatId,
            Long homeId,
            String roomNumber
    ) {

        boolean exists;

        switch (parentType) {

            case DIRECT_BUILDING ->

                    exists = roomRepository
                            .existsByBuildingIdAndFlatIsNullAndHomeIsNullAndRoomNumberIgnoreCaseAndDeletedFalse(
                                    buildingId,
                                    roomNumber
                            );


            case DIRECT_HOME ->

                    exists = roomRepository
                            .existsByHomeIdAndFlatIsNullAndBuildingIsNullAndRoomNumberIgnoreCaseAndDeletedFalse(
                                    homeId,
                                    roomNumber
                            );


            case BUILDING_FLAT, HOME_FLAT ->

                    exists = roomRepository
                            .existsByFlatIdAndRoomNumberIgnoreCaseAndDeletedFalse(
                                    flatId,
                                    roomNumber
                            );


            default ->
                    throw invalidParentException();
        }

        if (!exists) {
            return;
        }

        /*
         * During UPDATE, repository exists(...) also sees the current Room.
         *
         * Therefore, allow the result only when the matching record is
         * actually this same Room in the same parent scope.
         *
         * During CREATE room.getId() == null, so duplicate always fails.
         */
        if (isSameExistingRoom(
                room,
                parentType,
                buildingId,
                flatId,
                homeId,
                roomNumber
        )) {
            return;
        }

        String location = switch (parentType) {

            case DIRECT_BUILDING ->
                    "directly in this building";

            case DIRECT_HOME ->
                    "directly in this home";

            case BUILDING_FLAT, HOME_FLAT ->
                    "in this flat";

            default ->
                    "in this parent";
        };

        throw fieldError(
                "roomNumber",
                "Room number already exists "
                        + location
                        + "."
        );
    }


    /**
     * Determines whether exists(...) found the current Room itself.
     */
    private boolean isSameExistingRoom(
            EntityRooms room,
            RoomParentType parentType,
            Long buildingId,
            Long flatId,
            Long homeId,
            String roomNumber
    ) {

        if (room == null
                || room.getId() == null
                || room.getRoomNumber() == null
                || !roomNumber.equalsIgnoreCase(room.getRoomNumber())) {

            return false;
        }

        return switch (parentType) {

            case DIRECT_BUILDING ->

                    room.getBuilding() != null
                            && room.getFlat() == null
                            && room.getHome() == null
                            && Objects.equals(
                            room.getBuilding().getId(),
                            buildingId
                    );


            case DIRECT_HOME ->

                    room.getHome() != null
                            && room.getFlat() == null
                            && room.getBuilding() == null
                            && Objects.equals(
                            room.getHome().getId(),
                            homeId
                    );


            case BUILDING_FLAT, HOME_FLAT ->

                    room.getFlat() != null
                            && Objects.equals(
                            room.getFlat().getId(),
                            flatId
                    );


            default -> false;
        };
    }


    /**
     * Used when UPDATE changes Room number but does not submit parent IDs.
     *
     * This scenario must still run duplicate validation.
     */
    private void validateRoomNumberAgainstCurrentParent(
            EntityRooms room
    ) {

        String roomNumber =
                cleanRoomNumber(room.getRoomNumber());

        room.setRoomNumber(roomNumber);

        /*
         * Flat Room has priority because a Flat Room also contains either
         * Building or Home references.
         */
        if (room.getFlat() != null) {

            RoomParentType type =
                    room.getBuilding() != null
                            ? RoomParentType.BUILDING_FLAT
                            : RoomParentType.HOME_FLAT;

            validateRoomNumberUnique(
                    room,
                    type,
                    null,
                    room.getFlat().getId(),
                    null,
                    roomNumber
            );

            return;
        }


        if (room.getBuilding() != null) {

            validateRoomNumberUnique(
                    room,
                    RoomParentType.DIRECT_BUILDING,
                    room.getBuilding().getId(),
                    null,
                    null,
                    roomNumber
            );

            return;
        }


        if (room.getHome() != null) {

            validateRoomNumberUnique(
                    room,
                    RoomParentType.DIRECT_HOME,
                    null,
                    null,
                    room.getHome().getId(),
                    roomNumber
            );

            return;
        }


        throw new BusinessConflictException(
                "Room does not have a valid parent."
        );
    }


    // =========================================================
    // OWNERSHIP / AUTHORIZATION
    // =========================================================

    private void validateRoomOwnerAccess(
            EntityRooms room,
            Long landlordId
    ) {

        /*
         * If Room belongs to Flat, Flat ownership is enough because Flat
         * itself has already been assigned to its landlord.
         */
        if (room.getFlat() != null) {

            validateFlatOwnership(
                    room.getFlat(),
                    landlordId
            );

            return;
        }


        if (room.getBuilding() != null) {

            validateBuildingOwnership(
                    room.getBuilding(),
                    landlordId
            );

            return;
        }


        if (room.getHome() != null) {

            validateHomeOwnership(
                    room.getHome(),
                    landlordId
            );

            return;
        }


        throw new AccessDeniedException(
                "Room does not have a valid parent owner."
        );
    }


    private void validateBuildingOwnership(
            EntityBuildings building,
            Long landlordId
    ) {

        if (building.getLandlord() == null
                || !Objects.equals(
                building.getLandlord().getId(),
                landlordId
        )) {

            throw new AccessDeniedException(
                    "You are not allowed to modify this building."
            );
        }
    }


    private void validateFlatOwnership(
            EntityFlats flat,
            Long landlordId
    ) {

        if (flat.getLandlord() == null
                || !Objects.equals(
                flat.getLandlord().getId(),
                landlordId
        )) {

            throw new AccessDeniedException(
                    "You are not allowed to modify this flat."
            );
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
                    "You are not allowed to modify this home."
            );
        }
    }


    // =========================================================
    // ENTITY LOOKUP
    // =========================================================

    private EntityRooms getRoom(Long roomId) {

        EntityRooms room =
                roomRepository.findDetailsById(roomId);

        if (room == null) {

            throw new EntityNotFoundException(
                    "Room not found with id: "
                            + roomId
            );
        }

        return room;
    }


    private EntityBuildings getBuilding(Long buildingId) {

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


    /**
     * Locked because adding/moving Rooms may depend on Home capacity/state.
     */
    private EntityHomes getHome(Long homeId) {

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


    /**
     * Locked because multiple concurrent Room creates must not exceed
     * Flat room capacity.
     */
    private EntityFlats getFlat(Long flatId) {

        validateId(
                flatId,
                "flatId"
        );

        return flatsRepository
                .findByIdForUpdate(flatId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Flat not found with id: "
                                        + flatId
                        )
                );
    }


    // =========================================================
    // DTO -> ENTITY
    // =========================================================

    private void applyCreateFields(EntityRooms room, RoomCreateRequestDTO dto) {

        room.setRoomNumber(cleanRoomNumber(dto.getRoomNumber()));
        room.setSize(clean(dto.getSize()));
        room.setTenantType(clean(dto.getTenantType()));
        room.setRentTimeType(clean(dto.getRentTimeType()));
        room.setPrice(dto.getPrice());
        room.setHasPrivateBathroom(Boolean.TRUE.equals(dto.getHasPrivateBathroom()));
        room.setHasAC(Boolean.TRUE.equals(dto.getHasAC()));
        room.setHasWIFI(Boolean.TRUE.equals(dto.getHasWIFI()));
        room.setFurnished(Boolean.TRUE.equals(dto.getIsFurnished()));
        room.setKitchen(clean(dto.getKitchen()));
        room.setDescription(clean(dto.getDescription()));
    }


    /**
     * Partial update.
     *
     * null means "do not update this field".
     */
    private void applyUpdateFields(EntityRooms room, RoomUpdateRequestDTO dto) {

        if (dto.getRoomNumber() != null) {
            room.setRoomNumber(cleanRoomNumber(dto.getRoomNumber()));
        }

        if (dto.getSize() != null) {
            room.setSize(clean(dto.getSize()));
        }

        if (dto.getTenantType() != null) {
            room.setTenantType(clean(dto.getTenantType()));
        }

        if (dto.getRentTimeType() != null) {
            room.setRentTimeType(clean(dto.getRentTimeType()));
        }

        if (dto.getPrice() != null) {
            room.setPrice(dto.getPrice());
        }

        if (dto.getHasPrivateBathroom() != null) {
            room.setHasPrivateBathroom(dto.getHasPrivateBathroom());
        }

        if (dto.getHasAC() != null) {
            room.setHasAC(dto.getHasAC());
        }

        if (dto.getHasWIFI() != null) {
            room.setHasWIFI(dto.getHasWIFI());
        }

        if (dto.getIsFurnished() != null) {
            room.setFurnished(dto.getIsFurnished());
        }

        if (dto.getKitchen() != null) {
            room.setKitchen(clean(dto.getKitchen()));
        }

        if (dto.getDescription() != null) {
            room.setDescription(clean(dto.getDescription()));
        }
    }


    // =========================================================
    // STATUS
    // =========================================================
    private EntityUnitStatus resolveRoomStatus(String code, StatusLevel level) {

        return unitStatusService.getRequiredStatus(StatusDomain.ROOM, normalizeStatus(code), level);
    }


    private String statusCode(EntityUnitStatus status) {

        return status == null ? null
                : status.getCode();
    }


    // =========================================================
    // MEDIA
    // =========================================================
    private void saveRoomImages(Long roomId, List<MultipartFile> images) throws IOException {

        if (images == null || images.isEmpty()) {
            return;
        }

        propertyMediaService.saveImages(
                images,
                PropertyType.ROOM,
                roomId
        );
    }


    // =========================================================
    // REFRESH PARENT STATES
    // =========================================================

    /**
     * Refresh every affected Flat/Home exactly once.
     *
     * Examples:
     *
     * CREATE:
     * old = null
     * new = selected parent
     *
     * MOVE:
     * old = old parent
     * new = new parent
     *
     * STATUS CHANGE:
     * old/new may be the same -> Set removes duplicate
     *
     * DELETE:
     * old = previous parent
     * new = null
     */
    private void refreshAffectedParents(Long flatId, Long homeId) {


            flatsService.refreshFlatState(flatId);

            homeService.refreshHomeState(homeId);

    }


    private void addValidId(Set<Long> ids, Long id) {

        if (id != null && id > 0) {
            ids.add(id);
        }
    }


    // =========================================================
    // ENTITY -> DTO
    // =========================================================

    private RoomDetailsDTO toDTO(EntityRooms room) {

        EntityBuildings building = room.getBuilding();
        EntityHomes home = room.getHome();
        EntityFlats flat = room.getFlat();

        EntityBuildings locationBuilding = building != null ? building
                                        : flat != null ? flat.getBuilding()
                                        : null;


        EntityHomes locationHome = home != null ? home
                                : flat != null ? flat.getHome()
                                : null;


        List<PropertyMediaDTO> images =
                propertyMediaService
                        .getImages(PropertyType.ROOM, room.getId())
                        .stream()
                        .map(image ->
                                PropertyMediaDTO.builder()
                                        .id(image.getId())
                                        .fileUrl(image.getFileUrl())
                                        .originalFileName(image.getOriginalFileName())
                                        .contentType(image.getContentType())
                                        .sizeBytes(image.getSizeBytes())
                                        .mainImage(image.isMainImage())
                                        .sortOrder(image.getSortOrder())
                                        .build()
                        )
                        .toList();


        return RoomDetailsDTO.builder()

                .id(room.getId())
                .roomNumber(room.getRoomNumber())
                .size(room.getSize())
                .status(toStatusDTO(room.getStatus()))
                .tenantType(room.getTenantType())
                .rentTimeType(room.getRentTimeType())
                .price(room.getPrice())
                .hasPrivateBathroom(room.isHasPrivateBathroom())
                .hasAC(room.isHasAC())
                .hasWIFI(room.isHasWIFI())
                .isFurnished(room.isFurnished())
                .kitchen(room.getKitchen())
                .description(room.getDescription())
                .buildingId(building != null ? building.getId() : null)
                .flatId(flat != null ? flat.getId() : null)
                .regionId(locationBuilding !=null ? locationBuilding.getRegion().getId() : locationHome.getRegion().getId())
                .region(locationBuilding != null ? locationBuilding.getRegion().getNameEn() : locationHome.getRegion().getNameEn())
                .regionAr(locationBuilding != null ? locationBuilding.getRegion().getNameAr() : locationHome.getRegion().getNameAr())
                .wilayaId(locationBuilding != null ? locationBuilding.getWilaya().getId() : locationHome.getWilaya().getId())
                .wilaya(locationBuilding != null ? locationBuilding.getWilaya().getNameEn() : locationHome.getWilaya().getNameEn())
                .wilayaAr(locationBuilding != null ? locationBuilding.getWilaya().getNameAr() : locationHome.getWilaya().getNameAr())
                .areaId(locationBuilding != null ? locationBuilding.getArea().getId() : locationHome.getArea().getId())
                .area(locationBuilding != null ? locationBuilding.getArea().getNameEn() : locationHome.getArea().getNameEn())
                .areaAr(locationBuilding !=null ? locationBuilding.getArea().getNameAr() : locationHome.getArea().getNameAr())
                .homeId(home != null ? home.getId() : null)
                .buildingName(locationBuilding != null ? locationBuilding.getBuildingName() : null)
                .buildingNumber(locationBuilding != null ? locationBuilding.getBuildingNumber() : null)
                .flatLabel(flat != null ? "Flat " + flat.getFlatNumber() : null)
                .homeLabel(locationHome != null ? locationHome.getHomeType() + "." + locationHome.getHomeNumber() : null)
                .fromFlat(flat != null)
                .fromHome(locationHome != null)
                .images(images == null ? Collections.emptyList() : images)
                .build();
    }


    private RoomStatusDTO toStatusDTO(EntityUnitStatus status) {

        if (status == null) {
            return null;
        }

        return RoomStatusDTO.builder()
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

    private void activateRoom(EntityRooms room) {

        room.setActive(true);
        room.setDeleted(false);
        room.setDeletedAt(null);
    }



    private String cleanRoomNumber(String value) {

        String cleaned = clean(value);

        if (cleaned == null) {

            throw fieldError("roomNumber", "Room number is required.");
        }

        if (cleaned.contains("_R-")) {

            cleaned = cleaned.substring(cleaned.lastIndexOf("_R-") + 3);
        }

        if (cleaned.isBlank()) {

            throw fieldError("roomNumber", "Room number is required.");
        }

        return cleaned;
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


    private String normalizeStatus(String value) {

        String cleaned = clean(value);

        if (cleaned == null) {

            throw fieldError("statusCode", "Room status is required.");
        }

        return cleaned.toUpperCase(Locale.ROOT);
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


    private FieldValidationException fieldError(String field, String message) {

        return new FieldValidationException(message, Map.of(field, message));
    }


    private Long idOf(EntityFlats flat) {

        return flat == null ? null
                : flat.getId();
    }


    private Long resolveHomeId(EntityRooms room) {

        if (room == null) {
            return null;
        }

        if (room.getHome() != null) {
            return room.getHome().getId();
        }

        if (room.getFlat() != null && room.getFlat().getHome() != null) {

            return room
                    .getFlat()
                    .getHome()
                    .getId();
        }

        return null;
    }
}