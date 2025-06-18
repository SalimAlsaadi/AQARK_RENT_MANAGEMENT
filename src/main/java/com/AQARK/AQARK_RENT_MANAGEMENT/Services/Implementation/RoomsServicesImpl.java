package com.AQARK.AQARK_RENT_MANAGEMENT.Services.Implementation;

import com.AQARK.AQARK_RENT_MANAGEMENT.Common_Utilities.FolderProperties;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.RoomDTO.RoomDetailsDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.RoomDTO.RoomSaveRequestDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityFlats;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityHomes;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityRooms;
import com.AQARK.AQARK_RENT_MANAGEMENT.Repositories.FlatsRepository;
import com.AQARK.AQARK_RENT_MANAGEMENT.Repositories.HomeRepository;
import com.AQARK.AQARK_RENT_MANAGEMENT.Repositories.RoomRepository;
import com.AQARK.AQARK_RENT_MANAGEMENT.Common_Utilities.ImageService;

import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface.RoomsServicesInterface;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
public class RoomsServicesImpl implements RoomsServicesInterface {

    private final RoomRepository roomRepository;
    private final FlatsRepository flatsRepository;
    private final HomeRepository homeRepository;
    private final ImageService imageService;
    private final FolderProperties folder;

    @Value("${room.link.src}")
    private String room_link;

    @Autowired
    public RoomsServicesImpl(RoomRepository roomRepository,
                             FlatsRepository flatsRepository,
                             HomeRepository homeRepository,
                             ImageService imageService, FolderProperties folderProperties) {
        this.roomRepository = roomRepository;
        this.flatsRepository = flatsRepository;
        this.homeRepository = homeRepository;
        this.imageService = imageService;
        this.folder = folderProperties;
    }

    @Override
    public String saveRoom(RoomSaveRequestDTO dto, List<MultipartFile> images) throws IOException {
        EntityRooms room = new EntityRooms();
        room.setRoomName(dto.getRoomName());
        room.setSize(dto.getSize());
        room.setRentalType(dto.getRentalType());
        room.setDescription(dto.getDescription());
        room.setPrice(dto.getPrice());
        room.setRented(dto.isRented());
        room.setTenantType(dto.getTenantType());
        room.setHasPrivateBathroom(dto.isHasPrivateBathroom());


        if (dto.getFlatId() != null) {
            EntityFlats flat = flatsRepository.findById(dto.getFlatId())
                    .orElseThrow(() -> new IllegalArgumentException("Flat not found: " + dto.getFlatId()));
            room.setFlat(flat);
        }

        if (dto.getHomeId() != null) {
            EntityHomes home = homeRepository.findById(dto.getHomeId())
                    .orElseThrow(() -> new IllegalArgumentException("Home not found: " + dto.getHomeId()));
            room.setHome(home);
        }

        room = roomRepository.save(room);

        if (images != null && !images.isEmpty()) {
            String baseFolder = folder.getRooms();
            String subFolder;

            if (room.getFlat() != null) {
                Long buildingId = room.getFlat().getBuilding().getId();
                Long flatId = room.getFlat().getId();
                Long roomId = room.getId();

                     subFolder = folder.getBuilding() + "_" + buildingId + "/" +
                                 folder.getFlat() + "_" + flatId + "/" +
                                 folder.getRooms() + "_" + roomId;

            } else if (room.getHome() != null) {
                Long homeId = room.getHome().getId();
                Long roomId = room.getId();

                subFolder = folder.getHome() + "_" + homeId + "/" +
                            folder.getRooms() + "_" + roomId;

            } else {
                throw new IllegalStateException("Room must belong to either a flat or a home.");
            }

            List<String> imagePaths = imageService.saveImages(images, baseFolder, subFolder);
            room.setPicturePaths(imagePaths);
            roomRepository.save(room); // update with image paths
        }



        return "Room saved successfully.";
    }

