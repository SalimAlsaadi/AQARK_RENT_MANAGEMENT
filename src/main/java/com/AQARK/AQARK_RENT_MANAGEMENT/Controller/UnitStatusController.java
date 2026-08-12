package com.AQARK.AQARK_RENT_MANAGEMENT.Controller;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.StatusDTO.*;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Enums.StatusDomain;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Enums.StatusLevel;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface.UnitStatusServiceInterface;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/unit-statuses")
@RequiredArgsConstructor
public class UnitStatusController {

    private final UnitStatusServiceInterface unitStatusService;

    /*
     * ============================================================
     * FRONTEND LOOKUPS
     * ============================================================
     */

    /**
     * Returns every active status from all domains.
     *
     * This endpoint is mainly useful for administrative lookup.
     */
    @GetMapping("/getAllActiveStatuses")
    public ResponseEntity<List<UnitStatusDTO>> getAllActiveStatuses() {

        return ResponseEntity.ok(unitStatusService.getAllActiveStatuses());
    }

    /**
     * Returns all statuses, including inactive statuses.
     *
     * Intended for the status administration page.
     */
    @GetMapping("/getAllStatuses")
    public ResponseEntity<List<UnitStatusDTO>> getAllStatuses() {

        return ResponseEntity.ok(unitStatusService.getAllStatuses());
    }

    /**
     * Returns one status by its ID.
     */
    @PostMapping("/getStatusById")
    public ResponseEntity<UnitStatusDTO> getStatusById(@Valid @RequestBody StatusIdDTO dto) {

        return ResponseEntity.ok(unitStatusService.getStatusById(dto.getStatusId()));
    }

    /**
     * Returns one status using domain and code.
     *
     * Example:
     * {
     *   "statusDomain": "ROOM",
     *   "code": "AVAILABLE"
     * }
     */
    @PostMapping("/getStatusByCode")
    public ResponseEntity<UnitStatusDTO> getStatusByCode(@Valid @RequestBody DomainWithCodeDTO dto) {

        return ResponseEntity.ok(unitStatusService.getStatusByCode(dto.getStatusDomain(), dto.getCode()));
    }

    /**
     * Returns all active statuses belonging to a domain.
     *
     * This may include S, U and L statuses.
     */
    @PostMapping("/getActiveStatusesByDomain")
    public ResponseEntity<List<UnitStatusDTO>>
    getActiveStatusesByDomain(@Valid @RequestBody StatusDomainDTO dto) {

        StatusDomain domain = parseStatusDomain(dto.getStatusDomain());

        return ResponseEntity.ok(unitStatusService.getActiveStatusesByDomain(domain));
    }

    /**
     * Returns statuses by domain and lifecycle level.
     *
     * Example for property registration:
     * {
     *   "statusDomain": "ROOM",
     *   "statusLevel": "S"
     * }
     */
    @PostMapping("/getActiveStatusesByDomainAndLevel")
    public ResponseEntity<List<UnitStatusDTO>> getActiveStatusesByDomainAndLevel(@Valid @RequestBody StatusDomainLevelDTO dto) {

        StatusDomain domain = parseStatusDomain(dto.getStatusDomain());

        StatusLevel level = parseStatusLevel(dto.getStatusLevel());

        return ResponseEntity.ok(unitStatusService.getActiveStatusesByDomainAndLevel(domain, level));
    }

    /**
     * Dedicated property-registration endpoint.
     *
     * It returns only active S-level statuses for the selected
     * property domain.
     *
     * Example:
     * {
     *   "statusDomain": "FLAT"
     * }
     */
    @PostMapping("/getRegistrationStatusesByDomain")
    public ResponseEntity<List<UnitStatusDTO>>
    getRegistrationStatusesByDomain(
            @Valid @RequestBody StatusDomainDTO dto
    ) {

        StatusDomain domain =
                parseStatusDomain(dto.getStatusDomain());

        return ResponseEntity.ok(
                unitStatusService
                        .getSetupStatusesByDomain(domain)
        );
    }

    /*
     * ============================================================
     * ADMINISTRATION
     * ============================================================
     */

    /**
     * Creates a new status.
     */
    @PostMapping("/createStatus")
    public ResponseEntity<UnitStatusDTO> createStatus(
            @Valid @RequestBody UnitStatusSaveRequestDTO dto
    ) {

        UnitStatusDTO createdStatus =
                unitStatusService.createStatus(dto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdStatus);
    }

    /**
     * Updates an existing status.
     *
     * The status ID is included in the body.
     */
    @PutMapping("/updateStatus")
    public ResponseEntity<UnitStatusDTO> updateStatus(@Valid @RequestBody UnitStatusUpdateRequestDTO dto) {

        UnitStatusDTO updatedStatus = unitStatusService.updateStatus(dto.getStatusId(), dto);

        return ResponseEntity.ok(updatedStatus);
    }

    /**
     * Activates a status using the ID from the request body.
     */
    @PatchMapping("/activateStatus")
    public ResponseEntity<Void> activateStatus(
            @Valid @RequestBody StatusIdDTO dto
    ) {

        unitStatusService.activateStatus(
                dto.getStatusId()
        );

        return ResponseEntity.noContent().build();
    }

    /**
     * Deactivates a status using the ID from the request body.
     */
    @PatchMapping("/deactivateStatus")
    public ResponseEntity<Void> deactivateStatus(
            @Valid @RequestBody StatusIdDTO dto
    ) {

        unitStatusService.deactivateStatus(
                dto.getStatusId()
        );

        return ResponseEntity.noContent().build();
    }

    /*
     * ============================================================
     * ENUM PARSING
     * ============================================================
     */

    private StatusDomain parseStatusDomain(
            String value
    ) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Status domain is required."
            );
        }

        String normalized =
                value.trim()
                        .toUpperCase()
                        .replace(' ', '_')
                        .replace('-', '_');

        try {
            return StatusDomain.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Unsupported status domain: " + value
            );
        }
    }

    private StatusLevel parseStatusLevel(
            String value
    ) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Status level is required."
            );
        }

        String normalized =
                value.trim().toUpperCase();

        try {
            return StatusLevel.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Unsupported status level: "
                            + value
                            + ". Allowed values are S, U and L."
            );
        }
    }
}