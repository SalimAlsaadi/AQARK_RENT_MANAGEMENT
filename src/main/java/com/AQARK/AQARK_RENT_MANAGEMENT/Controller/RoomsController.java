package com.AQARK.AQARK_RENT_MANAGEMENT.Controller;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.RoomDTO.RoomSaveRequestDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Implementation.RoomsServicesImpl;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface.RoomsServicesInterface;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/rooms")
public class RoomsController {

    RoomsServicesInterface roomService;

    @Autowired
    public RoomsController(RoomsServicesImpl roomService){
        this.roomService=roomService;
    }
    @PostMapping(value = "/save", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> createRoom(
            @RequestPart("roomData") String roomData,
            @RequestPart(value = "pictures", required = false) List<MultipartFile> pictures) throws IOException {

        RoomSaveRequestDTO dto = new ObjectMapper().readValue(roomData, RoomSaveRequestDTO.class);


        // Then call your roomService.saveRoom(dto);
        return ResponseEntity.ok(roomService.saveRoom(dto, pictures));
    }

    @DeleteMapping("/{roomId}")
    public ResponseEntity<String> deleteRoomById(@PathVariable Long roomId){
       return ResponseEntity.ok(roomService.deleteRoom(roomId)) ;
    }

}
