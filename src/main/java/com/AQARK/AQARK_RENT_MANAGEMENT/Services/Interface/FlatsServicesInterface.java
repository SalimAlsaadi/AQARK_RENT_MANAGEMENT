package com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.FlatDTO.FlatSaveRequestDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.FlatDTO.HostelSearchRequestDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.FlatDTO.FlatsForShowListDTO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface FlatsServicesInterface {

    String saveFlats(FlatSaveRequestDTO flatDTO, List<MultipartFile> pictures) throws IOException;

    String updateFlat(FlatSaveRequestDTO dto, List<MultipartFile> newImages, List<String> imagesToDelete) throws IOException;

    void deleteFlat(Long flatId);

    List<FlatsForShowListDTO> getAllHostels();

    List<FlatsForShowListDTO> getHostelBySearch(HostelSearchRequestDTO search);

    List<FlatsForShowListDTO> getHostelByLandlordID(long landlordID);

    FlatsForShowListDTO getFlatById(Long id);
}
