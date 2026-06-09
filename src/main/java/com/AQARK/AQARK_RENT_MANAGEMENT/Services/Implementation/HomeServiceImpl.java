package com.AQARK.AQARK_RENT_MANAGEMENT.Services.Implementation;

import com.AQARK.AQARK_RENT_MANAGEMENT.Common_Utilities.PropertyMediaService;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.HomeDTO.HomeDetailsDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.HomeDTO.HomeSaveRequestDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.MediaDTO.PropertyMediaDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityHomes;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityRooms;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityUser;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.MediaOwnerType;
import com.AQARK.AQARK_RENT_MANAGEMENT.Repositories.HomeRepository;
import com.AQARK.AQARK_RENT_MANAGEMENT.Repositories.RoomRepository;
import com.AQARK.AQARK_RENT_MANAGEMENT.Repositories.UserRepository;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface.HomeServiceInterface;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HomeServiceImpl implements HomeServiceInterface {

    private final HomeRepository homeRepository;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final PropertyMediaService propertyMediaService;

    @Override
    @Transactional
    public HomeDetailsDTO createHome(
            HomeSaveRequestDTO dto,
            List<MultipartFile> pictures,
            Long landlordId
    ) throws IOException {

        dto.setLandlordId(landlordId);
        validateCreateRequest(dto);

        EntityUser landlord = userRepository.findById(dto.getLandlordId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Landlord not found with ID: " + dto.getLandlordId()
                ));

        EntityHomes home = new EntityHomes();

        home.setHomeType(dto.getHomeType());
        home.setNumberOfRooms(dto.getNumberOfRooms());
        home.setNumberOfBathrooms(dto.getNumberOfBathrooms());

        home.setHasLivingRoom(Boolean.TRUE.equals(dto.getHasLivingRoom()));
        home.setFurnished(Boolean.TRUE.equals(dto.getFurnished()));
        home.setRented(Boolean.TRUE.equals(dto.getRented()));

        home.setFloorNumber(dto.getFloorNumber());
        home.setDescription(dto.getDescription());

        home.setRegion(dto.getRegion());
        home.setWilaya(dto.getWilaya());
        home.setLocation(dto.getLocation());

        home.setLatitude(dto.getLatitude());
        home.setLongitude(dto.getLongitude());

        home.setTenantType(dto.getTenantType());
        home.setRentType(dto.getRentType());

        home.setPrice(dto.getPrice());
        home.setLandlord(landlord);

        EntityHomes savedHome = homeRepository.save(home);

        if (pictures != null && !pictures.isEmpty()) {
            propertyMediaService.saveImages(
                    pictures,
                    MediaOwnerType.HOME,
                    savedHome.getId()
            );
        }

        return convertToDTO(savedHome);
    }

    @Override
    @Transactional
    public HomeDetailsDTO updateHome(

            HomeSaveRequestDTO dto,
            List<MultipartFile> newImages,
            List<Long> imageIdsToDelete
    ) throws IOException {

        if (dto.getId() == null || dto.getId() <= 0) {
            throw new IllegalArgumentException("Home ID is required.");
        }

        EntityHomes home = homeRepository.findById(dto.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Home not found with ID: " + dto.getId()
                ));

        updateHomeFields(home, dto);

        if (dto.getLandlordId() != null) {
            EntityUser landlord = userRepository.findById(dto.getLandlordId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Landlord not found with ID: " + dto.getLandlordId()
                    ));

            home.setLandlord(landlord);
        }

        EntityHomes savedHome = homeRepository.save(home);

        propertyMediaService.updateImages(
                MediaOwnerType.HOME,
                savedHome.getId(),
                imageIdsToDelete,
                newImages
        );

        return convertToDTO(savedHome);
    }

    @Override
    public HomeDetailsDTO getHomeById(Long homeId) {

        EntityHomes home = homeRepository.findById(homeId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Home not found with ID: " + homeId
                ));

        return convertToDTO(home);
    }

    @Override
    public List<HomeDetailsDTO> getAllHomes() {

        return homeRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Override
    public List<HomeDetailsDTO> getHomesByLandlord(Long landlordId) {

        if (landlordId == null || landlordId <= 0) {
            throw new IllegalArgumentException("Landlord ID is required.");
        }

        return homeRepository.findByLandlordId(landlordId)
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Override
    @Transactional
    public void deleteHomeById(Long homeId) {

        EntityHomes home = homeRepository.findById(homeId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Home not found with ID: " + homeId
                ));

        List<EntityRooms> rooms = roomRepository.findByHomeId(homeId);

        if (rooms != null && !rooms.isEmpty()) {
            for (EntityRooms room : rooms) {
                propertyMediaService.softDeleteAllImages(
                        MediaOwnerType.ROOM,
                        room.getId()
                );

                roomRepository.delete(room);
            }
        }

        propertyMediaService.softDeleteAllImages(
                MediaOwnerType.HOME,
                home.getId()
        );

        homeRepository.delete(home);
    }

    private void updateHomeFields(EntityHomes home, HomeSaveRequestDTO dto) {

        if (dto.getHomeType() != null) {
            home.setHomeType(dto.getHomeType());
        }

        if (dto.getNumberOfRooms() != null) {
            home.setNumberOfRooms(dto.getNumberOfRooms());
        }

        if (dto.getNumberOfBathrooms() != null) {
            home.setNumberOfBathrooms(dto.getNumberOfBathrooms());
        }

        if (dto.getHasLivingRoom() != null) {
            home.setHasLivingRoom(dto.getHasLivingRoom());
        }

        if (dto.getFurnished() != null) {
            home.setFurnished(dto.getFurnished());
        }

        if (dto.getRented() != null) {
            home.setRented(dto.getRented());
        }

        if (dto.getFloorNumber() != null) {
            home.setFloorNumber(dto.getFloorNumber());
        }

        if (dto.getDescription() != null) {
            home.setDescription(dto.getDescription());
        }

        if (dto.getRegion() != null) {
            home.setRegion(dto.getRegion());
        }

        if (dto.getWilaya() != null) {
            home.setWilaya(dto.getWilaya());
        }

        if (dto.getLocation() != null) {
            home.setLocation(dto.getLocation());
        }

        if (dto.getLatitude() != null) {
            home.setLatitude(dto.getLatitude());
        }

        if (dto.getLongitude() != null) {
            home.setLongitude(dto.getLongitude());
        }

        if (dto.getTenantType() != null) {
            home.setTenantType(dto.getTenantType());
        }

        if (dto.getRentType() != null) {
            home.setRentType(dto.getRentType());
        }

        if (dto.getPrice() != null) {
            home.setPrice(dto.getPrice());
        }
    }

    private HomeDetailsDTO convertToDTO(EntityHomes home) {

        EntityUser landlord = home.getLandlord();

        List<PropertyMediaDTO> images = propertyMediaService.getImages(
                MediaOwnerType.HOME,
                home.getId()
        );

        return HomeDetailsDTO.builder()
                .id(home.getId())
                .homeType(home.getHomeType())

                .numberOfRooms(home.getNumberOfRooms())
                .numberOfBathrooms(home.getNumberOfBathrooms())

                .hasLivingRoom(home.isHasLivingRoom())
                .furnished(home.isFurnished())
                .rented(home.isRented())

                .floorNumber(home.getFloorNumber())
                .description(home.getDescription())

                .region(home.getRegion())
                .wilaya(home.getWilaya())
                .location(home.getLocation())

                .latitude(home.getLatitude())
                .longitude(home.getLongitude())

                .tenantType(home.getTenantType())
                .rentType(home.getRentType())

                .price(home.getPrice())

                .landlordId(landlord != null ? landlord.getId() : null)
                .landlordName(
                        landlord != null
                                ? landlord.getFirstName() + " " + landlord.getLastName()
                                : null
                )
                .landlordMobileNumber(
                        landlord != null ? landlord.getPhoneNumber() : null
                )

                .images(images != null ? images : Collections.emptyList())
                .build();
    }

    private void validateCreateRequest(HomeSaveRequestDTO dto) {

        if (dto == null) {
            throw new IllegalArgumentException("Home request body is required.");
        }

        if (dto.getLandlordId() == null || dto.getLandlordId() <= 0) {
            throw new IllegalArgumentException("Landlord ID is required.");
        }

        if (dto.getHomeType() == null || dto.getHomeType().isBlank()) {
            throw new IllegalArgumentException("Home type is required.");
        }

        if (dto.getNumberOfRooms() == null || dto.getNumberOfRooms() < 1) {
            throw new IllegalArgumentException("Number of rooms must be at least 1.");
        }

        if (dto.getNumberOfBathrooms() == null || dto.getNumberOfBathrooms() < 1) {
            throw new IllegalArgumentException("Number of bathrooms must be at least 1.");
        }

        if (dto.getRegion() == null || dto.getRegion().isBlank()) {
            throw new IllegalArgumentException("Region is required.");
        }

        if (dto.getWilaya() == null || dto.getWilaya().isBlank()) {
            throw new IllegalArgumentException("Wilaya is required.");
        }

        if (dto.getTenantType() == null || dto.getTenantType().isBlank()) {
            throw new IllegalArgumentException("Tenant type is required.");
        }

        if (dto.getRentType() == null || dto.getRentType().isBlank()) {
            throw new IllegalArgumentException("Rent type is required.");
        }

        if (dto.getPrice() == null) {
            throw new IllegalArgumentException("Price is required.");
        }
    }
}



