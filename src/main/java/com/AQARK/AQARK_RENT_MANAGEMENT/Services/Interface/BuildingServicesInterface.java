package com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.BuildingDTO.BuildingDetailsDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.BuildingDTO.BuildingSaveRequestDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityBuildings;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface BuildingServicesInterface {

    BuildingDetailsDTO createBuilding(BuildingSaveRequestDTO building, List<MultipartFile> pictures) throws IOException;

    BuildingDetailsDTO getBuildingById(Long id);

    List<EntityBuildings> getAllBuildings();

    BuildingDetailsDTO updateBuilding(BuildingDetailsDTO updatedBuilding, List<String> picturesToDelete, List<MultipartFile> newPictures) throws IOException;

    void deleteBuilding(Long id);
}
