package com.AQARK.AQARK_RENT_MANAGEMENT.Controller;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.BuildingDTO.BuildingIdDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.FlatDTO.FlatIdDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.HomeDTO.HomeIdDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.RoomDTO.RoomDetailsDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.RoomDTO.RoomIdDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.RoomDTO.RoomUpdateRequestDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Exceptions.ApiResponse;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface.RoomsServicesInterface;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomsController {

    private final RoomsServicesInterface roomService;
    private final ObjectMapper objectMapper;

    @PostMapping(value = "/updateRoom", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RoomDetailsDTO updateRoom(
            @RequestPart("room") String roomJson,
            @RequestPart(value = "newImages", required = false) List<MultipartFile> newPictures,
            @RequestPart(value = "imageIdsToDelete", required = false) String imagesToDeleteJson,
            Authentication authentication
    ) throws IOException {

        Long landlordId = extractLandlordId(authentication);
        RoomUpdateRequestDTO dto = objectMapper.readValue(roomJson, RoomUpdateRequestDTO.class);

        List<Long> imagesToDelete = parseIds(imagesToDeleteJson);

        return roomService.updateRoom(dto.getRoomId(), dto, safeFiles(newPictures), imagesToDelete, landlordId);
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

    @PostMapping("/deleteRoom")
    public ResponseEntity<?> deleteRoomById(@RequestBody RoomIdDTO room) {
        roomService.deleteRoom(room.getRoomId());
       return ResponseEntity.ok(ApiResponse.success("Room deleted successfully", null));

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

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Invalid authentication.");
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof OAuth2User oauth2User)) {
            throw new IllegalStateException(
                    "Unsupported authentication principal: " +
                            (principal == null
                                    ? "null"
                                    : principal.getClass().getName())
            );
        }

        Object refId = oauth2User.getAttribute("refId");

        if (refId == null) {
            throw new IllegalStateException(
                    "Authenticated user does not contain refId."
            );
        }

        try {
            return Long.valueOf(refId.toString());

        } catch (NumberFormatException ex) {

            throw new IllegalStateException(
                    "Invalid refId in authenticated user: " + refId,
                    ex
            );
        }
    }
}