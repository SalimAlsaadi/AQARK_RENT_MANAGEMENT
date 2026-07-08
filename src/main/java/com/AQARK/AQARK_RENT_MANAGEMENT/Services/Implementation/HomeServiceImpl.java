package com.AQARK.AQARK_RENT_MANAGEMENT.Services.Implementation;

import com.AQARK.AQARK_RENT_MANAGEMENT.Common_Utilities.PropertyMediaService;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.HomeDTO.*;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.LocationDTO.LocationDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.MediaDTO.PropertyMediaDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.StatusDTO.UnitStatusDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.*;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Enums.PropertyType;
import com.AQARK.AQARK_RENT_MANAGEMENT.Repositories.*;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface.HomeServiceInterface;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class HomeServiceImpl implements HomeServiceInterface {

    private static final String HOME_STATUS_DOMAIN = "HOME";
    private static final String ROOM_STATUS_CODE_RENTED = "RENTED";

    private final OmanLocationServiceImpl omanLocationService;
    /*
     * These statuses are calculated automatically based on rooms.
     * Frontend must not force them manually.
     */
    private static final Set<String> AUTO_MANAGED_HOME_STATUSES = Set.of(
            "AVAILABLE_AS_ROOMS",
            "PARTIALLY_RENTED_AS_ROOMS",
            "FULLY_RENTED_AS_ROOMS"
    );

    private final HomeRepository homeRepository;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;

    private final OmanRegionRepository regionRepository;
    private final OmanWilayaRepository wilayaRepository;
    private final OmanAreaRepository areaRepository;
    private final UnitStatusRepository unitStatusRepository;

    private final PropertyMediaService propertyMediaService;

    @Override
    @Transactional
    public HomeDetailsDTO createHome(HomeCreateRequestDTO dto, List<MultipartFile> pictures, Long authenticatedLandlordId) throws IOException {

        EntityUser landlord = getLandlord(authenticatedLandlordId);

        LocationData locationData = validateAndGetLocation(
                dto.getRegionId(),
                dto.getWilayaId(),
                dto.getAreaId()
        );

        EntityUnitStatus status = resolveInitialHomeStatus(dto.getStatusId());

        EntityHomes home = EntityHomes.builder()
                .homeType(dto.getHomeType())
                .homeNumber(dto.getHomeNumber())
                .numberOfRooms(dto.getNumberOfRooms())
                .numberOfBathrooms(dto.getNumberOfBathrooms())
                .hasLivingRoom(Boolean.TRUE.equals(dto.getHasLivingRoom()))
                .furnished(Boolean.TRUE.equals(dto.getFurnished()))
                .numberOfFloor(dto.getNumberOfFloor())
                .description(dto.getDescription())
                .region(locationData.region())
                .wilaya(locationData.wilaya())
                .area(locationData.area())
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .tenantType(dto.getTenantType())
                .rentType(dto.getRentTimeType())
                .price(dto.getPrice())
                .status(status)
                .landlord(landlord)
                .active(true)
                .deleted(false)
                .build();

        EntityHomes savedHome = homeRepository.save(home);

        if (pictures != null && !pictures.isEmpty()) {
            propertyMediaService.saveImages(
                    pictures,
                    PropertyType.HOME,
                    savedHome.getId()
            );
        }

        return convertToDetailsDTO(savedHome);
    }

    @Override
    @Transactional
    public HomeDetailsDTO updateHome(HomeUpdateRequestDTO dto, List<MultipartFile> newImages, List<Long> imageIdsToDelete, Long authenticatedLandlordId) throws IOException {

        EntityHomes home = getOwnedHome(dto.getId(), authenticatedLandlordId);

        updateHomeFields(home, dto);

        EntityHomes savedHome = homeRepository.save(home);

        propertyMediaService.updateImages(
                PropertyType.HOME,
                savedHome.getId(),
                imageIdsToDelete,
                newImages
        );

        return convertToDetailsDTO(savedHome);
    }

    @Override
    @Transactional
    public HomeDetailsDTO changeHomeStatus(HomeChangeStatusRequestDTO dto, Long authenticatedLandlordId) {

        EntityHomes home = getOwnedHome(dto.getHomeId(), authenticatedLandlordId);

        EntityUnitStatus requestedStatus = getHomeStatusById(dto.getStatusId());

        String requestedCode = requestedStatus.getCode();

        if (AUTO_MANAGED_HOME_STATUSES.contains(requestedCode)) {
            throw new IllegalStateException(
                    "This status is managed automatically based on home rooms."
            );
        }

        if ("RENTED".equalsIgnoreCase(requestedCode)) {
            validateCanRentWholeHome(home);
        }

        home.setStatus(requestedStatus);

        if ("INACTIVE".equalsIgnoreCase(requestedCode)) {
            home.setActive(false);
        } else {
            home.setActive(true);
        }

        EntityHomes savedHome = homeRepository.save(home);

        return convertToDetailsDTO(savedHome);
    }

    @Override
    public HomeDetailsDTO getHomeById(Long homeId, Long landlordId) {

        EntityHomes home = getOwnedHome(homeId, landlordId);

        return convertToDetailsDTO(home);
    }

    @Override
    public List<HomeDetailsDTO> getAllHomes() {

        return homeRepository.findByDeletedFalseOrderByCreatedAtDesc()
                .stream()
                .map(this::convertToListDTO)
                .toList();
    }

    @Override
    public List<HomeDetailsDTO> getHomesByLandlord(Long landlordId, Long authenticatedLandlordId) {

        if (!landlordId.equals(authenticatedLandlordId)) {
            throw new SecurityException("You are not allowed to view homes owned by another landlord.");
        }

        return homeRepository
                .findByLandlordIdAndDeletedFalseOrderByCreatedAtDesc(landlordId)
                .stream()
                .map(this::convertToListDTO)
                .toList();
    }

    @Override
    @Transactional
    public void deleteHome(
            Long homeId,
            Long authenticatedLandlordId
    ) {

        EntityHomes home = getOwnedHome(
                homeId,
                authenticatedLandlordId
        );

        List<EntityRooms> rooms = roomRepository.findDetailsByHomeId(homeId);

        boolean hasRentedRoom = rooms.stream()
                .anyMatch(room ->
                        room.getStatus() != null
                                && ROOM_STATUS_CODE_RENTED.equalsIgnoreCase(
                                room.getStatus().getCode()
                        )
                );

        if (hasRentedRoom) {
            throw new IllegalStateException(
                    "Cannot delete this home because one or more rooms are rented."
            );
        }

        /*
         * Soft delete keeps historical data safer than physical delete.
         */
        EntityUnitStatus inactiveStatus = getHomeStatusByCode("INACTIVE");

        home.setActive(false);
        home.setDeleted(true);
        home.setStatus(inactiveStatus);

        homeRepository.save(home);

        propertyMediaService.softDeleteAllImages(
                PropertyType.HOME,
                homeId
        );
    }

    private EntityHomes getOwnedHome(Long homeId, Long landlordId) {

        if (homeId == null || homeId <= 0) {
            throw new IllegalArgumentException("Home ID is required.");
        }

        EntityHomes home = homeRepository.findByIdAndDeletedFalse(homeId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Home not found with ID: " + homeId
                ));

        if (!home.getLandlord().getId().equals(landlordId)) {
            throw new SecurityException("You are not allowed to access this home.");
        }

        return home;
    }

    private EntityUser getLandlord(Long landlordId) {

        if (landlordId == null || landlordId <= 0) {
            throw new IllegalArgumentException("Authenticated landlord ID is required.");
        }

        return userRepository.findById(landlordId).orElseThrow(() -> new IllegalArgumentException("Landlord not found with ID: " + landlordId));
    }

    private LocationData validateAndGetLocation(Long regionId, Long wilayaId, Long areaId) {

        OmanRegionEntity region = regionRepository.findById(regionId).orElseThrow(() -> new IllegalArgumentException("Region not found with ID: " + regionId));

        OmanWilayaEntity wilaya = wilayaRepository.findByIdAndRegionId(wilayaId, regionId);

        OmanAreaEntity area = areaRepository.findByIdAndWilayaId(areaId, wilayaId);

        return new LocationData(region, wilaya, area);
    }

    private EntityUnitStatus resolveInitialHomeStatus(Long statusId) {

        if (statusId == null) {
            return getHomeStatusByCode("AVAILABLE");
        }

        EntityUnitStatus status = getHomeStatusById(statusId);

        String statusCode = status.getCode();

        if (AUTO_MANAGED_HOME_STATUSES.contains(statusCode)) {
            throw new IllegalArgumentException("Room-based home statuses cannot be selected when creating a home.");
        }

        if ("RENTED".equalsIgnoreCase(statusCode)) {
            throw new IllegalArgumentException("A new home cannot be created directly as RENTED.");
        }

        return status;
    }

    private EntityUnitStatus getHomeStatusById(Long statusId) {

        EntityUnitStatus status = unitStatusRepository.findById(statusId).orElseThrow(() -> new IllegalArgumentException("Status not found or inactive with ID: " + statusId));

        validateHomeStatusDomain(status);

        return status;
    }


    private EntityUnitStatus getHomeStatusByCode(String code) {

        return unitStatusRepository.findByStatusDomainAndCodeAndActiveTrue(HOME_STATUS_DOMAIN, code)
                .orElseThrow(() -> new IllegalStateException("HOME status is not configured: " + code));
    }


    private void validateHomeStatusDomain(EntityUnitStatus status) {

        if (!HOME_STATUS_DOMAIN.equalsIgnoreCase(status.getStatusDomain())) {
            throw new IllegalArgumentException("Selected status does not belong to HOME domain.");
        }
    }

    private void validateCanRentWholeHome(EntityHomes home) {

        List<EntityRooms> rooms = roomRepository.findDetailsByHomeId(home.getId());

        boolean hasRentedRoom = rooms.stream()
                .anyMatch(room -> room.getStatus() != null
                        && ROOM_STATUS_CODE_RENTED.equalsIgnoreCase(room.getStatus().getCode()));

        if (hasRentedRoom) {
            throw new IllegalStateException("Cannot rent the whole home because one or more rooms are already rented.");
        }
    }

    private void updateHomeFields( EntityHomes home, HomeUpdateRequestDTO dto ) {

        if (dto.getHomeType() != null) {
            home.setHomeType(dto.getHomeType());
        }

        if (dto.getHomeNumber() != null) {
            home.setHomeNumber(dto.getHomeNumber());
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

        if (dto.getNumberOfFloor() != null) {
            home.setNumberOfFloor(dto.getNumberOfFloor());
        }

        if (dto.getDescription() != null) {
            home.setDescription(dto.getDescription());
        }

        if (dto.getRegionId() != null || dto.getWilayaId() != null || dto.getAreaId() != null) {

            Long regionId = dto.getRegionId() != null
                    ? dto.getRegionId()
                    : home.getRegion().getId();

            Long wilayaId = dto.getWilayaId() != null
                    ? dto.getWilayaId()
                    : home.getWilaya().getId();

            Long areaId = dto.getAreaId() != null
                    ? dto.getAreaId()
                    : home.getArea().getId();

            LocationData locationData = validateAndGetLocation(regionId, wilayaId, areaId);

            home.setRegion(locationData.region());
            home.setWilaya(locationData.wilaya());
            home.setArea(locationData.area());
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

    private HomeDetailsDTO convertToDetailsDTO(EntityHomes home) {

        EntityUser landlord = home.getLandlord();

        List<PropertyMediaDTO> images = propertyMediaService.getImages(
                PropertyType.HOME,
                home.getId()
        );

        return HomeDetailsDTO.builder()
                .id(home.getId())
                .homeType(home.getHomeType())
                .homeNumber(home.getHomeNumber())
                .numberOfRooms(home.getNumberOfRooms())
                .numberOfBathrooms(home.getNumberOfBathrooms())
                .hasLivingRoom(home.isHasLivingRoom())
                .furnished(home.isFurnished())
                .numberOfFloor(home.getNumberOfFloor())
                .description(home.getDescription())
                .location(convertLocationDTO(home))
                .latitude(home.getLatitude())
                .longitude(home.getLongitude())
                .tenantType(home.getTenantType())
                .rentType(home.getRentType())
                .price(home.getPrice())
                .status(convertStatusDTO(home.getStatus()))
                .landlordId(landlord != null ? landlord.getId() : null)
                .landlordName(getLandlordFullName(landlord))
                .landlordMobileNumber(
                        landlord != null ? landlord.getPhoneNumber() : null
                )
                .active(home.isActive())
                .deleted(home.isDeleted())
                .createdAt(home.getCreatedAt())
                .updatedAt(home.getUpdatedAt())
                .images(images != null ? images : Collections.emptyList())
                .build();
    }

    private HomeDetailsDTO convertToListDTO(EntityHomes home) {

        List<PropertyMediaDTO> images = propertyMediaService.getImages(
                PropertyType.HOME,
                home.getId()
        );

        String mainImageUrl = images == null || images.isEmpty()
                ? null
                : images.getFirst().getFileUrl();

        return HomeDetailsDTO.builder()
                .id(home.getId())
                .homeType(home.getHomeType())
                .homeNumber(home.getHomeNumber())
                .numberOfRooms(home.getNumberOfRooms())
                .numberOfBathrooms(home.getNumberOfBathrooms())
                .numberOfFloor(home.getNumberOfFloor())
                .location(omanLocationService.location(
                        home.getRegion().getId(),
                        home.getWilaya().getId(),
                        home.getArea().getId()))
                .price(home.getPrice())
                .rentType(home.getRentType())
                .status(convertStatusDTO(home.getStatus()))
                .images(images)
                .build();
    }



    private UnitStatusDTO convertStatusDTO(EntityUnitStatus status) {

        return UnitStatusDTO.builder()
                .id(status.getId())
                .code(status.getCode())
                .nameEn(status.getNameEn())
                .nameAr(status.getNameAr())
                .colorCode(status.getColorCode())
                .icon(status.getIcon())
                .build();
    }

    private LocationDTO convertLocationDTO(EntityHomes home) {

        return LocationDTO.builder()
                .regionId(home.getRegion().getId())
                .regionNameEn(home.getRegion().getNameEn())
                .regionNameAr(home.getRegion().getNameAr())
                .wilayaId(home.getWilaya().getId())
                .wilayaNameEn(home.getWilaya().getNameEn())
                .wilayaNameAr(home.getWilaya().getNameAr())
                .areaId(home.getArea().getId())
                .areaNameEn(home.getArea().getNameEn())
                .areaNameAr(home.getArea().getNameAr())
                .build();
    }

    private String getLandlordFullName(EntityUser landlord) {

        if (landlord == null) {
            return null;
        }

        return (
                (landlord.getFirstName() == null
                        ? ""
                        : landlord.getFirstName())
                        + " "
                        + (landlord.getLastName() == null
                        ? ""
                        : landlord.getLastName())
        ).trim();
    }

    private record LocationData(
            OmanRegionEntity region,
            OmanWilayaEntity wilaya,
            OmanAreaEntity area
    ) {
    }
}