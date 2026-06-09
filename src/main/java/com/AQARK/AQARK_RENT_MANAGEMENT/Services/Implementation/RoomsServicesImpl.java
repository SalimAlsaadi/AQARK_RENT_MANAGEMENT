package com.AQARK.AQARK_RENT_MANAGEMENT.Services.Implementation;

import com.AQARK.AQARK_RENT_MANAGEMENT.Common_Utilities.PropertyMediaService;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.MediaDTO.PropertyMediaDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.RoomDTO.RoomDetailsDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.RoomDTO.RoomSaveRequestDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.*;
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
    private final PropertyMediaService propertyMediaService;

    @Override
    @Transactional
    public RoomDetailsDTO createRoom(
            RoomSaveRequestDTO dto,
            List<MultipartFile> images
    ) throws IOException {

        validateCreateRequest(dto);

        EntityRooms room = new EntityRooms();

        room.setRoomName(dto.getRoomName());
        room.setSize(dto.getSize());
        room.setRented(dto.isRented());
        room.setTenantType(dto.getTenantType());
        room.setRentalType(dto.getRentalType());
        room.setPrice(dto.getPrice());
        room.setHasPrivateBathroom(dto.isHasPrivateBathroom());
        room.setDescription(dto.getDescription());

        if (dto.getFlatId() != null) {
            EntityFlats flat = flatsRepository.findById(dto.getFlatId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Flat not found with ID: " + dto.getFlatId()
                    ));

            room.setFlat(flat);
            room.setHome(null);
        }

        if (dto.getHomeId() != null) {
            EntityHomes home = homeRepository.findById(dto.getHomeId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Home not found with ID: " + dto.getHomeId()
                    ));

            room.setHome(home);
            room.setFlat(null);
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
    public RoomDetailsDTO updateRoom(
            Long roomId,
            RoomSaveRequestDTO dto,
            List<MultipartFile> newImages,
            List<Long> imageIdsToDelete
    ) throws IOException {

        if (roomId == null || roomId <= 0) {
            throw new IllegalArgumentException("Room ID is required.");
        }

        EntityRooms room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Room not found with ID: " + roomId
                ));

        updateRoomFields(room, dto);

        if (dto.getFlatId() != null && dto.getHomeId() != null) {
            throw new IllegalArgumentException("Room cannot belong to both flat and home.");
        }

        if (dto.getFlatId() != null) {
            EntityFlats flat = flatsRepository.findById(dto.getFlatId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Flat not found with ID: " + dto.getFlatId()
                    ));

            room.setFlat(flat);
            room.setHome(null);
        }

        if (dto.getHomeId() != null) {
            EntityHomes home = homeRepository.findById(dto.getHomeId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Home not found with ID: " + dto.getHomeId()
                    ));

            room.setHome(home);
            room.setFlat(null);
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

        EntityRooms room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Room not found with ID: " + roomId
                ));

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
    public List<RoomDetailsDTO> getRoomsByFlatId(Long flatId) {

        if (flatId == null || flatId <= 0) {
            throw new IllegalArgumentException("Flat ID is required.");
        }

        return roomRepository.findByFlatId(flatId)
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Override
    public List<RoomDetailsDTO> getRoomsByHomeId(Long homeId) {

        if (homeId == null || homeId <= 0) {
            throw new IllegalArgumentException("Home ID is required.");
        }

        return roomRepository.findByHomeId(homeId)
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Override
    @Transactional
    public void deleteRoom(Long roomId) {

        EntityRooms room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Room not found with ID: " + roomId
                ));

        propertyMediaService.softDeleteAllImages(
                MediaOwnerType.ROOM,
                room.getId()
        );

        roomRepository.delete(room);
    }

    private void updateRoomFields(
            EntityRooms room,
            RoomSaveRequestDTO dto
    ) {

        if (dto.getRoomName() != null) {
            room.setRoomName(dto.getRoomName());
        }

        if (dto.getSize() != null) {
            room.setSize(dto.getSize());
        }

        room.setRented(dto.isRented());

        if (dto.getTenantType() != null) {
            room.setTenantType(dto.getTenantType());
        }

        if (dto.getRentalType() != null) {
            room.setRentalType(dto.getRentalType());
        }

        if (dto.getPrice() != null) {
            room.setPrice(dto.getPrice());
        }

        room.setHasPrivateBathroom(dto.isHasPrivateBathroom());

        if (dto.getDescription() != null) {
            room.setDescription(dto.getDescription());
        }
    }

    private RoomDetailsDTO convertToDTO(EntityRooms room) {

        EntityFlats flat = room.getFlat();
        EntityHomes home = room.getHome();

        EntityBuildings building =
                flat != null ? flat.getBuilding() : null;

        List<PropertyMediaDTO> images = propertyMediaService.getImages(
                MediaOwnerType.ROOM,
                room.getId()
        );

        boolean fromFlat = flat != null;

        return RoomDetailsDTO.builder()
                .id(room.getId())

                .roomName(room.getRoomName())
                .size(room.getSize())

                .rented(room.isRented())
                .tenantType(room.getTenantType())
                .rentalType(room.getRentalType())

                .price(room.getPrice())

                .hasPrivateBathroom(room.isHasPrivateBathroom())
                .description(room.getDescription())

                .region(fromFlat ? flat.getBuilding().getRegion() : home != null ? home.getRegion() : null)
                .wilaya(fromFlat ? flat.getBuilding().getWilaya() : home != null ? home.getWilaya() : null)
                .location(fromFlat
                        ? "Flat ID: " + flat.getId()
                        : home != null ? home.getLocation() : null)

                .latitude(fromFlat ? flat.getBuilding().getLatitude() : home != null ? home.getLatitude() : null)
                .longitude(fromFlat ? flat.getBuilding().getLongitude() : home != null ? home.getLongitude() : null)

                .fromFlat(fromFlat)
                .flatId(flat != null ? flat.getId() : null)
                .homeId(home != null ? home.getId() : null)
                .buildingId(building != null ? building.getId() : null)

                .buildingName(building != null ? building.getBuildingName() : null)
                .flatLabel(flat != null ? "Flat " + flat.getId() : null)
                .homeLabel(home != null ? "Home " + home.getId() : null)

                .images(images != null ? images : Collections.emptyList())
                .build();
    }

    private void validateCreateRequest(RoomSaveRequestDTO dto) {

        if (dto == null) {
            throw new IllegalArgumentException("Room request body is required.");
        }

        boolean hasFlat = dto.getFlatId() != null;
        boolean hasHome = dto.getHomeId() != null;

        if (hasFlat == hasHome) {
            throw new IllegalArgumentException(
                    "Room must belong to either a flat or a home, not both or neither."
            );
        }

        if (dto.getPrice() == null) {
            throw new IllegalArgumentException("Room price is required.");
        }

        if (dto.getRentalType() == null || dto.getRentalType().isBlank()) {
            throw new IllegalArgumentException("Rental type is required.");
        }

        if (dto.getTenantType() == null || dto.getTenantType().isBlank()) {
            throw new IllegalArgumentException("Tenant type is required.");
        }
    }
}




