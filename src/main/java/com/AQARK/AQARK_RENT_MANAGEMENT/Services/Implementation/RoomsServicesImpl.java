package com.AQARK.AQARK_RENT_MANAGEMENT.Services.Implementation;

import com.AQARK.AQARK_RENT_MANAGEMENT.Common_Utilities.PropertyMediaService;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.MediaDTO.PropertyMediaDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.RoomDTO.RoomDetailsDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.RoomDTO.RoomSaveRequestDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.RoomDTO.RoomStatusDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.*;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.StatusDomain;
import com.AQARK.AQARK_RENT_MANAGEMENT.Repositories.*;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface.RoomsServicesInterface;
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
public class RoomsServicesImpl implements RoomsServicesInterface {

    private static final String FLAT_STATUS_AVAILABLE = "AVAILABLE";
    private static final String FLAT_STATUS_AVAILABLE_AS_ROOMS = "AVAILABLE_AS_ROOMS";
    private static final String FLAT_STATUS_RENTED = "RENTED";
    private static final String FLAT_STATUS_RESERVED = "RESERVED";
    private static final String FLAT_STATUS_UNDER_MAINTENANCE = "UNDER_MAINTENANCE";
    private static final String FLAT_STATUS_BLOCKED = "BLOCKED";

    private final RoomRepository roomRepository;
    private final FlatsRepository flatsRepository;
    private final HomeRepository homeRepository;
    private final BuildingRepository buildingRepository;
    private final PropertyMediaService propertyMediaService;
    private final UnitStatusServiceInterface unitStatusService;

    @Override
    @Transactional
    public RoomDetailsDTO createRoom(RoomSaveRequestDTO dto, List<MultipartFile> images) throws IOException {

        validateCreateRoomRequest(dto);

        EntityRooms room = new EntityRooms();

        applyRoomFields(room, dto);

        room.setStatus(resolveRoomStatus(dto.getStatusCode()));

        if (isFlatRoomRequest(dto)) {
            assignRoomToFlat(
                    room,
                    dto.getBuildingId(),
                    dto.getFlatId()
            );
        }

        if (isHomeRoomRequest(dto)) {
            assignRoomToHome(
                    room,
                    dto.getHomeId()
            );
        }

        EntityRooms savedRoom = roomRepository.save(room);

        if (images != null && !images.isEmpty()) {
            propertyMediaService.saveImages(
                    images,
                    MediaOwnerType.ROOM,
                    savedRoom.getId()
            );
        }

        EntityRooms loadedRoom = roomRepository.findDetailsById(savedRoom.getId());

        return toDTO(loadedRoom);
    }

    @Override
    @Transactional
    public RoomDetailsDTO updateRoom(Long roomId, RoomSaveRequestDTO dto, List<MultipartFile> newImages, List<Long> imageIdsToDelete) throws IOException {

        validateId(roomId, "Room id");

        if (dto == null) {
            throw new IllegalArgumentException("Room request is required.");
        }

        EntityRooms room = roomRepository.findDetailsById(roomId);

        if (room == null) {
            throw new EntityNotFoundException("Room not found with id: " + roomId);
        }

        EntityFlats oldFlat = room.getFlat();

        applyRoomFields(room, dto);

        if (dto.getStatusCode() != null && !dto.getStatusCode().isBlank()) {
            room.setStatus(resolveRoomStatus(dto.getStatusCode()));
        }

        if (hasAnyParentField(dto)) {
            if (!isValidParentRequest(dto)) {
                throw new IllegalArgumentException(
                        "Room must belong either to building + flat, or to home only."
                );
            }

            if (isFlatRoomRequest(dto)) {
                assignRoomToFlat(room, dto.getBuildingId(), dto.getFlatId());
            }

            if (isHomeRoomRequest(dto)) {
                assignRoomToHome(room, dto.getHomeId());
            }
        }

        EntityRooms savedRoom = roomRepository.save(room);

        propertyMediaService.updateImages(
                MediaOwnerType.ROOM,
                savedRoom.getId(),
                imageIdsToDelete,
                newImages
        );

        refreshFlatStatusAfterRoomMove(oldFlat, savedRoom.getFlat());

        EntityRooms loadedRoom = roomRepository.findDetailsById(savedRoom.getId());

        return toDTO(loadedRoom);
    }

