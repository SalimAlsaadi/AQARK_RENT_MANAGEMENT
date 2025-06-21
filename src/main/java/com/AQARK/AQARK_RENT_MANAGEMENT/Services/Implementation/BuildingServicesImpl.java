package com.AQARK.AQARK_RENT_MANAGEMENT.Services.Implementation;

import com.AQARK.AQARK_RENT_MANAGEMENT.Common_Utilities.FolderProperties;
import com.AQARK.AQARK_RENT_MANAGEMENT.Common_Utilities.ImageService;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.BuildingDTO.BuildingDetailsDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.BuildingDTO.BuildingSaveRequestDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityBuildings;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityFlats;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityRooms;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.Landlord;
import com.AQARK.AQARK_RENT_MANAGEMENT.Repositories.BuildingRepository;
import com.AQARK.AQARK_RENT_MANAGEMENT.Repositories.FlatsRepository;
import com.AQARK.AQARK_RENT_MANAGEMENT.Repositories.LandlordRepository;
import com.AQARK.AQARK_RENT_MANAGEMENT.Repositories.RoomRepository;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface.BuildingServicesInterface;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
public class BuildingServicesImpl implements BuildingServicesInterface {

    private final BuildingRepository buildingRepository;
    private final FlatsRepository flatsRepository;
    private final RoomRepository roomRepository;
    private final ImageService imageService;
    private final LandlordRepository landlordRepository;
    private final FolderProperties folderProperties;

    @Value("${building.link.src}")
    private String folder_building_link;

    @Autowired
    public BuildingServicesImpl(BuildingRepository buildingRepository, FlatsRepository flatsRepository, RoomRepository repository, ImageService imageService, LandlordRepository landlordRepository,FolderProperties folderProperties) {
        this.buildingRepository = buildingRepository;
        this.flatsRepository = flatsRepository;
        this.roomRepository = repository;
        this.imageService = imageService;
        this.landlordRepository = landlordRepository;
        this.folderProperties=folderProperties;
    }

    @Override
    public BuildingDetailsDTO createBuilding(BuildingSaveRequestDTO buildingDTO, List<MultipartFile> pictures) throws IOException {

        EntityBuildings building=convertToEntity(buildingDTO);
        EntityBuildings savedBuilding = buildingRepository.save(building);

        if (pictures != null && !pictures.isEmpty()) {
            List<String> picturePaths = imageService.saveImages(pictures, folderProperties.getBuildings(), folderProperties.getBuilding()+ "_" +savedBuilding.getId().toString());
            savedBuilding.setPicturePaths(picturePaths);
            savedBuilding = buildingRepository.save(savedBuilding); // update with pictures
        }
        BuildingDetailsDTO dto=convertToDTO(savedBuilding);


        return dto;
    }

    @Override
    public BuildingDetailsDTO getBuildingById(Long id) {
        EntityBuildings building = buildingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Building not found with id: " + id));

        return (convertToDTO(building));
    }


    @Override
    public List<EntityBuildings> getAllBuildings() {
        return buildingRepository.findAll();
    }

    @Override
    public BuildingDetailsDTO updateBuilding(BuildingDetailsDTO updatedBuilding, List<String> picturesToDelete, List<MultipartFile> newPictures) throws IOException {
        EntityBuildings existing = buildingRepository.findById(updatedBuilding.getId())
                .orElseThrow(() -> new RuntimeException("Building not found with id: " + updatedBuilding.getId()));

        // Update fields
        existing.setBuildingName(updatedBuilding.getBuildingName());
        existing.setRegion(updatedBuilding.getRegion());
        existing.setWilaya(updatedBuilding.getWilaya());
        existing.setTotalFlats(updatedBuilding.getTotalFlats());
        existing.setFlatsRented(updatedBuilding.getFlatsRented());
        existing.setFlatsNotRented(updatedBuilding.getFlatsNotRented());
        existing.setNumberOfFloors(updatedBuilding.getNumberOfFloors());
        existing.setHasParking(updatedBuilding.isHasParking());
        existing.setHasGym(updatedBuilding.isHasGym());
        existing.setHasSwimmingPool(updatedBuilding.isHasSwimmingPool());
        existing.setDescription(updatedBuilding.getDescription());
        existing.setLatitude(updatedBuilding.getLatitude());
        existing.setLongitude(updatedBuilding.getLongitude());

//        // Update pictures
        List<String> updatedPictures = imageService.updateImages(
                existing.getPicturePaths(), picturesToDelete, newPictures, folderProperties.getBuilding(), existing.getId().toString()
        );
        existing.setPicturePaths(updatedPictures);

        buildingRepository.save(existing);
        return convertToDTO(existing);
    }


