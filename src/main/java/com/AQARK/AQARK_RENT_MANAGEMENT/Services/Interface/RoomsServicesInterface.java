package com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.RoomDTO.RoomCreateRequestDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.RoomDTO.RoomDetailsDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.RoomDTO.RoomUpdateRequestDTO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface RoomsServicesInterface {

    RoomDetailsDTO createRoom(RoomCreateRequestDTO dto, List<MultipartFile> images, Long landlordId) throws IOException;

    RoomDetailsDTO updateRoom(Long roomId, RoomUpdateRequestDTO dto, List<MultipartFile> newImages, List<Long> imageIdsToDelete, Long landlord) throws IOException;

    RoomDetailsDTO getRoomById(Long roomId);

    List<RoomDetailsDTO> getAllRooms();

    List<RoomDetailsDTO> getRoomsByBuildingId(Long buildingId);

    List<RoomDetailsDTO> getRoomsByFlatId(Long flatId);

    List<RoomDetailsDTO> getRoomsByHomeId(Long homeId);

    void deleteRoom(Long roomId);
}