package com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.FlatDTO.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface FlatsServicesInterface {

    FlatDetailsDTO createFlat(FlatSaveRequestDTO dto, List<MultipartFile> pictures, Long landlordId) throws IOException;

    FlatDetailsDTO updateFlat(Long flatId, FlatUpdateRequestDTO dto, List<MultipartFile> newImages, List<Long> imageIdsToDelete) throws IOException;

    FlatDetailsDTO getFlatById(Long flatId);

    List<FlatDetailsDTO> getFlatsByBuildingId(Long buildingId);

    List<FlatDetailsDTO> getFlatsByLandlordId(Long landlordId);

    List<FlatListDTO> getFlatListByBuildingId(Long buildingId);

    List<FlatDetailsDTO> searchFlats(FlatSearchRequestDTO filter);

    void deleteFlat(Long flatId);
}