    @Override
    public void deleteBuilding(Long buildingId) {
        EntityBuildings building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new RuntimeException("Building not found with id: " + buildingId));

        List<EntityFlats> flats = building.getFlats();
        for (EntityFlats flat : flats) {
            Long flatId = flat.getId();

            List<EntityRooms> rooms = flat.getRooms();
            for (EntityRooms room : rooms) {
                Long roomId = room.getId();

                // Delete room folder: room/building_3/flat_2/room_10
                String roomFolder = folderProperties.getBuilding() + "_" + buildingId + "/" +
                        folderProperties.getFlat() + "_" + flatId + "/" +
                        folderProperties.getRooms() + "_" + roomId;

                roomRepository.deleteById(roomId);
                imageService.deleteAllImages(folderProperties.getRooms(), roomFolder);
            }

            // Delete flat folder under 'room/': room/building_3/flat_2
            String flatRoomFolder = folderProperties.getBuilding() + "_" + buildingId + "/" +
                    folderProperties.getFlat() + "_" + flatId;
            imageService.deleteFolderIfEmpty(folderProperties.getRooms(), flatRoomFolder);

            // Delete flat folder under 'flat/': flat/3_2
            String flatFolder = folderProperties.getBuilding() + "_" + buildingId
                                + "/" + folderProperties.getFlat() + "_" + flatId;
            imageService.deleteAllImages(folderProperties.getFlat(), flatFolder);

            // Delete flat from DB
            flatsRepository.delete(flat);
        }

        // Delete building folder under 'room/' if empty: room/building_3
        String buildingRoomFolder = folderProperties.getBuilding() + "_" + buildingId;
        imageService.deleteFolderIfEmpty(folderProperties.getFlat(), buildingRoomFolder);

        // Delete building folder under 'room/' if empty: room/building_3
        String buildingFlatFolder = folderProperties.getBuilding() + "_" + buildingId;
        imageService.deleteFolderIfEmpty(folderProperties.getRooms(), buildingFlatFolder);

        // Delete building image folder: buildings/3
        imageService.deleteAllImages(folderProperties.getBuildings(), folderProperties.getBuilding() + "_" + buildingId);

        // Delete building from DB
        buildingRepository.delete(building);
    }



    private EntityBuildings convertToEntity(BuildingSaveRequestDTO dto) {
        EntityBuildings building = new EntityBuildings();

        building.setBuildingName(dto.getBuildingName());
        building.setRegion(dto.getRegion());
        building.setWilaya(dto.getWilaya());
        building.setTotalFlats(dto.getTotalFlats());
        building.setFlatsRented(dto.getFlatsRented());
        building.setFlatsNotRented(dto.getFlatsNotRented());
        building.setNumberOfFloors(dto.getNumberOfFloors());
        building.setHasParking(dto.isHasParking());
        building.setHasGym(dto.isHasGym());
        building.setHasSwimmingPool(dto.isHasSwimmingPool());
        building.setDescription(dto.getDescription());
        building.setLatitude(dto.getLatitude());
        building.setLongitude(dto.getLongitude());

        // You must fetch the landlord from the DB using mobile number
        Landlord landlord = landlordRepository.findById(dto.getLandlord()).orElseThrow(()->new RuntimeException("no landlord found with ID: "+dto.getLandlord()));

        building.setLandlord(landlord);

        return building;
    }

    private BuildingDetailsDTO convertToDTO(EntityBuildings building) {
        BuildingDetailsDTO dto = new BuildingDetailsDTO();

        dto.setBuildingName(building.getBuildingName());
        dto.setRegion(building.getRegion());
        dto.setWilaya(building.getWilaya());
        dto.setTotalFlats(building.getTotalFlats());
        dto.setFlatsRented(building.getFlatsRented());
        dto.setFlatsNotRented(building.getFlatsNotRented());
        dto.setNumberOfFloors(building.getNumberOfFloors());
        dto.setHasParking(building.isHasParking());
        dto.setHasGym(building.isHasGym());
        dto.setHasSwimmingPool(building.isHasSwimmingPool());
        dto.setDescription(building.getDescription());
        dto.setLatitude(building.getLatitude());
        dto.setLongitude(building.getLongitude());

        List<String> imageUrls = building.getPicturePaths().stream()
                .map(path -> {
                    Path p = Paths.get(path);
                    String fileName = p.getFileName().toString();
                    return folder_building_link + building.getId() + "/" + fileName;
                })
                .toList();
        dto.setPictureUrls(imageUrls);

        if (building.getLandlord() != null) {
            dto.setLandlordName(building.getLandlord().getFirstName()+" "+building.getLandlord().getLastName());
            dto.setLandLordMobileNumber(building.getLandlord().getPhoneNumber());
        }


        return dto;
    }


}
