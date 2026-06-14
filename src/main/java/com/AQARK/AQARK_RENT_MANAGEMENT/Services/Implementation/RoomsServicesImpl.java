package com.AQARK.AQARK_RENT_MANAGEMENT.Services.Implementation;

import com.AQARK.AQARK_RENT_MANAGEMENT.Common_Utilities.PropertyMediaService;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.MediaDTO.PropertyMediaDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.RoomDTO.RoomDetailsDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.RoomDTO.RoomSaveRequestDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityBuildings;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityFlats;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityHomes;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityRooms;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.MediaOwnerType;
import com.AQARK.AQARK_RENT_MANAGEMENT.Repositories.BuildingRepository;
import com.AQARK.AQARK_RENT_MANAGEMENT.Repositories.FlatsRepository;
import com.AQARK.AQARK_RENT_MANAGEMENT.Repositories.HomeRepository;
import com.AQARK.AQARK_RENT_MANAGEMENT.Repositories.RoomRepository;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface.RoomsServicesInterface;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomsServicesImpl implements RoomsServicesInterface {

    private final RoomRepository roomRepository;
    private final FlatsRepository flatsRepository;
    private final HomeRepository homeRepository;
    private final BuildingRepository buildingRepository;
    private final PropertyMediaService propertyMediaService;

    @Override
    @Transactional
    public RoomDetailsDTO createRoom(RoomSaveRequestDTO dto, List<MultipartFile> images) throws IOException {

        validateCreateRoomRequest(dto);

        EntityRooms room = new EntityRooms();

        applyRoomFields(room, dto);

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

        return convertToDTO(savedRoom);
    }

    @Override
    @Transactional
    public RoomDetailsDTO updateRoom(Long roomId, RoomSaveRequestDTO dto, List<MultipartFile> newImages, List<Long> imageIdsToDelete) throws IOException {

        validateId(roomId, "Room ID");

        if (dto == null) {
            throw new IllegalArgumentException("Room request is required.");
        }

        EntityRooms room = roomRepository.findById(roomId).orElseThrow(() -> new IllegalArgumentException("Room not found with ID: " + roomId));

        applyRoomFields(room, dto);


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

        return convertToDTO(savedRoom);
    }

    @Override
    public RoomDetailsDTO getRoomById(Long roomId) {

        validateId(roomId, "Room ID");

        EntityRooms room = roomRepository.findById(roomId).orElseThrow(() -> new IllegalArgumentException("Room not found with ID: " + roomId));

        return convertToDTO(room);
    }

    @Override
    public List<RoomDetailsDTO> getAllRooms() {

        return roomRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Override
    public List<RoomDetailsDTO> getRoomsByBuildingId(Long buildingId) {

        validateId(buildingId, "Building ID");

        return roomRepository.findByBuildingId(buildingId)
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Override
    public List<RoomDetailsDTO> getRoomsByFlatId(Long flatId) {

        validateId(flatId, "Flat ID");

        return roomRepository.findByFlatId(flatId)
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Override
    public List<RoomDetailsDTO> getRoomsByHomeId(Long homeId) {

        validateId(homeId, "Home ID");

        return roomRepository.findByHomeId(homeId)
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Override
    @Transactional
    public void deleteRoom(Long roomId) {

        validateId(roomId, "Room ID");

        EntityRooms room = roomRepository.findById(roomId).orElseThrow(() -> new IllegalArgumentException("Room not found with ID: " + roomId));

        propertyMediaService.softDeleteAllImages(
                MediaOwnerType.ROOM,
                room.getId()
        );

        roomRepository.delete(room);
    }

    private void applyRoomFields(EntityRooms room, RoomSaveRequestDTO dto) {

        if (dto.getRoomNumber() != null) {
            room.setRoomNumber(trimToNull(dto.getRoomNumber()));
        }

        if (dto.getSize() != null) {
            room.setSize(trimToNull(dto.getSize()));
        }

        if (dto.getRented() != null) {
            room.setRented(Boolean.TRUE.equals(dto.getRented()));
        }

        if (dto.getTenantType() != null) {
            room.setTenantType(trimToNull(dto.getTenantType()));
        }

        if (dto.getRentTimeType() != null) {
            room.setRentTimeType(trimToNull(dto.getRentTimeType()));
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
            room.setKitchen(trimToNull(dto.getKitchen()));
        }

        if (dto.getIsFurnished() != null) {
            room.setFurnished(Boolean.TRUE.equals(dto.getIsFurnished()));
        }

        if (dto.getDescription() != null) {
            room.setDescription(trimToNull(dto.getDescription()));
        }
    }

    private void assignRoomToFlat(EntityRooms room, Long buildingId, Long flatId) {

        validateId(buildingId, "Building ID");
        validateId(flatId, "Flat ID");

        EntityBuildings building = buildingRepository.findById(buildingId).orElseThrow(() -> new IllegalArgumentException("Building not found with ID: " + buildingId));

        EntityFlats flat = flatsRepository.findById(flatId).orElseThrow(() -> new IllegalArgumentException("Flat not found with ID: " + flatId));

        if (flat.getBuilding() == null || !flat.getBuilding().getId().equals(building.getId())) {
            throw new IllegalArgumentException("Selected flat does not belong to selected building.");
        }

        String cleanRoomNumber = extractCleanRoomNumber(room.getRoomNumber());

        room.setRoomNumber(
                "B-" + building.getId()
                        + "_F-" + flat.getFlatNumber()
                        + "_R-" + cleanRoomNumber
        );

        room.setBuilding(building);
        room.setFlat(flat);
        room.setHome(null);
    }

    private void assignRoomToHome(EntityRooms room, Long homeId) {

        validateId(homeId, "Home ID");

        EntityHomes home = homeRepository.findById(homeId).orElseThrow(() -> new IllegalArgumentException("Home not found with ID: " + homeId));

        String cleanRoomNumber = extractCleanRoomNumber(room.getRoomNumber());

        room.setRoomNumber(
                "H-" + home.getId()
                        + "_R-" + cleanRoomNumber
        );

        room.setHome(home);
        room.setBuilding(null);
        room.setFlat(null);
    }

    private RoomDetailsDTO convertToDTO(EntityRooms room) {

        EntityBuildings building = room.getBuilding();
        EntityFlats flat = room.getFlat();
        EntityHomes home = room.getHome();

        boolean fromFlat = building != null && flat != null && home == null;

        boolean fromHome = building == null && flat == null && home != null;

        List<PropertyMediaDTO> images = propertyMediaService.getImages(
                MediaOwnerType.ROOM,
                room.getId()
        );

        String region = null;
        String wilaya = null;
        String area = null;

        Double latitude = null;
        Double longitude = null;

        if (fromFlat) {
            region = building.getRegion();
            wilaya = building.getWilaya();

            area = "Flat " + flat.getFlatNumber();

            latitude = building.getLatitude();
            longitude = building.getLongitude();
        }

        if (fromHome) {
            region = home.getRegion();
            wilaya = home.getWilaya();

            area = home.getLocation();

            latitude = home.getLatitude();
            longitude = home.getLongitude();
        }

        return RoomDetailsDTO.builder()
                .id(room.getId())

                .roomNumber(room.getRoomNumber())
                .size(room.getSize())

                .rented(room.isRented())
                .tenantType(room.getTenantType())
                .rentTimeType(room.getRentTimeType())

                .price(room.getPrice())

                .hasPrivateBathroom(room.isHasPrivateBathroom())
                .hasAC(room.isHasAC())
                .hasWIFI(room.isHasWIFI())
                .isFurnished(room.isFurnished())

                .kitchen(room.getKitchen())
                .description(room.getDescription())

                .region(region)
                .wilaya(wilaya)
                .area(area)

                .latitude(latitude)
                .longitude(longitude)

                .fromFlat(fromFlat)

                .buildingId(building != null ? building.getId() : null)
                .flatId(flat != null ? flat.getId() : null)
                .homeId(home != null ? home.getId() : null)

                .buildingName(building != null ? building.getBuildingName() : null)
                .flatLabel(flat != null ? "Flat " + flat.getFlatNumber() : null)
                .homeLabel(home != null ? "Home " + home.getId() : null)

                .images(images != null ? images : Collections.emptyList())

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

        return dto.getBuildingId() != null &&
                dto.getFlatId() != null &&
                dto.getHomeId() == null;
    }

    private boolean isHomeRoomRequest(RoomSaveRequestDTO dto) {

        return dto.getBuildingId() == null &&
                dto.getFlatId() == null &&
                dto.getHomeId() != null;
    }

    private boolean hasAnyParentField(RoomSaveRequestDTO dto) {

        return dto.getBuildingId() != null ||
                dto.getFlatId() != null ||
                dto.getHomeId() != null;
    }

    private String extractCleanRoomNumber(String roomNumber) {

        String value = trimToNull(roomNumber);

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

    private String trimToNull(String value) {

        if (value == null) {
            return null;
        }

        String trimmed = value.trim();

        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isBlank(String value) {

        return value == null || value.trim().isEmpty();
    }
}