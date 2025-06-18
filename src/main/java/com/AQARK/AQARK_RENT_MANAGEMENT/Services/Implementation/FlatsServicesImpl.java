package com.AQARK.AQARK_RENT_MANAGEMENT.Services.Implementation;

import com.AQARK.AQARK_RENT_MANAGEMENT.Common_Utilities.FolderProperties;
import com.AQARK.AQARK_RENT_MANAGEMENT.Common_Utilities.ImageService;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.FlatDTO.FlatsForShowListDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.FlatDTO.FlatSaveRequestDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.FlatDTO.HostelSearchRequestDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityBuildings;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityFlats;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityRooms;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.Landlord;
import com.AQARK.AQARK_RENT_MANAGEMENT.Repositories.BuildingRepository;
import com.AQARK.AQARK_RENT_MANAGEMENT.Repositories.FlatsRepository;
import com.AQARK.AQARK_RENT_MANAGEMENT.Repositories.LandlordRepository;
import com.AQARK.AQARK_RENT_MANAGEMENT.Repositories.RoomRepository;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface.FlatsServicesInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
public class FlatsServicesImpl implements FlatsServicesInterface {

    private final FlatsRepository flatsRepository;
    private final LandlordRepository landlordRepository;
    private final BuildingRepository buildingRepository;
    private final ImageService imageService;
    private final RoomRepository roomRepository;
    private final FolderProperties folder;

    @Value("${flat.link.src}")
    private String flat_link;


    @Autowired
    public FlatsServicesImpl(FlatsRepository flatsRepository,
                             LandlordRepository landlordRepository,
                             BuildingRepository buildingRepository,
                             ImageService imageService, RoomRepository roomRepository, FolderProperties folderProperties) {

        this.flatsRepository = flatsRepository;
        this.landlordRepository = landlordRepository;
        this.buildingRepository = buildingRepository;
        this.imageService = imageService;
        this.roomRepository = roomRepository;
        this.folder = folderProperties;
    }

