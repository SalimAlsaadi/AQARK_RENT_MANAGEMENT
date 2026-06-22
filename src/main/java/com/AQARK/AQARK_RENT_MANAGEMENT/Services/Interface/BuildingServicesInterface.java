package com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.BuildingDTO.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface BuildingServicesInterface {

    BuildingDetailsDTO createBuilding(BuildingSaveRequestDTO dto, List<MultipartFile> pictures, Long landlordId) throws IOException;

    BuildingDetailsDTO getBuildingById(Long id);

    List<BuildingListDTO> getBuildingsByLandlordId(Long landlordId);

    List<BuildingListDTO> getAllBuildings();

    BuildingDetailsDTO updateBuilding(BuildingUpdateRequestDTO dto, List<Long> picturesToDelete, List<MultipartFile> newPictures) throws IOException;

    void softDeleteBuilding(Long buildingId);
}