//package com.AQARK.AQARK_RENT_MANAGEMENT.Services.Implementation;
//
//import com.AQARK.AQARK_RENT_MANAGEMENT.Common_Utilities.FolderProperties;
//import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.RoomDTO.RoomDetailsDTO;
//import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.RoomDTO.RoomSaveRequestDTO;
//import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityFlats;
//import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityHomes;
//import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityRooms;
//import com.AQARK.AQARK_RENT_MANAGEMENT.Repositories.FlatsRepository;
//import com.AQARK.AQARK_RENT_MANAGEMENT.Repositories.HomeRepository;
//import com.AQARK.AQARK_RENT_MANAGEMENT.Repositories.RoomRepository;
//import com.AQARK.AQARK_RENT_MANAGEMENT.Common_Utilities.ImageService;
//
//import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface.RoomsServicesInterface;
//import jakarta.transaction.Transactional;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.IOException;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.util.List;
//
//@Service
//public class RoomsServicesImpl implements RoomsServicesInterface {
//
//    private final RoomRepository roomRepository;
//    private final FlatsRepository flatsRepository;
//    private final HomeRepository homeRepository;
//    private final ImageService imageService;
//    private final FolderProperties folder;
//
//    @Value("${room.link.src}")
//    private String room_link;
//
//    @Autowired
//    public RoomsServicesImpl(RoomRepository roomRepository,
//                             FlatsRepository flatsRepository,
//                             HomeRepository homeRepository,
//                             ImageService imageService, FolderProperties folderProperties) {
//        this.roomRepository = roomRepository;
//        this.flatsRepository = flatsRepository;
//        this.homeRepository = homeRepository;
//        this.imageService = imageService;
//        this.folder = folderProperties;
//    }
//
//    @Override
//    public String saveRoom(RoomSaveRequestDTO dto, List<MultipartFile> images) throws IOException {
//        EntityRooms room = new EntityRooms();
//        room.setRoomName(dto.getRoomName());
//        room.setSize(dto.getSize());
//        room.setRentalType(dto.getRentalType());
//        room.setDescription(dto.getDescription());
//        room.setPrice(dto.getPrice());
//        room.setRented(dto.isRented());
//        room.setTenantType(dto.getTenantType());
//        room.setHasPrivateBathroom(dto.isHasPrivateBathroom());
//
//
//        if (dto.getFlatId() != null) {
//            EntityFlats flat = flatsRepository.findById(dto.getFlatId())
//                    .orElseThrow(() -> new IllegalArgumentException("Flat not found: " + dto.getFlatId()));
//            room.setFlat(flat);
//        }
//
//        if (dto.getHomeId() != null) {
//            EntityHomes home = homeRepository.findById(dto.getHomeId())
//                    .orElseThrow(() -> new IllegalArgumentException("Home not found: " + dto.getHomeId()));
//            room.setHome(home);
//        }
//
//        room = roomRepository.save(room);
//
//        if (images != null && !images.isEmpty()) {
//            String baseFolder = folder.getRooms();
//            String subFolder;
//
//            if (room.getFlat() != null) {
//                Long buildingId = room.getFlat().getBuilding().getId();
//                Long flatId = room.getFlat().getId();
//                Long roomId = room.getId();
//
//                     subFolder = folder.getBuilding() + "_" + buildingId + "/" +
//                                 folder.getFlat() + "_" + flatId + "/" +
//                                 folder.getRooms() + "_" + roomId;
//
//            } else if (room.getHome() != null) {
//                Long homeId = room.getHome().getId();
//                Long roomId = room.getId();
//
//                subFolder = folder.getHome() + "_" + homeId + "/" +
//                            folder.getRooms() + "_" + roomId;
//
//            } else {
//                throw new IllegalStateException("Room must belong to either a flat or a home.");
//            }
//
//            List<String> imagePaths = imageService.saveImages(images, baseFolder, subFolder);
//            room.setPicturePaths(imagePaths);
//            roomRepository.save(room); // update with image paths
//        }
//
//
//
//        return "Room saved successfully.";
//    }
//
//    @Override
//    public RoomDetailsDTO getRoomById(Long id) {
//        EntityRooms room = roomRepository.findById(id)
//                .orElseThrow(() -> new RuntimeException("Room not found with id: " + id));
//
//        RoomDetailsDTO dto = new RoomDetailsDTO();
//        dto.setId(room.getId());
//        dto.setRentalType(room.getRentalType());
//        dto.setDescription(room.getDescription());
//        dto.setPrice(room.getPrice());
//        dto.setRented(room.isRented());
//        dto.setTenantType(room.getTenantType());
//
//        List<String> imageUrls = room.getPicturePaths().stream()
//                .map(path -> {
//                    Path p = Paths.get(path);
//                    String fileName = p.getFileName().toString();
//                    return room_link + fileName;
//                }).toList();
//
//        dto.setPictureUrls(imageUrls);
//
//        return dto;
//    }
//
//    @Override
//    public List<RoomDetailsDTO> getAllRooms() {
//        return roomRepository.findAll().stream().map(this::convertToDTO).toList();
//    }
//
//    @Override
//    public String updateRoom(Long roomId, RoomSaveRequestDTO dto, List<MultipartFile> newImages, List<String> imagesToDelete) throws IOException {
//        EntityRooms room = roomRepository.findById(roomId)
//                .orElseThrow(() -> new RuntimeException("Room not found"));
//
//        room.setRentalType(dto.getRentalType());
//        room.setDescription(dto.getDescription());
//        room.setPrice(dto.getPrice());
//        room.setRented(dto.isRented());
//        room.setTenantType(dto.getTenantType());
//
//        List<String> updated = imageService.updateImages(
//                room.getPicturePaths(), imagesToDelete, newImages, folder.getRooms(),
//                room.getFlat() != null ? "flat_" + room.getFlat().getId() : "home_" + room.getHome().getId()
//        );
//        room.setPicturePaths(updated);
//
//        roomRepository.save(room);
//        return "Room updated successfully.";
//    }
//
//
//    @Override
//    public String deleteRoom(Long roomId) {
//        EntityRooms room = roomRepository.findById(roomId)
//                .orElseThrow(() -> new RuntimeException("Room not found"));
//
//        String folderPath;
//        String baseFolderPath;
//        Long parentId;
//
//        // Build folder structure
//        if (room.getFlat() != null) {
//            Long buildingId = room.getFlat().getBuilding().getId();
//            parentId = room.getFlat().getId();
//
//            // e.g., building_3/flat_2/room_10
//            baseFolderPath = folder.getBuilding() + "_" + buildingId + "/" + folder.getFlat() + "_" + parentId;
//            folderPath = baseFolderPath + "/" + folder.getRooms() + "_" + room.getId();
//
//        } else if (room.getHome() != null) {
//            parentId = room.getHome().getId();
//
//            // e.g., home_5/room_12
//            baseFolderPath = folder.getHome() + "_" + parentId;
//            folderPath = baseFolderPath + "/" + folder.getRooms() + "_" + room.getId();
//
//        } else {
//            throw new IllegalStateException("Room must belong to either a flat or a home.");
//        }
//
//        // Step 1: Delete the room's image folder
//        imageService.deleteAllImages(folder.getRooms(), folderPath);
//
//        // Step 2: Delete the room from the database
//        roomRepository.delete(room);
//
//        // Step 3: Check if any rooms remain under the same flat or home
//        boolean hasRemainingRooms = (room.getFlat() != null)
//                ? roomRepository.existsByFlatId(parentId)
//                : roomRepository.existsByHomeId(parentId);
//
//        // Step 4: If no rooms left, delete the parent folder
//        if (!hasRemainingRooms) {
//            imageService.deleteFolderIfEmpty(folder.getRooms(), baseFolderPath);
//        }
//
//        return "Room with ID " + roomId + " has been deleted.";
//    }
//
//
//    private RoomDetailsDTO convertToDTO(EntityRooms room) {
//        RoomDetailsDTO dto = new RoomDetailsDTO();
//        dto.setId(room.getId());
//        dto.setRentalType(room.getRentalType());
//        dto.setDescription(room.getDescription());
//        dto.setPrice(room.getPrice());
//        dto.setRented(room.isRented());
//        dto.setTenantType(room.getTenantType());
//
//        List<String> imageUrls = room.getPicturePaths().stream()
//                .map(path -> {
//                    Path p = Paths.get(path);
//                    String fileName = p.getFileName().toString();
//                    return room_link + fileName;
//                }).toList();
//
//        dto.setPictureUrls(imageUrls);
//        return dto;
//    }
//}
