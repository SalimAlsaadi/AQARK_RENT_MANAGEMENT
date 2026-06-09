package com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.HomeDTO.HomeDetailsDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.HomeDTO.HomeSaveRequestDTO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface HomeServiceInterface {

    HomeDetailsDTO createHome(HomeSaveRequestDTO dto, List<MultipartFile> pictures, Long landlordId) throws IOException;

    HomeDetailsDTO updateHome( HomeSaveRequestDTO dto, List<MultipartFile> newImages, List<Long> imageIdsToDelete) throws IOException;

    HomeDetailsDTO getHomeById(Long homeId);

    List<HomeDetailsDTO> getAllHomes();

    List<HomeDetailsDTO> getHomesByLandlord(Long landlordId);

    void deleteHomeById(Long homeId);
}