    @Override
    public String saveFlats(FlatSaveRequestDTO flatDTO, List<MultipartFile> pictures) throws IOException {

        Landlord findLandlord = landlordRepository.findById(flatDTO.getLandlordId().getId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid landlord ID: " + flatDTO.getLandlordId().getId()));

        EntityBuildings findBuilding = buildingRepository.findById(flatDTO.getBuildingId().getId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid building ID: " + flatDTO.getBuildingId().getId()));

        EntityFlats flat = convertToEntity(flatDTO, findBuilding, findLandlord);

        flat = flatsRepository.save(flat);

        if (pictures != null && !pictures.isEmpty()) {
            String folderName = findBuilding.getId() + "_" + flat.getId();
            List<String> picturePaths = imageService.saveImages(pictures, folder.getFlat(), folderName);
            flat.setPicturePaths(picturePaths);
            flat = flatsRepository.save(flat);
        }

        return "Flat has been added successfully.";
    }


    @Override
    public String updateFlat(FlatSaveRequestDTO dto, List<MultipartFile> newImages, List<String> imagesToDelete) throws IOException {
        EntityFlats flat = flatsRepository.findById(dto.getFlatID())
                .orElseThrow(() -> new IllegalArgumentException("Flat not found: " + dto.getFlatID()));

        // Update basic details
        flat.setNumberOfRooms(dto.getNumberOfRooms());
        flat.setNumberOfBathrooms(dto.getNumberOfBathrooms());
        flat.setHasLivingRoom(dto.getHasLivingRoom());
        flat.setFloorNumber(dto.getFloorNumber());
        flat.setRented(dto.isRented());
        flat.setDescription(dto.getDescription());
        flat.setRegion(dto.getRegion());
        flat.setWilaya(dto.getWilaya());
        flat.setTenantType(dto.getTenantType());
        flat.setRentalType(dto.getRentTimeType());
        flat.setPrice(dto.getPrice());
        flat.setLandlordMobileNumber(dto.getMobileNumber());
        flat.setLatitude(dto.getLatitude());
        flat.setLongitude(dto.getLongitude());

        // Optional: update landlord and building if needed
        if (dto.getLandlordId() != null) {
            Landlord landlord = landlordRepository.findById(dto.getLandlordId().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Landlord not found: " + dto.getLandlordId().getId()));
            flat.setLandlord(landlord);
        }

        if (dto.getBuildingId() != null) {
            EntityBuildings building = buildingRepository.findById(dto.getBuildingId().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Building not found: " + dto.getBuildingId().getId()));
            flat.setBuilding(building);
        }

        // ✅ Update images using the shared ImageService
        List<String> updatedPaths = imageService.updateImages(
                flat.getPicturePaths(),
                imagesToDelete,
                newImages,
                folder.getFlat(),
                flat.getBuilding().getId().toString()
        );
        flat.setPicturePaths(updatedPaths);

        flatsRepository.save(flat);
        return "Flat details and images updated successfully.";
    }


    @Override
    public void deleteFlat(Long flatId) {
        EntityFlats flat = flatsRepository.findById(flatId)
                .orElseThrow(() -> new IllegalArgumentException("Flat not found"));

        Long buildingId = flat.getBuilding().getId();

        // 1. Delete all rooms inside this flat
        List<EntityRooms> rooms = flat.getRooms();
        for (EntityRooms room : rooms) {
            String roomFolder = folder.getBuilding() + "_" + buildingId + "/" +
                    folder.getFlat() + "_" + flatId + "/" +
                    folder.getRooms() + "_" + room.getId();

            roomRepository.deleteById(room.getId());
            imageService.deleteAllImages(folder.getRooms(), roomFolder);
        }

        // 2. Delete flat-level folder inside 'room/' if empty (room/building_3/flat_2)
        String flatRoomFolder = folder.getBuilding() + "_" + buildingId + "/" +
                folder.getFlat() + "_" + flatId;

        imageService.deleteFolderIfEmpty(folder.getRooms(), flatRoomFolder);

        // 3. Delete flat folder under 'flat/' (flat/3_2)
        String flatFolder = buildingId + "_" + flatId;
        imageService.deleteAllImages(folder.getFlat(), flatFolder);

        // 4. Delete the flat from database
        flatsRepository.delete(flat);
    }


    @Override
    public List<FlatsForShowListDTO> getAllHostels() {
        List<EntityFlats> hostels = flatsRepository.findAll();

        if (hostels.isEmpty()) {
            throw new IllegalStateException("No hostels found.");
        }

        return hostels.stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Override
    public List<FlatsForShowListDTO> getHostelBySearch(HostelSearchRequestDTO search) {
        List<EntityFlats> hostels = flatsRepository.findFlats(search);

        if (hostels.isEmpty()) {
            throw new IllegalStateException("No hostels found matching the search criteria.");
        }

        return hostels.stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Override
    public List<FlatsForShowListDTO> getHostelByLandlordID(long landlordID) {
        List<EntityFlats> hostels = flatsRepository.findByLandlordId(landlordID);

        if (hostels.isEmpty()) {
            throw new IllegalStateException("No hostels found for landlord with ID: " + landlordID);
        }

        return hostels.stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Override
    public FlatsForShowListDTO getFlatById(Long id) {
        EntityFlats flat = flatsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Flat not found with id: " + id));

        return convertToDTO(flat);
    }



    private FlatsForShowListDTO convertToDTO(EntityFlats hostel) {
        FlatsForShowListDTO dto = new FlatsForShowListDTO();

        dto.setId(hostel.getId());
        dto.setNumberOfRooms(hostel.getNumberOfRooms());
        dto.setNumberOfBathroom(hostel.getNumberOfBathrooms());
        dto.setHasLivingroom(hostel.isHasLivingRoom());
        dto.setDescription(hostel.getDescription());
        dto.setRegion(hostel.getRegion());
        dto.setWilaya(hostel.getWilaya());
        dto.setLatitude(hostel.getLatitude());
        dto.setLongitude(hostel.getLongitude());
        dto.setPrice(hostel.getPrice());
        dto.setTenantType(hostel.getTenantType());
        dto.setRentTimeType(hostel.getRentalType());

        EntityBuildings building = hostel.getBuilding(); // Use directly without re-fetch
        dto.setBuildingName(building.getBuildingName());
        dto.setHasGYM(building.isHasGym());
        dto.setHasSwimmingPool(building.isHasSwimmingPool());
        dto.setHasParking(building.isHasParking());

        // ✅ Build image URLs (folder name is buildingId_flatId)
        String folder = building.getId() + "_" + hostel.getId();
        List<String> imageUrls = hostel.getPicturePaths().stream()
                .map(path -> {
                    Path p = Paths.get(path);
                    String fileName = p.getFileName().toString();
                    return flat_link + folder + "/" + fileName;
                })
                .toList();

        dto.setPictures(imageUrls);
        return dto;
    }


    public EntityFlats convertToEntity(FlatSaveRequestDTO dto, EntityBuildings building, Landlord landlord) {
        EntityFlats flat = new EntityFlats();

        flat.setNumberOfRooms(dto.getNumberOfRooms());
        flat.setNumberOfBathrooms(dto.getNumberOfBathrooms());
        flat.setHasLivingRoom(dto.getHasLivingRoom());
        flat.setFloorNumber(dto.getFloorNumber());
        flat.setRented(dto.isRented());
        flat.setDescription(dto.getDescription());
        flat.setRegion(dto.getRegion());
        flat.setWilaya(dto.getWilaya());
        flat.setTenantType(dto.getTenantType());
        flat.setRentalType(dto.getRentTimeType());
        flat.setPrice(dto.getPrice());
        flat.setLatitude(dto.getLatitude());
        flat.setLongitude(dto.getLongitude());
        flat.setLandlordMobileNumber(dto.getMobileNumber());

        // ✅ Use attached (managed) references
        flat.setLandlord(landlord);
        flat.setBuilding(building);

        return flat;
    }

//    public EntityFlats convertToEntity(FlatSaveRequestDTO dto) {
//        EntityFlats flat = new EntityFlats();
//
//        flat.setId(dto.getFlatID());
//        flat.setNumberOfRooms((dto.getNumberOfRooms()));
//        flat.setNumberOfBathrooms(dto.getNumberOfBathrooms());
//        flat.setHasLivingRoom(dto.getHasLivingRoom());
//        flat.setRented(dto.isRented());
//        flat.setFloorNumber(dto.getFloorNumber());
//        flat.setDescription(dto.getDescription());
//        flat.setRegion(dto.getRegion());
//        flat.setWilaya(dto.getWilaya());
//        flat.setTenantType(dto.getTenantType());
//        flat.setRentalType(dto.getRentTimeType());
//        flat.setPrice(dto.getPrice());
//        flat.setLatitude(dto.getLatitude());
//        flat.setLongitude(dto.getLongitude());
//        flat.setLandlord(dto.getLandlordId());
//        flat.setBuilding(dto.getBuildingId());
//        flat.setLandlordMobileNumber(dto.getMobileNumber());
//
//        return flat;
//    }


}