    @Override
    public RoomDetailsDTO getRoomById(Long id) {
        EntityRooms room = roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Room not found with id: " + id));

        RoomDetailsDTO dto = new RoomDetailsDTO();
        dto.setId(room.getId());
        dto.setRentalType(room.getRentalType());
        dto.setDescription(room.getDescription());
        dto.setPrice(room.getPrice());
        dto.setRented(room.isRented());
        dto.setTenantType(room.getTenantType());

        List<String> imageUrls = room.getPicturePaths().stream()
                .map(path -> {
                    Path p = Paths.get(path);
                    String fileName = p.getFileName().toString();
                    return room_link + fileName;
                }).toList();

        dto.setPictureUrls(imageUrls);

        return dto;
    }

    @Override
    public List<RoomDetailsDTO> getAllRooms() {
        return roomRepository.findAll().stream().map(this::convertToDTO).toList();
    }

    @Override
    public String updateRoom(Long roomId, RoomSaveRequestDTO dto, List<MultipartFile> newImages, List<String> imagesToDelete) throws IOException {
        EntityRooms room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        room.setRentalType(dto.getRentalType());
        room.setDescription(dto.getDescription());
        room.setPrice(dto.getPrice());
        room.setRented(dto.isRented());
        room.setTenantType(dto.getTenantType());

        List<String> updated = imageService.updateImages(
                room.getPicturePaths(), imagesToDelete, newImages, folder.getRooms(),
                room.getFlat() != null ? "flat_" + room.getFlat().getId() : "home_" + room.getHome().getId()
        );
        room.setPicturePaths(updated);

        roomRepository.save(room);
        return "Room updated successfully.";
    }


    @Override
    public String deleteRoom(Long roomId) {
        EntityRooms room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        String folderPath;
        String baseFolderPath;
        Long parentId;

        // Build folder structure
        if (room.getFlat() != null) {
            Long buildingId = room.getFlat().getBuilding().getId();
            parentId = room.getFlat().getId();

            // e.g., building_3/flat_2/room_10
            baseFolderPath = folder.getBuilding() + "_" + buildingId + "/" + folder.getFlat() + "_" + parentId;
            folderPath = baseFolderPath + "/" + folder.getRooms() + "_" + room.getId();

        } else if (room.getHome() != null) {
            parentId = room.getHome().getId();

            // e.g., home_5/room_12
            baseFolderPath = folder.getHome() + "_" + parentId;
            folderPath = baseFolderPath + "/" + folder.getRooms() + "_" + room.getId();

        } else {
            throw new IllegalStateException("Room must belong to either a flat or a home.");
        }

        // Step 1: Delete the room's image folder
        imageService.deleteAllImages(folder.getRooms(), folderPath);

        // Step 2: Delete the room from the database
        roomRepository.delete(room);

        // Step 3: Check if any rooms remain under the same flat or home
        boolean hasRemainingRooms = (room.getFlat() != null)
                ? roomRepository.existsByFlatId(parentId)
                : roomRepository.existsByHomeId(parentId);

        // Step 4: If no rooms left, delete the parent folder
        if (!hasRemainingRooms) {
            imageService.deleteFolderIfEmpty(folder.getRooms(), baseFolderPath);
        }

        return "Room with ID " + roomId + " has been deleted.";
    }


    private RoomDetailsDTO convertToDTO(EntityRooms room) {
        RoomDetailsDTO dto = new RoomDetailsDTO();
        dto.setId(room.getId());
        dto.setRentalType(room.getRentalType());
        dto.setDescription(room.getDescription());
        dto.setPrice(room.getPrice());
        dto.setRented(room.isRented());
        dto.setTenantType(room.getTenantType());

        List<String> imageUrls = room.getPicturePaths().stream()
                .map(path -> {
                    Path p = Paths.get(path);
                    String fileName = p.getFileName().toString();
                    return room_link + fileName;
                }).toList();

        dto.setPictureUrls(imageUrls);
        return dto;
    }
}