    @Override
    public RoomDetailsDTO getRoomById(Long roomId) {
        validateId(roomId, "Room id");

        EntityRooms room = roomRepository.findDetailsById(roomId);

        if (room == null) {
            throw new EntityNotFoundException("Room not found with id: " + roomId);
        }

        return toDTO(room);
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
        validateId(buildingId, "Building id");

        return roomRepository.findDetailsByBuildingId(buildingId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public List<RoomDetailsDTO> getRoomsByFlatId(Long flatId) {
        validateId(flatId, "Flat id");

        return roomRepository.findDetailsByFlatId(flatId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public List<RoomDetailsDTO> getRoomsByHomeId(Long homeId) {
        validateId(homeId, "Home id");

        return roomRepository.findDetailsByHomeId(homeId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    @Transactional
    public void deleteRoom(Long roomId) {
        validateId(roomId, "Room id");

        EntityRooms room = roomRepository.findDetailsById(roomId);

        if (room == null) {
            throw new EntityNotFoundException("Room not found with id: " + roomId);
        }

        EntityFlats relatedFlat = room.getFlat();

        propertyMediaService.softDeleteAllImages(
                MediaOwnerType.ROOM,
                room.getId()
        );

        room.setDeleted(true);
        room.setActive(false);
        room.setDeletedAt(LocalDateTime.now());

        roomRepository.save(room);

        refreshFlatStatusAfterRoomDelete(relatedFlat);
    }

    private void applyRoomFields(EntityRooms room, RoomSaveRequestDTO dto) {
        if (dto.getRoomNumber() != null) {
            room.setRoomNumber(cleanRequired(dto.getRoomNumber(), "Room number is required."));
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
            if (dto.getPrice().signum() < 0) {
                throw new IllegalArgumentException("Room price cannot be negative.");
            }

            room.setPrice(dto.getPrice());
        }

        if (dto.getHasPrivateBathroom() != null) {
            room.setHasPrivateBathroom(Boolean.TRUE.equals(dto.getHasPrivateBathroom()));
        }

        if (dto.getHasAC() != null) {
            room.setHasAC(Boolean.TRUE.equals(dto.getHasAC()));
        }

        if (dto.getHasWIFI() != null) {
            room.setHasWIFI(Boolean.TRUE.equals(dto.getHasWIFI()));
        }

        if (dto.getKitchen() != null) {
            room.setKitchen(clean(dto.getKitchen()));
        }

        if (dto.getIsFurnished() != null) {
            room.setFurnished(Boolean.TRUE.equals(dto.getIsFurnished()));
        }

        if (dto.getDescription() != null) {
            room.setDescription(clean(dto.getDescription()));
        }
    }

    private void assignRoomToFlat(EntityRooms room, Long buildingId, Long flatId) {
        validateId(buildingId, "Building id");
        validateId(flatId, "Flat id");

        EntityBuildings building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Building not found with id: " + buildingId
                ));

        EntityFlats flat = flatsRepository.findDetailsById(flatId);

        if (flat == null) {
            throw new EntityNotFoundException("Flat not found with id: " + flatId);
        }

        if (flat.getBuilding() == null || !flat.getBuilding().getId().equals(building.getId())) {
            throw new IllegalArgumentException("Selected flat does not belong to selected building.");
        }

        validateFlatCanBeUsedAsRooms(flat);

        String cleanRoomNumber = extractCleanRoomNumber(room.getRoomNumber());

        validateRoomNumberAvailableInFlat(
                flat.getId(),
                cleanRoomNumber,
                room.getId()
        );

        room.setRoomNumber(
                "B-" + building.getId()
                        + "_F-" + flat.getFlatNumber()
                        + "_R-" + cleanRoomNumber
        );

        room.setBuilding(building);
        room.setFlat(flat);
        room.setHome(null);
        room.setActive(true);
        room.setDeleted(false);

        markFlatAsAvailableAsRooms(flat);
    }

    private void assignRoomToHome(EntityRooms room, Long homeId) {
        validateId(homeId, "Home id");

        EntityHomes home = homeRepository.findById(homeId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Home not found with id: " + homeId
                ));

        String cleanRoomNumber = extractCleanRoomNumber(room.getRoomNumber());

        validateRoomNumberAvailableInHome(
                home.getId(),
                cleanRoomNumber,
                room.getId()
        );

        room.setRoomNumber(
                "H-" + home.getId()
                        + "_R-" + cleanRoomNumber
        );

        room.setHome(home);
        room.setBuilding(null);
        room.setFlat(null);
        room.setActive(true);
        room.setDeleted(false);
    }

    private void validateFlatCanBeUsedAsRooms(EntityFlats flat) {
        String statusCode =
                flat.getStatus() != null
                        ? flat.getStatus().getCode()
                        : null;

        if (statusCode == null) {
            throw new IllegalStateException("Flat status is missing.");
        }

        if (FLAT_STATUS_AVAILABLE.equalsIgnoreCase(statusCode)) {
            return;
        }

        if (FLAT_STATUS_AVAILABLE_AS_ROOMS.equalsIgnoreCase(statusCode)) {
            return;
        }

        if (FLAT_STATUS_RENTED.equalsIgnoreCase(statusCode)) {
            throw new IllegalStateException(
                    "Cannot register rooms because this flat is rented as a whole flat."
            );
        }

        if (FLAT_STATUS_RESERVED.equalsIgnoreCase(statusCode)) {
            throw new IllegalStateException(
                    "Cannot register rooms because this flat is reserved as a whole flat."
            );
        }

        if (FLAT_STATUS_UNDER_MAINTENANCE.equalsIgnoreCase(statusCode)) {
            throw new IllegalStateException(
                    "Cannot register rooms because this flat is under maintenance."
            );
        }

        if (FLAT_STATUS_BLOCKED.equalsIgnoreCase(statusCode)) {
            throw new IllegalStateException(
                    "Cannot register rooms because this flat is blocked."
            );
        }

        throw new IllegalStateException(
                "Flat status does not allow room registration: " + statusCode
        );
    }

    private void markFlatAsAvailableAsRooms(EntityFlats flat) {
        EntityUnitStatus availableAsRoomsStatus =
                unitStatusService.getRequiredStatus(
                        StatusDomain.UNIT,
                        FLAT_STATUS_AVAILABLE_AS_ROOMS
                );

        flat.setStatus(availableAsRoomsStatus);

        flatsRepository.save(flat);
    }

    private void refreshFlatStatusAfterRoomDelete(EntityFlats flat) {
        if (flat == null) {
            return;
        }

        long remainingRooms =
                roomRepository.countByFlatIdAndDeletedFalse(flat.getId());

        if (remainingRooms > 0) {
            markFlatAsAvailableAsRooms(flat);
            return;
        }

        EntityUnitStatus availableStatus =
                unitStatusService.getRequiredStatus(
                        StatusDomain.UNIT,
                        FLAT_STATUS_AVAILABLE
                );

        flat.setStatus(availableStatus);

        flatsRepository.save(flat);
    }

    private void refreshFlatStatusAfterRoomMove(
            EntityFlats oldFlat,
            EntityFlats newFlat
    ) {
        if (oldFlat != null && (newFlat == null || !oldFlat.getId().equals(newFlat.getId()))) {
            refreshFlatStatusAfterRoomDelete(oldFlat);
        }

        if (newFlat != null) {
            markFlatAsAvailableAsRooms(newFlat);
        }
    }

    private EntityUnitStatus resolveRoomStatus(String statusCode) {
        if (statusCode == null || statusCode.isBlank()) {
            return unitStatusService.getDefaultUnitStatus();
        }

        return unitStatusService.getRequiredStatus(
                StatusDomain.UNIT,
                statusCode
        );
    }

    private void validateRoomNumberAvailableInFlat(
            Long flatId,
            String cleanRoomNumber,
            Long currentRoomId
    ) {
        String finalRoomNumber = cleanRequired(cleanRoomNumber, "Room number is required.");

        boolean exists = roomRepository.existsByFlatIdAndRoomNumberIgnoreCaseAndDeletedFalse(
                flatId,
                finalRoomNumber
        );

        if (!exists) {
            return;
        }

        if (currentRoomId == null) {
            throw new IllegalStateException("Room number already exists in this flat.");
        }

        EntityRooms currentRoom = roomRepository.findDetailsById(currentRoomId);

        if (
                currentRoom != null
                        && currentRoom.getFlat() != null
                        && currentRoom.getFlat().getId().equals(flatId)
                        && extractCleanRoomNumber(currentRoom.getRoomNumber()).equalsIgnoreCase(finalRoomNumber)
        ) {
            return;
        }

        throw new IllegalStateException("Room number already exists in this flat.");
    }

    private void validateRoomNumberAvailableInHome(
            Long homeId,
            String cleanRoomNumber,
            Long currentRoomId
    ) {
        String finalRoomNumber = cleanRequired(cleanRoomNumber, "Room number is required.");

        boolean exists = roomRepository.existsByHomeIdAndRoomNumberIgnoreCaseAndDeletedFalse(
                homeId,
                finalRoomNumber
        );

        if (!exists) {
            return;
        }

        if (currentRoomId == null) {
            throw new IllegalStateException("Room number already exists in this home.");
        }

        EntityRooms currentRoom = roomRepository.findDetailsById(currentRoomId);

        if (
                currentRoom != null
                        && currentRoom.getHome() != null
                        && currentRoom.getHome().getId().equals(homeId)
                        && extractCleanRoomNumber(currentRoom.getRoomNumber()).equalsIgnoreCase(finalRoomNumber)
        ) {
            return;
        }

        throw new IllegalStateException("Room number already exists in this home.");
    }

    private RoomDetailsDTO toDTO(EntityRooms room) {
        EntityBuildings building = room.getBuilding();
        EntityFlats flat = room.getFlat();
        EntityHomes home = room.getHome();

        boolean fromFlat = building != null && flat != null && home == null;
        boolean fromHome = building == null && flat == null && home != null;

        List<String> images = propertyMediaService
                .getImages(MediaOwnerType.ROOM, room.getId())
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

                .regionId(
                        fromFlat && building.getRegion() != null
                                ? building.getRegion().getId()
                                : null
                )
                .region(
                        fromFlat && building.getRegion() != null
                                ? building.getRegion().getNameEn()
                                : fromHome ? home.getRegion() : null
                )
                .regionAr(
                        fromFlat && building.getRegion() != null
                                ? building.getRegion().getNameAr()
                                : null
                )

                .wilayaId(
                        fromFlat && building.getWilaya() != null
                                ? building.getWilaya().getId()
                                : null
                )
                .wilaya(
                        fromFlat && building.getWilaya() != null
                                ? building.getWilaya().getNameEn()
                                : fromHome ? home.getWilaya() : null
                )
                .wilayaAr(
                        fromFlat && building.getWilaya() != null
                                ? building.getWilaya().getNameAr()
                                : null
                )

                .areaId(
                        fromFlat && building.getArea() != null
                                ? building.getArea().getId()
                                : null
                )
                .area(
                        fromFlat && building.getArea() != null
                                ? building.getArea().getNameEn()
                                : fromHome ? home.getLocation() : null
                )
                .areaAr(
                        fromFlat && building.getArea() != null
                                ? building.getArea().getNameAr()
                                : null
                )

                .latitude(fromFlat ? building.getLatitude() : fromHome ? home.getLatitude() : null)
                .longitude(fromFlat ? building.getLongitude() : fromHome ? home.getLongitude() : null)

                .fromFlat(fromFlat)
                .fromHome(fromHome)

                .buildingId(building != null ? building.getId() : null)
                .flatId(flat != null ? flat.getId() : null)
                .homeId(home != null ? home.getId() : null)

                .buildingName(building != null ? building.getBuildingName() : null)
                .buildingNumber(building != null ? building.getBuildingNumber() : null)
                .flatLabel(flat != null ? "Flat " + flat.getFlatNumber() : null)
                .homeLabel(home != null ? "Home " + home.getId() : null)

                .images(images != null ? images : Collections.emptyList())
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

    private void validateCreateRoomRequest(RoomSaveRequestDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Room request is required.");
        }

        if (isBlank(dto.getRoomNumber())) {
            throw new IllegalArgumentException("Room number is required.");
        }

        if (!isValidParentRequest(dto)) {
            throw new IllegalArgumentException("Room must belong either to building + flat, or to home only.");
        }

        if (dto.getPrice() != null && dto.getPrice().signum() < 0) {
            throw new IllegalArgumentException("Room price cannot be negative.");
        }
    }

    private boolean isValidParentRequest(RoomSaveRequestDTO dto) {
        return isFlatRoomRequest(dto) || isHomeRoomRequest(dto);
    }

    private boolean isFlatRoomRequest(RoomSaveRequestDTO dto) {
        return dto.getBuildingId() != null
                && dto.getFlatId() != null
                && dto.getHomeId() == null;
    }

    private boolean isHomeRoomRequest(RoomSaveRequestDTO dto) {
        return dto.getBuildingId() == null
                && dto.getFlatId() == null
                && dto.getHomeId() != null;
    }

    private boolean hasAnyParentField(RoomSaveRequestDTO dto) {
        return dto.getBuildingId() != null
                || dto.getFlatId() != null
                || dto.getHomeId() != null;
    }

    private String extractCleanRoomNumber(String roomNumber) {
        String value = clean(roomNumber);

        if (value == null) {
            throw new IllegalArgumentException("Room number is required.");
        }

        if (value.contains("_R-")) {
            return value.substring(value.lastIndexOf("_R-") + 3);
        }

        return value;
    }

    private void validateId(Long id, String fieldName) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
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