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
public class RoomsServicesImpl
        implements RoomsServicesInterface {

    private static final String AVAILABLE = "AVAILABLE";
    private static final String RENTED = "RENTED";
    private static final String RESERVED = "RESERVED";
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
            RoomSaveRequestDTO dto,
            List<MultipartFile> images,
            Long landlordId
    ) throws IOException {

        validateCreateRoomRequest(dto, landlordId);

        EntityRooms room = new EntityRooms();

        applyRoomFields(room, dto);

        room.setStatus(resolveRoomStatus(dto.getStatusCode()));

        assignRequestedParent(room, dto, landlordId);

        EntityRooms savedRoom = roomRepository.saveAndFlush(room);

        if (images != null && !images.isEmpty()) {
            propertyMediaService.saveImages(
                    images,
                    PropertyType.ROOM,
                    savedRoom.getId()
            );
        }

        refreshAffectedParents(
                null,
                idOf(savedRoom.getFlat()),
                null,
                resolveHomeId(savedRoom)
        );

        return toDTO(getRoom(savedRoom.getId()));
    }

    @Override
    @Transactional
    public RoomDetailsDTO updateRoom(
            Long roomId,
            RoomSaveRequestDTO dto,
            List<MultipartFile> newImages,
            List<Long> imageIdsToDelete,
            Long landlordId
    ) throws IOException {

        validateId(roomId, "roomId");

        if (dto == null) {
            throw fieldError("data", "Room request is required.");
        }

        validateAuthenticatedLandlord(landlordId);

        EntityRooms room = getRoom(roomId);

        validateRoomOwnerAccess(room, landlordId);

        validateUpdateRoomRequest(dto);

        Long oldFlatId = idOf(room.getFlat());
        Long oldHomeId = resolveHomeId(room);

        applyRoomFields(room, dto);

        if (!isBlank(dto.getStatusCode())) {
            room.setStatus(resolveRoomStatus(dto.getStatusCode()));
        }

        if (hasAnyParentField(dto)) {

            validateParentFieldsOrThrow(dto);

            room.setBuilding(null);
            room.setFlat(null);
            room.setHome(null);

            assignRequestedParent(room, dto, landlordId);
        }

        EntityRooms savedRoom = roomRepository.saveAndFlush(room);

        propertyMediaService.updateImages(
                PropertyType.ROOM,
                savedRoom.getId(),
                imageIdsToDelete,
                newImages
        );

        refreshAffectedParents(
                oldFlatId,
                idOf(savedRoom.getFlat()),
                oldHomeId,
                resolveHomeId(savedRoom)
        );

        return toDTO(getRoom(savedRoom.getId()));
    }

    @Override
    public RoomDetailsDTO getRoomById(Long roomId) {
        validateId(roomId, "roomId");
        return toDTO(getRoom(roomId));
    }

    @Override
    public List<RoomDetailsDTO> getAllRooms() {
        return roomRepository.findAllDetails()
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

    @Override
    @Transactional
    public void deleteRoom(Long roomId) {

        validateId(roomId, "roomId");

        EntityRooms room = getRoom(roomId);
        Long oldFlatId = idOf(room.getFlat());
        Long oldHomeId = resolveHomeId(room);

        propertyMediaService.softDeleteAllImages(PropertyType.ROOM, room.getId());

        room.setDeleted(true);
        room.setActive(false);
        room.setDeletedAt(LocalDateTime.now());

        roomRepository.saveAndFlush(room);

        refreshAffectedParents(
                oldFlatId, null, oldHomeId, null
        );
    }

    private void assignRequestedParent(EntityRooms room, RoomSaveRequestDTO dto, Long landlordId) {

        if (isDirectBuildingRoom(dto)) {
            assignDirectBuildingRoom(room, dto.getBuildingId(), landlordId);
            return;
        }

        if (isBuildingFlatRoom(dto)) {
            assignBuildingFlatRoom(room, dto.getBuildingId(), dto.getFlatId(), landlordId);
            return;
        }

        if (isDirectHomeRoom(dto)) {
            assignDirectHomeRoom(room, dto.getHomeId(), landlordId);
            return;
        }

        if (isHomeFlatRoom(dto)) {
            assignHomeFlatRoom(room, dto.getHomeId(), dto.getFlatId(), landlordId);
            return;
        }

        throw fieldError("parent", "Unsupported room parent combination.");
    }

    private void assignDirectBuildingRoom(
            EntityRooms room,
            Long buildingId,
            Long landlordId
    ) {

        EntityBuildings building =
                getBuilding(buildingId);

        validateBuildingOwnership(
                building,
                landlordId
        );

        validateBuildingAcceptsRooms(building);

        String roomNumber =
                cleanRoomNumber(room.getRoomNumber());

        validateDirectBuildingRoomNumber(
                building.getId(),
                roomNumber,
                room.getId()
        );

        room.setRoomNumber(roomNumber);
        room.setBuilding(building);
        room.setFlat(null);
        room.setHome(null);

        activateRoom(room);
    }

    private void assignBuildingFlatRoom(
            EntityRooms room,
            Long buildingId,
            Long flatId,
            Long landlordId
    ) {

        EntityBuildings building =
                getBuilding(buildingId);

        EntityFlats flat =
                getFlat(flatId);

        validateBuildingOwnership(
                building,
                landlordId
        );

        validateFlatOwnership(
                flat,
                landlordId
        );

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

        validateFlatCanBeUsedAsRooms(flat);

        validateFlatRoomCapacity(
                flat,
                room.getId()
        );

        String roomNumber =
                cleanRoomNumber(room.getRoomNumber());

        validateFlatRoomNumber(
                flat.getId(),
                roomNumber,
                room.getId()
        );

        room.setRoomNumber(roomNumber);
        room.setBuilding(building);
        room.setFlat(flat);
        room.setHome(null);

        activateRoom(room);
    }

    private void assignDirectHomeRoom(
            EntityRooms room,
            Long homeId,
            Long landlordId
    ) {

        EntityHomes home = getHome(homeId);

        validateHomeOwnership(home, landlordId);

        validateHomeAcceptsAnnexRooms(home); //if home now rented as whole home or reserved or blocked

        validateHomeDirectRoomCapacity(home, room.getId());// to check if there is room free in this home to rent it.

        String roomNumber = cleanRoomNumber(room.getRoomNumber()); //generate room number with home arrangement code number

        validateDirectHomeRoomNumber(home.getId(), roomNumber, room.getId()); //to check if there is room has same room number as adding now to prevent duplicate room numbers

        room.setRoomNumber(roomNumber);
        room.setBuilding(null);
        room.setFlat(null);
        room.setHome(home);

        activateRoom(room);
    }

    private void assignHomeFlatRoom(
            EntityRooms room,
            Long homeId,
            Long flatId,
            Long landlordId
    ) {

        EntityHomes home = getHome(homeId);
        EntityFlats flat = getFlat(flatId);

        validateHomeOwnership(
                home,
                landlordId
        );

        validateFlatOwnership(
                flat,
                landlordId
        );

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

        validateFlatCanBeUsedAsRooms(flat);

        validateFlatRoomCapacity(
                flat,
                room.getId()
        );

        String roomNumber =
                cleanRoomNumber(room.getRoomNumber());

        validateFlatRoomNumber(
                flat.getId(),
                roomNumber,
                room.getId()
        );

        room.setRoomNumber(roomNumber);
        room.setBuilding(null);
        room.setFlat(flat);
        room.setHome(home);

        activateRoom(room);
    }

    private void validateFlatRoomCapacity(
            EntityFlats flat,
            Long currentRoomId
    ) {
        int allowedRooms = Optional.ofNullable(flat.getNumberOfRooms()).orElse(0);
        if (allowedRooms <= 0) {
            throw new BusinessConflictException("Flat does not have a valid room capacity.");
        }

        long usedRooms = roomRepository.countActiveRoomsByFlatId(flat.getId(), currentRoomId);
        if (usedRooms >= allowedRooms) {
            throw new BusinessConflictException(
                    "Cannot add another room. Flat " + flat.getFlatNumber()
                            + " allows only " + allowedRooms + " room(s)."
            );
        }
    }

    private void validateHomeDirectRoomCapacity(
            EntityHomes home,
            Long currentRoomId
    ) {
        int allowedRooms = Optional.ofNullable(home.getNumberOfRooms()).orElse(0);
        if (allowedRooms <= 0) {
            throw new BusinessConflictException("Home does not have a valid direct-room capacity.");
        }

        long usedRooms = roomRepository.countActiveDirectRoomsByHomeId(home.getId(), currentRoomId);
        if (usedRooms >= allowedRooms) {
            throw new BusinessConflictException(
                    "Cannot add another direct room. " + usedRooms + " of "
                            + allowedRooms + " direct Home room(s) are already assigned."
            );
        }
    }

    private void validateFlatCanBeUsedAsRooms(
            EntityFlats flat
    ) {

        String code = statusCode(flat.getStatus());

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

    /*
     * AVAILABLE, RENTED and RESERVED are accepted.
     *
     * A whole home may be rented while annex rooms are rented
     * independently.
     */
    private void validateHomeAcceptsAnnexRooms(
            EntityHomes home
    ) {

        String code = statusCode(home.getStatus());

        if (UNDER_MAINTENANCE.equalsIgnoreCase(code) || BLOCKED.equalsIgnoreCase(code)) {
            throw new BusinessConflictException("Cannot add an annex room because home status is " + code + ".");
        }
    }

    private void refreshFlatAfterRoomChange(
            EntityFlats oldFlat,
            EntityFlats newFlat
    ) {

        if (oldFlat != null) {
            refreshFlatStatus(oldFlat);
        }

        if (newFlat != null
                && (
                oldFlat == null
                        || !Objects.equals(
                        oldFlat.getId(),
                        newFlat.getId()
                )
        )) {
            refreshFlatStatus(newFlat);
        }
    }

    private void refreshFlatStatus(
            EntityFlats flat
    ) {

        if (flat == null || flat.isDeleted()) {
            return;
        }

        List<EntityRooms> activeRooms =
                roomRepository
                        .findDetailsByFlatId(flat.getId())
                        .stream()
                        .filter(EntityRooms::isActive)
                        .filter(room -> !room.isDeleted())
                        .toList();

        if (activeRooms.isEmpty()) {

            if (isDerivedRoomStatus(statusCode(flat.getStatus()))) {
                flat.setStatus(requiredStatus(StatusDomain.FLAT, AVAILABLE));

                flatsRepository.save(flat);
            }

            return;
        }

        long rented = countByStatus(activeRooms, RENTED);

        long nonAvailable =
                rented
                        + countByStatus(activeRooms, RESERVED)
                        + countByStatus(activeRooms, UNDER_MAINTENANCE)
                        + countByStatus(activeRooms, BLOCKED);

        String newStatusCode;

        if (rented == activeRooms.size()) {

            newStatusCode = FULLY_RENTED_AS_ROOMS;

        } else if (nonAvailable > 0) {

            newStatusCode = PARTIALLY_RENTED_AS_ROOMS;

        } else {

            newStatusCode = AVAILABLE_AS_ROOMS;
        }

        flat.setStatus(requiredStatus(StatusDomain.FLAT, newStatusCode));

        flatsRepository.save(flat);
    }

    private void applyRoomFields(EntityRooms room, RoomSaveRequestDTO dto) {

        if (dto.getRoomNumber() != null) {
            room.setRoomNumber(
                    cleanRequired(
                            dto.getRoomNumber(),
                            "Room number is required."
                    )
            );
        }

        if (dto.getSize() != null) {
            room.setSize(clean(dto.getSize()));
        }

        if (dto.getTenantType() != null) {
            room.setTenantType(
                    clean(dto.getTenantType())
            );
        }

        if (dto.getRentTimeType() != null) {
            room.setRentTimeType(
                    clean(dto.getRentTimeType())
            );
        }

        if (dto.getPrice() != null) {

            if (dto.getPrice()
                    .compareTo(BigDecimal.ZERO) < 0) {
                throw fieldError(
                        "price",
                        "Room price cannot be negative."
                );
            }

            room.setPrice(dto.getPrice());
        }

        if (dto.getHasPrivateBathroom() != null) {
            room.setHasPrivateBathroom(
                    Boolean.TRUE.equals(
                            dto.getHasPrivateBathroom()
                    )
            );
        }

        if (dto.getHasAC() != null) {
            room.setHasAC(
                    Boolean.TRUE.equals(dto.getHasAC())
            );
        }

        if (dto.getHasWIFI() != null) {
            room.setHasWIFI(
                    Boolean.TRUE.equals(dto.getHasWIFI())
            );
        }

        if (dto.getIsFurnished() != null) {
            room.setFurnished(
                    Boolean.TRUE.equals(
                            dto.getIsFurnished()
                    )
            );
        }

        if (dto.getKitchen() != null) {
            room.setKitchen(clean(dto.getKitchen()));
        }

        if (dto.getDescription() != null) {
            room.setDescription(
                    clean(dto.getDescription())
            );
        }
    }

    private void validateCreateRoomRequest(
            RoomSaveRequestDTO dto,
            Long landlordId
    ) {

        if (dto == null) {
            throw fieldError("data", "Room request is required.");
        }

        Map<String, String> errors = new LinkedHashMap<>();

        if (landlordId == null || landlordId <= 0) {
            errors.put("landlordId", "Authenticated landlord id is invalid.");
        }

        required(errors, "roomNumber", dto.getRoomNumber());

        required(errors, "size", dto.getSize());

        required(errors, "statusCode", dto.getStatusCode());

        required(errors, "tenantType", dto.getTenantType());

        required(errors, "rentTimeType", dto.getRentTimeType());

        required(errors, "kitchen", dto.getKitchen());

        if (dto.getPrice() == null) {
            errors.put("price", "Room price is required.");
        } else if (dto.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            errors.put("price", "Room price cannot be negative.");
        }

        validateParentFields(dto, errors);

        if (!errors.isEmpty()) {
            throw new FieldValidationException("Please complete all required room fields correctly.", errors);
        }
    }

    private void validateUpdateRoomRequest(
            RoomSaveRequestDTO dto
    ) {

        Map<String, String> errors =
                new LinkedHashMap<>();

        if (dto.getRoomNumber() != null
                && isBlank(dto.getRoomNumber())) {
            errors.put(
                    "roomNumber",
                    "Room number cannot be empty."
            );
        }

        if (dto.getPrice() != null
                && dto.getPrice()
                .compareTo(BigDecimal.ZERO) < 0) {
            errors.put(
                    "price",
                    "Room price cannot be negative."
            );
        }

        if (hasAnyParentField(dto)) {
            validateParentFields(dto, errors);
        }

        if (!errors.isEmpty()) {
            throw new FieldValidationException(
                    "Please correct the room fields.",
                    errors
            );
        }
    }

    private void validateParentFieldsOrThrow(
            RoomSaveRequestDTO dto
    ) {

        Map<String, String> errors =
                new LinkedHashMap<>();

        validateParentFields(dto, errors);

        if (!errors.isEmpty()) {
            throw new FieldValidationException(
                    "Room parent is invalid.",
                    errors
            );
        }
    }

    private void validateParentFields(
            RoomSaveRequestDTO dto,
            Map<String, String> errors
    ) {

        if (isValidParentRequest(dto)) {
            return;
        }

        errors.put(
                "parent",
                "Room must belong to a building directly, " +
                        "a building flat, a home directly, " +
                        "or a home flat."
        );
    }

    private boolean isValidParentRequest(
            RoomSaveRequestDTO dto
    ) {
        return isDirectBuildingRoom(dto)
                || isBuildingFlatRoom(dto)
                || isDirectHomeRoom(dto)
                || isHomeFlatRoom(dto);
    }

    private boolean isDirectBuildingRoom(
            RoomSaveRequestDTO dto
    ) {
        return dto.getBuildingId() != null
                && dto.getFlatId() == null
                && dto.getHomeId() == null;
    }

    private boolean isBuildingFlatRoom(
            RoomSaveRequestDTO dto
    ) {
        return dto.getBuildingId() != null
                && dto.getFlatId() != null
                && dto.getHomeId() == null;
    }

    private boolean isDirectHomeRoom(
            RoomSaveRequestDTO dto
    ) {
        return dto.getBuildingId() == null
                && dto.getFlatId() == null
                && dto.getHomeId() != null;
    }

    private boolean isHomeFlatRoom(
            RoomSaveRequestDTO dto
    ) {
        return dto.getBuildingId() == null
                && dto.getFlatId() != null
                && dto.getHomeId() != null;
    }

    private boolean hasAnyParentField(
            RoomSaveRequestDTO dto
    ) {
        return dto.getBuildingId() != null
                || dto.getFlatId() != null
                || dto.getHomeId() != null;
    }

    private void validateDirectBuildingRoomNumber(
            Long buildingId,
            String roomNumber,
            Long currentRoomId
    ) {

        boolean exists =
                roomRepository
                        .existsByBuildingIdAndFlatIsNullAndHomeIsNullAndRoomNumberIgnoreCaseAndDeletedFalse(
                                buildingId,
                                roomNumber
                        );

        if (!exists) {
            return;
        }

        EntityRooms current =
                currentRoomId == null
                        ? null
                        : roomRepository
                        .findDetailsById(currentRoomId);

        boolean sameRoom =
                current != null
                        && current.getBuilding() != null
                        && current.getFlat() == null
                        && current.getHome() == null
                        && Objects.equals(
                        current.getBuilding().getId(),
                        buildingId
                )
                        && roomNumber.equalsIgnoreCase(
                        current.getRoomNumber()
                );

        if (!sameRoom) {
            throw fieldError(
                    "roomNumber",
                    "Room number already exists directly in this building."
            );
        }
    }

    private void validateDirectHomeRoomNumber(
            Long homeId,
            String roomNumber,
            Long currentRoomId
    ) {

        boolean exists =
                roomRepository
                        .existsByHomeIdAndFlatIsNullAndBuildingIsNullAndRoomNumberIgnoreCaseAndDeletedFalse(
                                homeId,
                                roomNumber
                        );

        if (!exists) {
            return;
        }

        EntityRooms current =
                currentRoomId == null
                        ? null
                        : roomRepository
                        .findDetailsById(currentRoomId);

        boolean sameRoom =
                current != null
                        && current.getHome() != null
                        && current.getFlat() == null
                        && current.getBuilding() == null
                        && Objects.equals(
                        current.getHome().getId(),
                        homeId
                )
                        && roomNumber.equalsIgnoreCase(
                        current.getRoomNumber()
                );

        if (!sameRoom) {
            throw fieldError(
                    "roomNumber",
                    "Room number already exists directly in this home."
            );
        }
    }

    private void validateFlatRoomNumber(
            Long flatId,
            String roomNumber,
            Long currentRoomId
    ) {

        boolean exists =
                roomRepository
                        .existsByFlatIdAndRoomNumberIgnoreCaseAndDeletedFalse(
                                flatId,
                                roomNumber
                        );

        if (!exists) {
            return;
        }

        EntityRooms current =
                currentRoomId == null
                        ? null
                        : roomRepository
                        .findDetailsById(currentRoomId);

        boolean sameRoom =
                current != null
                        && current.getFlat() != null
                        && Objects.equals(
                        current.getFlat().getId(),
                        flatId
                )
                        && roomNumber.equalsIgnoreCase(
                        current.getRoomNumber()
                );

        if (!sameRoom) {
            throw fieldError(
                    "roomNumber",
                    "Room number already exists in this flat."
            );
        }
    }

    private void validateRoomOwnerAccess(
            EntityRooms room,
            Long landlordId
    ) {

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

    private EntityBuildings getBuilding(
            Long buildingId
    ) {

        validateId(buildingId, "buildingId");

        return buildingRepository
                .findById(buildingId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Building not found with id: "
                                        + buildingId
                        )
                );
    }

    private EntityHomes getHome(Long homeId) {

        validateId(homeId, "homeId");

        EntityHomes home = homeRepository.findByIdForUpdate(homeId).orElse(null);

        if (home == null) {
            throw new EntityNotFoundException("Home not found with id: " + homeId);
        }

        return home;
    }

    private EntityFlats getFlat(Long flatId) {

        validateId(flatId, "flatId");

        EntityFlats flat =
                flatsRepository.findByIdForUpdate(flatId).orElse(null);

        if (flat == null) {
            throw new EntityNotFoundException(
                    "Flat not found with id: "
                            + flatId
            );
        }

        return flat;
    }

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

    private EntityUnitStatus resolveRoomStatus(
            String code
    ) {
        if (isBlank(code)) {
            return unitStatusService.getDefaultStatus(StatusDomain.ROOM);
        }

        // Create and direct edit requests may only submit setup-level statuses.
        return unitStatusService.getRequiredSetupStatus(StatusDomain.ROOM, code);
    }

    private EntityUnitStatus requiredStatus(
            StatusDomain domain,
            String code
    ) {
        return unitStatusService.getRequiredStatus(
                domain,
                normalizeStatus(code)
        );
    }

    private long countByStatus(
            List<EntityRooms> rooms,
            String expectedStatus
    ) {
        return rooms.stream()
                .filter(room -> room.getStatus() != null)
                .filter(room ->
                        expectedStatus.equalsIgnoreCase(
                                room.getStatus().getCode()
                        )
                )
                .count();
    }

    private boolean isDerivedRoomStatus(
            String code
    ) {
        return AVAILABLE_AS_ROOMS
                .equalsIgnoreCase(code)
                || PARTIALLY_RENTED_AS_ROOMS
                .equalsIgnoreCase(code)
                || FULLY_RENTED_AS_ROOMS
                .equalsIgnoreCase(code);
    }

    private String statusCode(
            EntityUnitStatus status
    ) {
        return status == null
                ? null
                : status.getCode();
    }

    private void activateRoom(EntityRooms room) {
        room.setActive(true);
        room.setDeleted(false);
        room.setDeletedAt(null);
    }

    private String cleanRoomNumber(
            String value
    ) {

        String cleaned = cleanRequired(value, "Room number is required.");

        /*
         * Allows old generated values to be cleaned during migration.
         */
        if (cleaned.contains("_R-")) {
            return cleaned.substring(cleaned.lastIndexOf("_R-") + 3);
        }

        return cleaned;
    }

    private void validateAuthenticatedLandlord(
            Long landlordId
    ) {
        if (landlordId == null || landlordId <= 0) {
            throw new AccessDeniedException(
                    "Authenticated landlord id is required."
            );
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

    private void required(
            Map<String, String> errors,
            String field,
            String value
    ) {
        if (isBlank(value)) {
            errors.put(field, field + " is required.");
        }
    }

    private String normalizeStatus(String value) {
        return cleanRequired(
                value,
                "Status is required."
        ).toUpperCase(Locale.ROOT);
    }

    private String cleanRequired(
            String value,
            String message
    ) {

        String cleaned = clean(value);

        if (cleaned == null) {
            throw fieldError(
                    "value",
                    message
            );
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

    private boolean isBlank(String value) {
        return value == null
                || value.trim().isEmpty();
    }

    private FieldValidationException fieldError(
            String field,
            String message
    ) {
        return new FieldValidationException(
                message,
                Map.of(field, message)
        );
    }

    private Long idOf(EntityFlats flat) {
        return flat == null ? null : flat.getId();
    }

    private Long resolveHomeId(EntityRooms room) {
        if (room == null) return null;
        if (room.getHome() != null) return room.getHome().getId();
        if (room.getFlat() != null && room.getFlat().getHome() != null) {
            return room.getFlat().getHome().getId();
        }
        return null;
    }

    /*
     * Keep your existing RoomDetailsDTO mapping if its builder fields
     * differ. The important parent location resolution is:
     *
     * EntityBuildings locationBuilding =
     *     room.getBuilding() != null
     *         ? room.getBuilding()
     *         : room.getFlat() != null
     *             ? room.getFlat().getBuilding()
     *             : null;
     *
     * EntityHomes locationHome =
     *     room.getHome() != null
     *         ? room.getHome()
     *         : room.getFlat() != null
     *             ? room.getFlat().getHome()
     *             : null;
     */
    private RoomDetailsDTO toDTO(EntityRooms room) {

        EntityBuildings building = room.getBuilding();

        EntityHomes home = room.getHome();

        EntityFlats flat = room.getFlat();

        EntityBuildings locationBuilding =
                building != null
                        ? building
                        : flat != null
                        ? flat.getBuilding()
                        : null;

        EntityHomes locationHome =
                home != null
                        ? home
                        : flat != null
                        ? flat.getHome()
                        : null;

        List<String> images = propertyMediaService.getImages(PropertyType.ROOM, room.getId())
                        .stream()
                        .map(PropertyMediaDTO::getFileUrl)
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
                .buildingId(
                        building != null
                                ? building.getId()
                                : null
                )
                .flatId(
                        flat != null
                                ? flat.getId()
                                : null
                )
                .homeId(
                        home != null
                                ? home.getId()
                                : null
                )
                .buildingName(
                        locationBuilding != null
                                ? locationBuilding
                                .getBuildingName()
                                : null
                )
                .buildingNumber(
                        locationBuilding != null
                                ? locationBuilding
                                .getBuildingNumber()
                                : null
                )
                .flatLabel(
                        flat != null
                                ? "Flat "
                                + flat.getFlatNumber()
                                : null
                )
                .homeLabel(
                        locationHome != null
                                ? locationHome.getHomeType()+"."+locationHome.getHomeNumber()
                                : null
                )
                .fromFlat(flat != null)
                .fromHome(locationHome != null)
                .images(
                        images == null
                                ? Collections.emptyList()
                                : images
                )
                .build();
    }

    private RoomStatusDTO toStatusDTO(
            EntityUnitStatus status
    ) {

        if (status == null) {
            return null;
        }

        return RoomStatusDTO.builder()
                .id(status.getId())
                .statusDomain(
                        status.getStatusDomain()
                )
                .code(status.getCode())
                .nameEn(status.getNameEn())
                .nameAr(status.getNameAr())
                .colorCode(status.getColorCode())
                .icon(status.getIcon())
                .build();
    }

    /**
     * Refreshes all old/new Flat and Home parents after Room create, move,
     * status update or soft-delete. Building-only paths remain unchanged.
     */
    private void refreshAffectedParents(
            Long oldFlatId,
            Long newFlatId,
            Long oldHomeId,
            Long newHomeId
    ) {
        Set<Long> flatIds = new LinkedHashSet<>();
        if (oldFlatId != null && oldFlatId > 0) flatIds.add(oldFlatId);
        if (newFlatId != null && newFlatId > 0) flatIds.add(newFlatId);

        for (Long flatId : flatIds) {
            flatsService.refreshFlatState(flatId);
        }

        Set<Long> homeIds = new LinkedHashSet<>();
        if (oldHomeId != null && oldHomeId > 0) homeIds.add(oldHomeId);
        if (newHomeId != null && newHomeId > 0) homeIds.add(newHomeId);

        for (Long homeId : homeIds) {
            homeService.refreshHomeState(homeId);
        }
    }

}
