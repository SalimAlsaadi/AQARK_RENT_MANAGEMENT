package com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.HomeDTO.HomeDetailsDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.HomeDTO.HomeSaveRequestDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.UpdateHostelPicturesDTO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface HomeServiceInterface {

    List<HomeSaveRequestDTO> getAllHomes();

    List<HomeDetailsDTO> getHomesByLandlord(Long landlordId);

    String saveHome(HomeSaveRequestDTO home, List<MultipartFile> pictures);

    HomeDetailsDTO updateHome(HomeDetailsDTO homeDto,List<String> picturesToDelete, List<MultipartFile> newPictures) throws IOException;

    HomeSaveRequestDTO updateHomePictures(UpdateHostelPicturesDTO dto);

    HomeDetailsDTO getHomeById(Long homeID);

    String deleteHomeById(Long id);
}
