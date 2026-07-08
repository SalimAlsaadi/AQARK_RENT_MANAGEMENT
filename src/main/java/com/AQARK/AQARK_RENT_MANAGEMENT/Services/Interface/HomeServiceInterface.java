package com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.HomeDTO.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface HomeServiceInterface {

    HomeDetailsDTO createHome(HomeCreateRequestDTO dto, List<MultipartFile> pictures, Long authenticatedLandlordId) throws IOException;

    HomeDetailsDTO updateHome(HomeUpdateRequestDTO dto, List<MultipartFile> newImages, List<Long> imageIdsToDelete, Long authenticatedLandlordId) throws IOException;

    HomeDetailsDTO changeHomeStatus(HomeChangeStatusRequestDTO dto, Long authenticatedLandlordId);

    HomeDetailsDTO getHomeById(Long homeId, Long authenticatedLandlordId);

    List<HomeDetailsDTO> getAllHomes();

    List<HomeDetailsDTO> getHomesByLandlord(Long landlordId, Long authenticatedLandlordId);

    void deleteHome(Long homeId, Long authenticatedLandlordId);
}