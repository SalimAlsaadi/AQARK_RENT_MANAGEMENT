package com.AQARK.AQARK_RENT_MANAGEMENT.Services.Implementation;

import com.AQARK.AQARK_RENT_MANAGEMENT.Common_Utilities.PropertyMediaService;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.BuildingDTO.BuildingIdDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.FlatDTO.FlatDetailsDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.FlatDTO.FlatSaveRequestDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.MediaDTO.PropertyMediaDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.*;
import com.AQARK.AQARK_RENT_MANAGEMENT.Repositories.BuildingRepository;
import com.AQARK.AQARK_RENT_MANAGEMENT.Repositories.FlatsRepository;
import com.AQARK.AQARK_RENT_MANAGEMENT.Repositories.RoomRepository;
import com.AQARK.AQARK_RENT_MANAGEMENT.Repositories.UserRepository;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface.FlatsServicesInterface;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FlatsServicesImpl implements FlatsServicesInterface {

    private final FlatsRepository flatsRepository;
    private final UserRepository userRepository;
    private final BuildingRepository buildingRepository;
    private final RoomRepository roomRepository;
    private final PropertyMediaService propertyMediaService;

    @Override
    @Transactional
    public FlatDetailsDTO createFlat(FlatSaveRequestDTO dto, List<MultipartFile> pictures, Long landlordId) throws IOException {

        dto.setLandlordId(landlordId);
        validateCreateRequest(dto);

        EntityUser landlord = userRepository.findById(dto.getLandlordId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Landlord not found with ID: " + dto.getLandlordId()
                ));

        EntityBuildings building = buildingRepository.findById(dto.getBuildingId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Building not found with ID: " + dto.getBuildingId()
                ));

        EntityFlats flat = new EntityFlats();

        flat.setFlatNumber(dto.getFlat_number());
        flat.setNumberOfRooms(dto.getNumberOfRooms());
        flat.setNumberOfBathrooms(dto.getNumberOfBathrooms());
        flat.setHasLivingRoom(Boolean.TRUE.equals(dto.getHasLivingRoom()));
        flat.setRented(Boolean.TRUE.equals(dto.getRented()));
        flat.setFloorNumber(dto.getFloorNumber());

        flat.setDescription(dto.getDescription());
        flat.setTenantType(dto.getTenantType());
        flat.setRentalType(dto.getRentTimeType());

        flat.setPrice(dto.getPrice());
        flat.setLandlordMobileNumber(dto.getMobileNumber());

        flat.setLandlord(landlord);
        flat.setBuilding(building);

        EntityFlats savedFlat = flatsRepository.save(flat);

        if (pictures != null && !pictures.isEmpty()) {
            propertyMediaService.saveImages(
                    pictures,
                    MediaOwnerType.FLAT,
                    savedFlat.getId()
            );
        }


        return convertToDTO(savedFlat);
    }

    @Override
    @Transactional
    public FlatDetailsDTO updateFlat(FlatSaveRequestDTO dto, List<MultipartFile> newImages, List<Long> imageIdsToDelete) throws IOException {

        if (dto.getFlatId() == null || dto.getFlatId() <= 0) {
            throw new IllegalArgumentException("Flat ID is required.");
        }

        EntityFlats flat = flatsRepository.findById(dto.getFlatId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Flat not found with ID: " + dto.getFlatId()
                ));

        updateFlatFields(flat, dto);

        if (dto.getLandlordId() != null) {
            EntityUser landlord = userRepository.findById(dto.getLandlordId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Landlord not found with ID: " + dto.getLandlordId()
                    ));

            flat.setLandlord(landlord);
        }

        if (dto.getBuildingId() != null) {
            EntityBuildings building = buildingRepository.findById(dto.getBuildingId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Building not found with ID: " + dto.getBuildingId()
                    ));

            flat.setBuilding(building);
        }

        EntityFlats savedFlat = flatsRepository.save(flat);

        propertyMediaService.updateImages(
                MediaOwnerType.FLAT,
                savedFlat.getId(),
                imageIdsToDelete,
                newImages
        );

        return convertToDTO(savedFlat);
    }

    @Override
    public List<FlatDetailsDTO> getFlatsByBuildingId(BuildingIdDTO buildingIdDTO){
        List<EntityFlats> flats=flatsRepository.findByBuildingId(buildingIdDTO.getBuildingId());

        return convertListFlatsToDTO(flats);
    }

    @Override
    public FlatDetailsDTO getFlatById(Long flatId) {

        EntityFlats flat = flatsRepository.findById(flatId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Flat not found with ID: " + flatId
                ));

        return convertToDTO(flat);
    }

    @Override
    public List<FlatDetailsDTO> getFlatsByBuildingId(Long buildingId) {

        if (buildingId == null || buildingId <= 0) {
            throw new IllegalArgumentException("Building ID is required.");
        }

        return flatsRepository.findByBuildingId(buildingId)
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Override
    public List<FlatDetailsDTO> getFlatsByLandlordId(Long landlordId) {

        if (landlordId == null || landlordId <= 0) {
            throw new IllegalArgumentException("Landlord ID is required.");
        }

        return flatsRepository.findByLandlordId(landlordId)
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Override
    @Transactional
    public void deleteFlat(Long flatId) {

        EntityFlats flat = flatsRepository.findById(flatId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Flat not found with ID: " + flatId
                ));

        if (flat.getRooms() != null && !flat.getRooms().isEmpty()) {
            for (EntityRooms room : flat.getRooms()) {
                propertyMediaService.softDeleteAllImages(
                        MediaOwnerType.ROOM,
                        room.getId()
                );

                roomRepository.delete(room);
            }
        }

        propertyMediaService.softDeleteAllImages(
                MediaOwnerType.FLAT,
                flat.getId()
        );

        flatsRepository.delete(flat);
    }

    private void updateFlatFields(
            EntityFlats flat,
            FlatSaveRequestDTO dto
    ) {

        if (dto.getNumberOfRooms() != null) {
            flat.setNumberOfRooms(dto.getNumberOfRooms());
        }

        if (dto.getNumberOfBathrooms() != null) {
            flat.setNumberOfBathrooms(dto.getNumberOfBathrooms());
        }

        if (dto.getHasLivingRoom() != null) {
            flat.setHasLivingRoom(dto.getHasLivingRoom());
        }

        if (dto.getRented() != null) {
            flat.setRented(dto.getRented());
        }

        if (dto.getFloorNumber() != null) {
            flat.setFloorNumber(dto.getFloorNumber());
        }

        if (dto.getDescription() != null) {
            flat.setDescription(dto.getDescription());
        }

        if (dto.getTenantType() != null) {
            flat.setTenantType(dto.getTenantType());
        }

        if (dto.getRentTimeType() != null) {
            flat.setRentalType(dto.getRentTimeType());
        }

        if (dto.getPrice() != null) {
            flat.setPrice(dto.getPrice());
        }

        if (dto.getMobileNumber() != null) {
            flat.setLandlordMobileNumber(dto.getMobileNumber());
        }

    }

    private FlatDetailsDTO convertToDTO(EntityFlats flat) {

        EntityBuildings building = flat.getBuilding();
        EntityUser landlord = flat.getLandlord();

        List<String> imageUrls = propertyMediaService
                .getImages(MediaOwnerType.FLAT, flat.getId())
                .stream()
                .map(PropertyMediaDTO::getFileUrl)
                .toList();

        return FlatDetailsDTO.builder()
                .id(flat.getId())

                .flatNumber(flat.getFlatNumber())
                .numberOfRooms(flat.getNumberOfRooms())
                .numberOfBathrooms(flat.getNumberOfBathrooms())
                .hasLivingRoom(flat.isHasLivingRoom())
                .rented(flat.isRented())
                .floorNumber(flat.getFloorNumber())

                .description(flat.getDescription())
                .region(flat.getBuilding().getRegion())
                .wilaya(flat.getBuilding().getWilaya())
                .area(flat.getBuilding().getArea())
                .tenantType(flat.getTenantType())
                .rentTimeType(flat.getRentalType())

                .price(flat.getPrice())

                .latitude(flat.getBuilding().getLatitude())
                .longitude(flat.getBuilding().getLongitude())

                .landlordId(landlord != null ? landlord.getId() : null)
                .landlordName(
                        landlord != null
                                ? landlord.getFirstName() + " " + landlord.getLastName()
                                : null
                )
                .landlordPhoneNumber(
                        landlord != null ? landlord.getPhoneNumber() : null
                )

                .buildingId(building != null ? building.getId() : null)
                .buildingName(building != null ? building.getBuildingName() : null)

                .hasParking(building != null ? building.isHasParking() : null)
                .hasGym(building != null ? building.isHasGym() : null)
                .hasSwimmingPool(building != null ? building.isHasSwimmingPool() : null)

                .images(imageUrls != null ? imageUrls : Collections.emptyList())
                .build();
    }

    private List<FlatDetailsDTO> convertListFlatsToDTO(List<EntityFlats> flats){

        List<FlatDetailsDTO> flatList=new ArrayList<>();

        for(EntityFlats flat : flats){
            FlatDetailsDTO flatDetails=new FlatDetailsDTO();

            flatDetails.setId(flat.getId());
            flatDetails.setFlatNumber(flat.getFlatNumber());
            flatDetails.setFloorNumber(flat.getFloorNumber());
            flatDetails.setNumberOfRooms(flat.getNumberOfRooms());
            flatDetails.setNumberOfBathrooms(flat.getNumberOfBathrooms());
            flatDetails.setHasLivingRoom(flat.isHasLivingRoom());
            flatDetails.setBuildingId(flat.getBuilding().getId());
            flatDetails.setBuildingName(flat.getBuilding().getBuildingName());
            flatDetails.setHasGym(flat.getBuilding().isHasGym());
            flatDetails.setHasParking(flat.getBuilding().isHasParking());
            flatDetails.setHasSwimmingPool(flat.getBuilding().isHasSwimmingPool());
            flatDetails.setRegion(flat.getBuilding().getRegion());
            flatDetails.setWilaya(flat.getBuilding().getWilaya());
            flatDetails.setArea(flat.getBuilding().getArea());
            flatDetails.setLatitude(flat.getBuilding().getLatitude());
            flatDetails.setLongitude(flat.getBuilding().getLongitude());
            flatDetails.setDescription(flat.getDescription());
            flatDetails.setLandlordId(flat.getLandlord().getId());
            flatDetails.setLandlordName(flat.getLandlord().getFirstName()+ " " + flat.getLandlord().getLastName());
            flatDetails.setLandlordPhoneNumber(flat.getLandlord().getPhoneNumber());
            flatDetails.setRented(flat.isRented());
            flatDetails.setRentTimeType(flat.getRentalType());
            flatDetails.setTenantType(flat.getTenantType());
            flatDetails.setPrice(flat.getPrice());

            List<String> imageUrls = propertyMediaService
                    .getImages(MediaOwnerType.FLAT, flat.getId())
                    .stream()
                    .map(PropertyMediaDTO::getFileUrl)
                    .toList();

            flatDetails.setImages(imageUrls);
            flatList.add(flatDetails);
        }

        return flatList;
    }
    private void validateCreateRequest(FlatSaveRequestDTO dto) {

        if (dto == null) {
            throw new IllegalArgumentException("Flat request body is required.");
        }

        if (dto.getLandlordId() == null || dto.getLandlordId() <= 0) {
            throw new IllegalArgumentException("Landlord ID is required.");
        }

        if (dto.getBuildingId() == null || dto.getBuildingId() <= 0) {
            throw new IllegalArgumentException("Building ID is required.");
        }

        if (dto.getNumberOfRooms() == null || dto.getNumberOfRooms().isEmpty() ) {
            throw new IllegalArgumentException("Number of rooms must be at least 1.");
        }

        if (dto.getNumberOfBathrooms() == null || dto.getNumberOfBathrooms().isEmpty()) {
            throw new IllegalArgumentException("Number of bathrooms must be at least 1.");
        }

        if (dto.getFloorNumber() == null || dto.getFloorNumber().isEmpty()) {
            throw new IllegalArgumentException("Floor number is required.");
        }

        if (dto.getPrice() == null) {
            throw new IllegalArgumentException("Price is required.");
        }
    }



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



