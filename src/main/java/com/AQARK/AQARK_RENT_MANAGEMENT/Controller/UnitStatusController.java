package com.AQARK.AQARK_RENT_MANAGEMENT.Controller;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.StatusDTO.*;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Enums.StatusDomain;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface.UnitStatusServiceInterface;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/unit-statuses")
@RequiredArgsConstructor
public class UnitStatusController {

    private final UnitStatusServiceInterface unitStatusService;

    /**
     * For frontend dropdown.
     */
    @GetMapping
    public List<UnitStatusDTO> getActiveStatuses() {
        return unitStatusService.getAllActiveStatuses();
    }

    /**
     * For admin management page.
     */
    @GetMapping("/all")
    public List<UnitStatusDTO> getAllStatuses() {
        return unitStatusService.getAllStatuses();
    }

    @PostMapping("/getStatusById")
    public UnitStatusDTO getStatusById(@RequestBody StatusIdDTO status) {
        return unitStatusService.getStatusById(status.getStatusId());
    }

    @PostMapping("/getStatusByCode")
    public UnitStatusDTO getStatusByCode(@RequestBody DomainWithCodeDTO data) {
        return unitStatusService.getStatusByCode( data.getStatusDomain(), data.getCode());
    }

    @PostMapping("/getStatusesByDomain")
    public ResponseEntity<List<UnitStatusDTO>> getActiveStatusesByDomain(@RequestBody StatusDomainDTO domain){
        return ResponseEntity.ok(unitStatusService.getActiveStatusesByDomain(StatusDomain.valueOf(domain.getStatusDomain())));
    }

    @PostMapping
    public UnitStatusDTO createStatus(
            @Valid @RequestBody UnitStatusSaveRequestDTO dto
    ) {
        return unitStatusService.createStatus(dto);
    }

    @PutMapping("/{id}")
    public UnitStatusDTO updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UnitStatusSaveRequestDTO dto
    ) {
        return unitStatusService.updateStatus(id, dto);
    }

    @PatchMapping("/{id}/activate")
    public void activateStatus(
            @PathVariable Long id
    ) {
        unitStatusService.activateStatus(id);
    }

    @PatchMapping("/{id}/deactivate")
    public void deactivateStatus(
            @PathVariable Long id
    ) {
        unitStatusService.deactivateStatus(id);
    }
}