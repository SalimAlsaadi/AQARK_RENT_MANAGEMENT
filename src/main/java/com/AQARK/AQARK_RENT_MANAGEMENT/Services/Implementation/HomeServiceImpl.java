package com.AQARK.AQARK_RENT_MANAGEMENT.Services.Implementation;

import com.AQARK.AQARK_RENT_MANAGEMENT.Common_Utilities.ImageService;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.HomeDTO.HomeDetailsDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.HomeDTO.HomeSaveRequestDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.UpdateHostelPicturesDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityHomes;
import com.AQARK.AQARK_RENT_MANAGEMENT.Repositories.HomeRepository;
import com.AQARK.AQARK_RENT_MANAGEMENT.Repositories.LandlordRepository;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface.HomeServiceInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class HomeServiceImpl implements HomeServiceInterface {

    private final HomeRepository homeRepository;
    private final LandlordRepository landlordRepository;
    private final ImageService imageService;

    @Autowired
    public HomeServiceImpl(HomeRepository homeRepository, LandlordRepository landlordRepository, ImageService imageService) {
        this.homeRepository = homeRepository;
        this.landlordRepository = landlordRepository;
        this.imageService = imageService;
    }

    @Override
    public List<HomeSaveRequestDTO> getAllHomes() {
        return homeRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<HomeDetailsDTO> getHomesByLandlord(Long landlordId) {
        List<EntityHomes> homes=homeRepository.findByLandlordId(landlordId);

        if (homes.isEmpty()) {
            throw new IllegalArgumentException("No homes found for landlord ID: " + landlordId);
        }

        return homes.stream()
                .map(this::convertHomeDetailsToDTO)
                .toList();
    }

    @Override
    public String saveHome(HomeSaveRequestDTO home, List<MultipartFile> pictures) {
        if (home == null) throw new IllegalArgumentException("Home DTO cannot be null");

        EntityHomes entity = convertToEntity(home);

        EntityHomes saved = homeRepository.save(entity);

        try {
            if (pictures != null && !pictures.isEmpty()) {
                List<String> picturePaths = imageService.saveImages(pictures, "home", saved.getId().toString());
                saved.setPicturePaths(picturePaths);
                saved = homeRepository.save(saved);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error saving images", e);
        }

        return "new home has been saved";
    }

    @Override
    public HomeDetailsDTO updateHome(HomeDetailsDTO homeDto,List<String> picturesToDelete, List<MultipartFile> newPictures) throws IOException {
        if (homeDto == null || homeDto.getId() == null) {
            throw new IllegalArgumentException("Home ID must not be null for update");
        }

        EntityHomes existingHome = homeRepository.findById(homeDto.getId())
                .orElseThrow(() -> new RuntimeException("Home not found with ID: " + homeDto.getId()));

        existingHome.setHomeType(homeDto.getHomeType());
        existingHome.setNumberOfRooms(homeDto.getNumberOfRooms());
        existingHome.setNumberOfBathrooms(homeDto.getNumberOfBathrooms());
        existingHome.setHasLivingRoom(homeDto.isHasLivingRoom());
        existingHome.setFurnished(homeDto.isFurnished());
        existingHome.setRented(homeDto.isRented());
        existingHome.setFloorNumber(homeDto.getFloorNumber());
        existingHome.setDescription(homeDto.getDescription());
        existingHome.setRegion(homeDto.getRegion());
        existingHome.setWilaya(homeDto.getWilaya());
        existingHome.setLocation(homeDto.getLocation());
        existingHome.setLatitude(homeDto.getLatitude());
        existingHome.setLongitude(homeDto.getLongitude());
        existingHome.setTenantType(homeDto.getTenantType());
        existingHome.setRentType(homeDto.getRentType());
        existingHome.setPrice(homeDto.getPrice());

        // Update pictures
        List<String> updatedPictures = imageService.updateImages(
                existingHome.getPicturePaths(), picturesToDelete, newPictures, "home", homeDto.getId().toString()
        );
        existingHome.setPicturePaths(updatedPictures);

        EntityHomes updated = homeRepository.save(existingHome);
        return convertHomeDetailsToDTO(updated);
    }

    @Override
    public HomeSaveRequestDTO updateHomePictures(UpdateHostelPicturesDTO dto) {
        EntityHomes home = homeRepository.findById(dto.getPropertyId())
                .orElseThrow(() -> new IllegalArgumentException("Home not found with id: " + dto.getPropertyId()));

        try {
            List<String> updatedPicturePaths = imageService.updateImages(
                    home.getPicturePaths(),
                    dto.getPicturesToRemove(),
                    dto.getNewPictures(),
                    "home",
                    dto.getPropertyId().toString()
            );
            home.setPicturePaths(updatedPicturePaths);
        } catch (IOException e) {
            throw new RuntimeException("Failed to update images", e);
        }

        EntityHomes updated = homeRepository.save(home);
        return convertToDTO(updated);
    }

    @Override
    public HomeDetailsDTO getHomeById(Long homeID) {
        EntityHomes home=homeRepository.findById(homeID).orElseThrow(()-> new RuntimeException("Building not found with id: " + homeID));

        return convertHomeDetailsToDTO(home);
    }

    @Override
    public String deleteHomeById(Long id){
        EntityHomes home=homeRepository.findById(id).orElseThrow(()-> new RuntimeException("Home not found with ID: " + id));

        imageService.deleteAllImages("home",id.toString());
        homeRepository.delete(home);

        return "home has been deleted";
    }

    private HomeSaveRequestDTO convertToDTO(EntityHomes home) {
        HomeSaveRequestDTO dto = new HomeSaveRequestDTO();
        dto.setId(home.getId());
        dto.setHomeType(home.getHomeType());
        dto.setNumberOfRooms(home.getNumberOfRooms());
        dto.setNumberOfBathrooms(home.getNumberOfBathrooms());
        dto.setHasLivingRoom(home.isHasLivingRoom());
        dto.setFurnished(home.isFurnished());
        dto.setRented(home.isRented());
        dto.setRegion(home.getRegion());
        dto.setWilaya(home.getWilaya());
        dto.setLocation(home.getLocation());
        dto.setPrice(home.getPrice());
        dto.setTenantType(home.getTenantType());
        dto.setRentType(home.getRentType());
        dto.setLatitude(home.getLatitude());
        dto.setLongitude(home.getLongitude());

        if (home.getLandlord() != null) {
            dto.setLandlordMobileNumber(home.getLandlord().getPhoneNumber());
        }

        // Convert image paths to base64 for UI
        if (home.getPicturePaths() != null) {
            List<String> pictures = home.getPicturePaths().stream()
                    .map(path -> {
                        try {
                            byte[] imageBytes = Files.readAllBytes(Path.of(path));
                            return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(imageBytes);
                        } catch (IOException e) {
                            return null;
                        }
                    })
                    .filter(p -> p != null)
                    .collect(Collectors.toList());

            dto.setPicturePaths(pictures);
        }

        return dto;
    }

    private HomeDetailsDTO convertHomeDetailsToDTO(EntityHomes home) {
        HomeDetailsDTO dto = new HomeDetailsDTO();

        dto.setId(home.getId());
        dto.setHomeType(home.getHomeType());
        dto.setNumberOfRooms(home.getNumberOfRooms());
        dto.setNumberOfBathrooms(home.getNumberOfBathrooms());
        dto.setHasLivingRoom(home.isHasLivingRoom());
        dto.setFurnished(home.isFurnished());
        dto.setRented(home.isRented());
        dto.setFloorNumber(home.getFloorNumber());
        dto.setDescription(home.getDescription());
        dto.setRegion(home.getRegion());
        dto.setWilaya(home.getWilaya());
        dto.setLocation(home.getLocation());
        dto.setLatitude(home.getLatitude());
        dto.setLongitude(home.getLongitude());
        dto.setTenantType(home.getTenantType());
        dto.setRentType(home.getRentType());
        dto.setPrice(home.getPrice());

        // Convert file paths to image URLs
        List<String> imageUrls = home.getPicturePaths().stream()
                .map(path -> {
                    Path p = Paths.get(path);
                    String fileName = p.getFileName().toString();
                    return "http://localhost:8008/api/images/homes/" + home.getId() + "/" + fileName;
                })
                .toList();
        dto.setPictureUrls(imageUrls);

        // Set landlord details if available
        if (home.getLandlord() != null) {
            dto.setLandlordName(home.getLandlord().getFirstName() + " " + home.getLandlord().getLastName());
            dto.setLandlordMobileNumber(home.getLandlord().getPhoneNumber());
        }

        return dto;
    }

    private EntityHomes convertToEntity(HomeSaveRequestDTO home) {
        EntityHomes entity = new EntityHomes();
        entity.setId(home.getId());
        entity.setHomeType(home.getHomeType());
        entity.setNumberOfRooms(home.getNumberOfRooms());
        entity.setNumberOfBathrooms(home.getNumberOfBathrooms());
        entity.setHasLivingRoom(home.isHasLivingRoom());
        entity.setFurnished(home.isFurnished());
        entity.setRented(home.isRented());
        entity.setFloorNumber(home.getFloorNumber());
        entity.setDescription(home.getDescription());
        entity.setRegion(home.getRegion());
        entity.setWilaya(home.getWilaya());
        entity.setLocation(home.getLocation());
        entity.setLatitude(home.getLatitude());
        entity.setLongitude(home.getLongitude());
        entity.setTenantType(home.getTenantType());
        entity.setRentType(home.getRentType());
        entity.setPrice(home.getPrice());
        if (home.getLandlordMobileNumber() != null) {
            entity.setLandlord(landlordRepository.findByPhoneNumber(home.getLandlordMobileNumber()));
        }
        return entity;
    }


}
