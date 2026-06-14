package com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.RoomDTO.RoomDetailsDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.RoomDTO.RoomSaveRequestDTO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface RoomsServicesInterface {

    RoomDetailsDTO createRoom( RoomSaveRequestDTO dto, List<MultipartFile> images) throws IOException;

    RoomDetailsDTO updateRoom(Long roomId, RoomSaveRequestDTO dto, List<MultipartFile> newImages, List<Long> imageIdsToDelete) throws IOException;

    RoomDetailsDTO getRoomById(Long roomId);

    List<RoomDetailsDTO> getAllRooms();

    List<RoomDetailsDTO> getRoomsByBuildingId(Long buildingId);

    List<RoomDetailsDTO> getRoomsByFlatId(Long flatId);

    List<RoomDetailsDTO> getRoomsByHomeId(Long homeId);

    void deleteRoom(Long roomId);
}