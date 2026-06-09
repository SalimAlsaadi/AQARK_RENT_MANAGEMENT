package com.AQARK.AQARK_RENT_MANAGEMENT.Services.Implementation;

import com.AQARK.AQARK_RENT_MANAGEMENT.Common_Utilities.FolderProperties;
import com.AQARK.AQARK_RENT_MANAGEMENT.Common_Utilities.ImageService;
import com.AQARK.AQARK_RENT_MANAGEMENT.Common_Utilities.PropertyMediaService;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.BuildingDTO.BuildingDetailsDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.BuildingDTO.BuildingSaveRequestDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.MediaDTO.PropertyMediaDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.*;
import com.AQARK.AQARK_RENT_MANAGEMENT.Repositories.*;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface.BuildingServicesInterface;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BuildingServicesImpl implements BuildingServicesInterface {

    private final BuildingRepository buildingRepository;
    private final FlatsRepository flatsRepository;
    private final RoomRepository roomRepository;
    private final ImageService imageService;
    private final UserRepository userRepository;
    private final FolderProperties folderProperties;
    private final PropertyMediaService propertyMediaService;
    private final ShopRoomRepository shopRoomRepository;

    @Value("${building.link.src}")
    private String folder_building_link;


    @Override
    @Transactional
    public BuildingDetailsDTO createBuilding(BuildingSaveRequestDTO buildingDTO, List<MultipartFile> pictures, Long landlord) throws IOException {

        buildingDTO.setLandlordId(landlord);
        EntityBuildings building=convertToEntity(buildingDTO);
        EntityBuildings savedBuilding = buildingRepository.save(building);

        propertyMediaService.saveImages(
                pictures,
                MediaOwnerType.BUILDING,
                savedBuilding.getId()
        );
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
    public List<BuildingDetailsDTO> getBuildingByLandlordID(Long landlordId) throws IOException{

        if (landlordId == null || landlordId <= 0) {
            throw new IllegalArgumentException("Invalid landlord ID.");
        }

        List<EntityBuildings> buildings=buildingRepository.findAllByLandlordId(landlordId);

        if(buildings==null || buildings.isEmpty()){
            throw  new FileNotFoundException( "no buildings available for this LandLord");
        }

        List<BuildingDetailsDTO> listBuilding= new ArrayList<>();

        for(EntityBuildings entityBuildings: buildings){
            BuildingDetailsDTO buildingList=convertToDTO(entityBuildings);
            listBuilding.add(buildingList);
        }

        return listBuilding;
    }

    @Override
    public BuildingDetailsDTO updateBuilding(BuildingDetailsDTO updatedBuilding, List<Long> picturesToDelete, List<MultipartFile> newPictures) throws IOException {
        EntityBuildings existing = buildingRepository.findById(updatedBuilding.getId())
                .orElseThrow(() -> new RuntimeException("Building not found with id: " + updatedBuilding.getId()));

        // Update fields
        existing.setBuildingName(updatedBuilding.getBuildingName());
        existing.setRegion(updatedBuilding.getRegion());
        existing.setWilaya(updatedBuilding.getWilaya());
        existing.setHasParking(updatedBuilding.isHasParking());
        existing.setHasGym(updatedBuilding.isHasGym());
        existing.setHasSwimmingPool(updatedBuilding.isHasSwimmingPool());
        existing.setDescription(updatedBuilding.getDescription());
        existing.setLatitude(updatedBuilding.getLatitude());
        existing.setLongitude(updatedBuilding.getLongitude());

//        // Update pictures
        List<PropertyMediaDTO> updatedPictures = propertyMediaService.updateImages(
                MediaOwnerType.BUILDING, existing.getId(), picturesToDelete, newPictures
        );

        buildingRepository.save(existing);
        return convertToDTO(existing);
    }


//    @Override
//    public void deleteBuilding(Long buildingId) {
//        EntityBuildings building = buildingRepository.findById(buildingId)
//                .orElseThrow(() -> new RuntimeException("Building not found with id: " + buildingId));
//
//        List<EntityFlats> flats = building.getFlats();
//        for (EntityFlats flat : flats) {
//            Long flatId = flat.getId();
//
//            List<EntityRooms> rooms = flat.getRooms();
//            for (EntityRooms room : rooms) {
//                Long roomId = room.getId();
//
//                // Delete room folder: room/building_3/flat_2/room_10
//                String roomFolder = folderProperties.getBuilding() + "_" + buildingId + "/" +
//                        folderProperties.getFlat() + "_" + flatId + "/" +
//                        folderProperties.getRooms() + "_" + roomId;
//
//                roomRepository.deleteById(roomId);
//                imageService.deleteAllImages(folderProperties.getRooms(), roomFolder);
//            }
//
//            // Delete flat folder under 'room/': room/building_3/flat_2
//            String flatRoomFolder = folderProperties.getBuilding() + "_" + buildingId + "/" +
//                    folderProperties.getFlat() + "_" + flatId;
//            imageService.deleteFolderIfEmpty(folderProperties.getRooms(), flatRoomFolder);
//
//            // Delete flat folder under 'flat/': flat/3_2
//            String flatFolder = folderProperties.getBuilding() + "_" + buildingId
//                                + "/" + folderProperties.getFlat() + "_" + flatId;
//            imageService.deleteAllImages(folderProperties.getFlat(), flatFolder);
//
//            // Delete flat from DB
//            flatsRepository.delete(flat);
//        }
//
//        // Delete building folder under 'room/' if empty: room/building_3
//        String buildingRoomFolder = folderProperties.getBuilding() + "_" + buildingId;
//        imageService.deleteFolderIfEmpty(folderProperties.getFlat(), buildingRoomFolder);
//
//        // Delete building folder under 'room/' if empty: room/building_3
//        String buildingFlatFolder = folderProperties.getBuilding() + "_" + buildingId;
//        imageService.deleteFolderIfEmpty(folderProperties.getRooms(), buildingFlatFolder);
//
//        // Delete building image folder: buildings/3
//        imageService.deleteAllImages(folderProperties.getBuildings(), folderProperties.getBuilding() + "_" + buildingId);
//
//        // Delete building from DB
//        buildingRepository.delete(building);
//    }



    private EntityBuildings convertToEntity(BuildingSaveRequestDTO dto) {
        EntityBuildings building = new EntityBuildings();

        building.setBuildingName(dto.getBuildingName());
        building.setRegion(dto.getRegion());
        building.setWilaya(dto.getWilaya());
        building.setArea(dto.getArea());
        building.setHasParking(dto.getHasParking());
        building.setHasGym(dto.getHasGym());
        building.setHasSwimmingPool(dto.getHasSwimmingPool());
        building.setDescription(dto.getDescription());
        building.setLatitude(dto.getLatitude());
        building.setLongitude(dto.getLongitude());

        // You must fetch the landlord from the DB using mobile number
        EntityUser entityUser = userRepository.findById(dto.getLandlordId()).orElseThrow(()->new RuntimeException("no landlord found with ID: "+dto.getLandlordId()));

        building.setLandlord(entityUser);

        return building;
    }

    private BuildingDetailsDTO convertToDTO(EntityBuildings building) {

        BuildingDetailsDTO dto = new BuildingDetailsDTO();

        dto.setId(building.getId());
        dto.setBuildingName(building.getBuildingName());
        dto.setRegion(building.getRegion());
        dto.setWilaya(building.getWilaya());
        dto.setArea(building.getArea());
        dto.setHasParking(building.isHasParking());
        dto.setHasGym(building.isHasGym());
        dto.setHasSwimmingPool(building.isHasSwimmingPool());
        dto.setDescription(building.getDescription());
        dto.setLatitude(building.getLatitude());
        dto.setLongitude(building.getLongitude());
        dto.setNumberOfFloors(building.getNumber_of_floors());
        dto.setFlatsRented(flatsRepository.countByBuildingIdAndIsRentedTrue(building.getId()));
        dto.setFlatsNotRented(flatsRepository.countByBuildingIdAndIsRentedFalse(building.getId()));
        dto.setRoomsRented(roomRepository.countByFlatBuildingIdAndRentedTrue(building.getId()));
        dto.setRoomsNotRented(roomRepository.countByFlatBuildingIdAndRentedFalse(building.getId()));
        dto.setShopsRented(shopRoomRepository.countByBuildingIdAndRentedTrue(building.getId()));
        dto.setShopsNotRented(shopRoomRepository.countByBuildingIdAndRentedFalse(building.getId()));


        if (building.getLandlord() != null) {
            dto.setLandlordName(building.getLandlord().getFirstName() + " " + building.getLandlord().getLastName());
            dto.setLandLordMobileNumber(building.getLandlord().getPhoneNumber());
        }

        List<String> imageUrls = propertyMediaService
                .getImages(MediaOwnerType.BUILDING, building.getId())
                .stream()
                .map(PropertyMediaDTO::getFileUrl)
                .toList();

        dto.setPictureUrls(imageUrls);

        return dto;
    }


}