//package com.AQARK.AQARK_RENT_MANAGEMENT.Services.Implementation;
//
//import com.AQARK.AQARK_RENT_MANAGEMENT.Common_Utilities.ImageService;
//import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.HomeDTO.HomeDetailsDTO;
//import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.HomeDTO.HomeSaveRequestDTO;
//import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.UpdateHostelPicturesDTO;
//import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityHomes;
//import com.AQARK.AQARK_RENT_MANAGEMENT.Repositories.HomeRepository;
//import com.AQARK.AQARK_RENT_MANAGEMENT.Repositories.UserRepository;
//import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface.HomeServiceInterface;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.util.Base64;
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Service
//public class HomeServiceImpl implements HomeServiceInterface {
//
//    private final HomeRepository homeRepository;
//    private final UserRepository userRepository;
//    private final ImageService imageService;
//
//    @Autowired
//    public HomeServiceImpl(HomeRepository homeRepository, UserRepository userRepository, ImageService imageService) {
//        this.homeRepository = homeRepository;
//        this.userRepository = userRepository;
//        this.imageService = imageService;
//    }
//
//    @Override
//    public List<HomeSaveRequestDTO> getAllHomes() {
//        return homeRepository.findAll().stream()
//                .map(this::convertToDTO)
//                .collect(Collectors.toList());
//    }
//
//    @Override
//    public List<HomeDetailsDTO> getHomesByLandlord(Long landlordId) {
//        List<EntityHomes> homes=homeRepository.findByLandlordId(landlordId);
//
//        if (homes.isEmpty()) {
//            throw new IllegalArgumentException("No homes found for landlord ID: " + landlordId);
//        }
//
//        return homes.stream()
//                .map(this::convertHomeDetailsToDTO)
//                .toList();
//    }
//
//    @Override
//    public String saveHome(HomeSaveRequestDTO home, List<MultipartFile> pictures) {
//        if (home == null) throw new IllegalArgumentException("Home DTO cannot be null");
//
//        EntityHomes entity = convertToEntity(home);
//
//        EntityHomes saved = homeRepository.save(entity);
//
//        try {
//            if (pictures != null && !pictures.isEmpty()) {
//                List<String> picturePaths = imageService.saveImages(pictures, "home", saved.getId().toString());
//                saved.setPicturePaths(picturePaths);
//                saved = homeRepository.save(saved);
//            }
//        } catch (IOException e) {
//            throw new RuntimeException("Error saving images", e);
//        }
//
//        return "new home has been saved";
//    }
//
//    @Override
//    public HomeDetailsDTO updateHome(HomeDetailsDTO homeDto,List<String> picturesToDelete, List<MultipartFile> newPictures) throws IOException {
//        if (homeDto == null || homeDto.getId() == null) {
//            throw new IllegalArgumentException("Home ID must not be null for update");
//        }
//
//        EntityHomes existingHome = homeRepository.findById(homeDto.getId())
//                .orElseThrow(() -> new RuntimeException("Home not found with ID: " + homeDto.getId()));
//
//        existingHome.setHomeType(homeDto.getHomeType());
//        existingHome.setNumberOfRooms(homeDto.getNumberOfRooms());
//        existingHome.setNumberOfBathrooms(homeDto.getNumberOfBathrooms());
//        existingHome.setHasLivingRoom(homeDto.isHasLivingRoom());
//        existingHome.setFurnished(homeDto.isFurnished());
//        existingHome.setRented(homeDto.isRented());
//        existingHome.setFloorNumber(homeDto.getFloorNumber());
//        existingHome.setDescription(homeDto.getDescription());
//        existingHome.setRegion(homeDto.getRegion());
//        existingHome.setWilaya(homeDto.getWilaya());
//        existingHome.setLocation(homeDto.getLocation());
//        existingHome.setLatitude(homeDto.getLatitude());
//        existingHome.setLongitude(homeDto.getLongitude());
//        existingHome.setTenantType(homeDto.getTenantType());
//        existingHome.setRentType(homeDto.getRentType());
//        existingHome.setPrice(homeDto.getPrice());
//
//        // Update pictures
//        List<String> updatedPictures = imageService.updateImages(
//                existingHome.getPicturePaths(), picturesToDelete, newPictures, "home", homeDto.getId().toString()
//        );
//        existingHome.setPicturePaths(updatedPictures);
//
//        EntityHomes updated = homeRepository.save(existingHome);
//        return convertHomeDetailsToDTO(updated);
//    }
//
//    @Override
//    public HomeSaveRequestDTO updateHomePictures(UpdateHostelPicturesDTO dto) {
//        EntityHomes home = homeRepository.findById(dto.getPropertyId())
//                .orElseThrow(() -> new IllegalArgumentException("Home not found with id: " + dto.getPropertyId()));
//
//        try {
//            List<String> updatedPicturePaths = imageService.updateImages(
//                    home.getPicturePaths(),
//                    dto.getPicturesToRemove(),
//                    dto.getNewPictures(),
//                    "home",
//                    dto.getPropertyId().toString()
//            );
//            home.setPicturePaths(updatedPicturePaths);
//        } catch (IOException e) {
//            throw new RuntimeException("Failed to update images", e);
//        }
//
//        EntityHomes updated = homeRepository.save(home);
//        return convertToDTO(updated);
//    }
//
//    @Override
//    public HomeDetailsDTO getHomeById(Long homeID) {
//        EntityHomes home=homeRepository.findById(homeID).orElseThrow(()-> new RuntimeException("Building not found with id: " + homeID));
//
//        return convertHomeDetailsToDTO(home);
//    }
//
//    @Override
//    public String deleteHomeById(Long id){
//        EntityHomes home=homeRepository.findById(id).orElseThrow(()-> new RuntimeException("Home not found with ID: " + id));
//
//        imageService.deleteAllImages("home",id.toString());
//        homeRepository.delete(home);
//
//        return "home has been deleted";
//    }
//
//    private HomeSaveRequestDTO convertToDTO(EntityHomes home) {
//        HomeSaveRequestDTO dto = new HomeSaveRequestDTO();
//        dto.setId(home.getId());
//        dto.setHomeType(home.getHomeType());
//        dto.setNumberOfRooms(home.getNumberOfRooms());
//        dto.setNumberOfBathrooms(home.getNumberOfBathrooms());
//        dto.setHasLivingRoom(home.isHasLivingRoom());
//        dto.setFurnished(home.isFurnished());
//        dto.setRented(home.isRented());
//        dto.setRegion(home.getRegion());
//        dto.setWilaya(home.getWilaya());
//        dto.setLocation(home.getLocation());
//        dto.setPrice(home.getPrice());
//        dto.setTenantType(home.getTenantType());
//        dto.setRentType(home.getRentType());
//        dto.setLatitude(home.getLatitude());
//        dto.setLongitude(home.getLongitude());
//
//        if (home.getLandlord() != null) {
//            dto.setLandlordMobileNumber(home.getLandlord().getPhoneNumber());
//        }
//
//        // Convert image paths to base64 for UI
//        if (home.getPicturePaths() != null) {
//            List<String> pictures = home.getPicturePaths().stream()
//                    .map(path -> {
//                        try {
//                            byte[] imageBytes = Files.readAllBytes(Path.of(path));
//                            return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(imageBytes);
//                        } catch (IOException e) {
//                            return null;
//                        }
//                    })
//                    .filter(p -> p != null)
//                    .collect(Collectors.toList());
//
//            dto.setPicturePaths(pictures);
//        }
//
//        return dto;
//    }
//
//    private HomeDetailsDTO convertHomeDetailsToDTO(EntityHomes home) {
//        HomeDetailsDTO dto = new HomeDetailsDTO();
//
//        dto.setId(home.getId());
//        dto.setHomeType(home.getHomeType());
//        dto.setNumberOfRooms(home.getNumberOfRooms());
//        dto.setNumberOfBathrooms(home.getNumberOfBathrooms());
//        dto.setHasLivingRoom(home.isHasLivingRoom());
//        dto.setFurnished(home.isFurnished());
//        dto.setRented(home.isRented());
//        dto.setFloorNumber(home.getFloorNumber());
//        dto.setDescription(home.getDescription());
//        dto.setRegion(home.getRegion());
//        dto.setWilaya(home.getWilaya());
//        dto.setLocation(home.getLocation());
//        dto.setLatitude(home.getLatitude());
//        dto.setLongitude(home.getLongitude());
//        dto.setTenantType(home.getTenantType());
//        dto.setRentType(home.getRentType());
//        dto.setPrice(home.getPrice());
//
//        // Convert file paths to image URLs
//        List<String> imageUrls = home.getPicturePaths().stream()
//                .map(path -> {
//                    Path p = Paths.get(path);
//                    String fileName = p.getFileName().toString();
//                    return "http://localhost:8008/api/images/homes/" + home.getId() + "/" + fileName;
//                })
//                .toList();
//        dto.setPictureUrls(imageUrls);
//
//        // Set landlord details if available
//        if (home.getLandlord() != null) {
//            dto.setLandlordName(home.getLandlord().getFirstName() + " " + home.getLandlord().getLastName());
//            dto.setLandlordMobileNumber(home.getLandlord().getPhoneNumber());
//        }
//
//        return dto;
//    }
//
//    private EntityHomes convertToEntity(HomeSaveRequestDTO home) {
//        EntityHomes entity = new EntityHomes();
//        entity.setId(home.getId());
//        entity.setHomeType(home.getHomeType());
//        entity.setNumberOfRooms(home.getNumberOfRooms());
//        entity.setNumberOfBathrooms(home.getNumberOfBathrooms());
//        entity.setHasLivingRoom(home.isHasLivingRoom());
//        entity.setFurnished(home.isFurnished());
//        entity.setRented(home.isRented());
//        entity.setFloorNumber(home.getFloorNumber());
//        entity.setDescription(home.getDescription());
//        entity.setRegion(home.getRegion());
//        entity.setWilaya(home.getWilaya());
//        entity.setLocation(home.getLocation());
//        entity.setLatitude(home.getLatitude());
//        entity.setLongitude(home.getLongitude());
//        entity.setTenantType(home.getTenantType());
//        entity.setRentType(home.getRentType());
//        entity.setPrice(home.getPrice());
//        if (home.getLandlordMobileNumber() != null) {
//            entity.setLandlord(userRepository.findByPhoneNumber(home.getLandlordMobileNumber()));
//        }
//        return entity;
//    }
//
//
//}
