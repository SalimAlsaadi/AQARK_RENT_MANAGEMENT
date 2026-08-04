package com.AQARK.AQARK_RENT_MANAGEMENT.Controller;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.BuildingDTO.BuildingIdDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.FlatDTO.FlatIdDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.HomeDTO.HomeIdDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.RoomDTO.RoomDetailsDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.RoomDTO.RoomIdDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.RoomDTO.RoomSaveRequestDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface.RoomsServicesInterface;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomsController {

    private final RoomsServicesInterface roomService;
    private final ObjectMapper objectMapper;

    @PutMapping(value = "/updateRoom", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RoomDetailsDTO updateRoom(@RequestBody RoomIdDTO room, @RequestPart("room") String roomJson,
            @RequestPart(value = "newPictures", required = false) List<MultipartFile> newPictures,
            @RequestPart(value = "imagesToDelete", required = false) String imagesToDeleteJson,
                                     Authentication authentication
    ) throws IOException {

        Long landlordId = extractLandlordId(authentication);
        RoomSaveRequestDTO dto = objectMapper.readValue(roomJson, RoomSaveRequestDTO.class);

        List<Long> imagesToDelete = parseIds(imagesToDeleteJson);

        return roomService.updateRoom(room.getRoomId(), dto, safeFiles(newPictures), imagesToDelete, landlordId);
    }

    @PostMapping("/getRoomById")
    public RoomDetailsDTO getRoomById(@RequestBody RoomIdDTO room) {
        return roomService.getRoomById(room.getRoomId());
    }

    @GetMapping
    public List<RoomDetailsDTO> getAllRooms() {
        return roomService.getAllRooms();
    }

    @PostMapping("/getRoomsByBuildingId")
    public List<RoomDetailsDTO> getRoomsByBuildingId(@RequestBody BuildingIdDTO building) {
        return roomService.getRoomsByBuildingId(building.getBuildingId());
    }

    @PostMapping("/getRoomsByFlatId")
    public List<RoomDetailsDTO> getRoomsByFlatId(@RequestBody FlatIdDTO flat) {
        return roomService.getRoomsByFlatId(flat.getFlatId());
    }

    @PostMapping("/getRoomsByHomeId")
    public List<RoomDetailsDTO> getRoomsByHomeId(@RequestBody HomeIdDTO home) {
        return roomService.getRoomsByHomeId(home.getHomeId());
    }

    @DeleteMapping("/deleteRoomById")
    public void deleteRoomById(@PathVariable Long roomId) {
        roomService.deleteRoom(roomId);
    }

    private List<MultipartFile> safeFiles(List<MultipartFile> files) {
        return files == null ? Collections.emptyList() : files;
    }

    private List<Long> parseIds(String json) throws IOException {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }

        return objectMapper.readValue(
                json,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Long.class)
        );
    }

    private Long extractLandlordId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new IllegalStateException("Invalid authentication.");
        }

        Object refId = jwt.getClaim("refId");

        if (refId == null) {
            throw new IllegalStateException("Missing refId in token.");
        }

        if (refId instanceof Number number) {
            return number.longValue();
        }

        return Long.parseLong(refId.toString());
    }
}