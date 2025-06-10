package com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.RoomDTO.RoomDetailsDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.RoomDTO.RoomSaveRequestDTO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface RoomsServicesInterface {

    String saveRoom(RoomSaveRequestDTO roomDTO, List<MultipartFile> images) throws IOException;

    RoomDetailsDTO getRoomById(Long id);

    List<RoomDetailsDTO> getAllRooms();

    String updateRoom(Long roomId, RoomSaveRequestDTO updatedDTO, List<MultipartFile> newImages, List<String> imagesToDelete) throws IOException;

    String deleteRoom(Long roomId);
}
