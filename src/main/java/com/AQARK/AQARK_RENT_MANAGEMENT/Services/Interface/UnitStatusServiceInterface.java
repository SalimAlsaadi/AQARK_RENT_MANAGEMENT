package com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.StatusDTO.UnitStatusDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.StatusDTO.UnitStatusSaveRequestDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityUnitStatus;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.StatusDomain;

import java.util.List;

public interface UnitStatusServiceInterface {

    UnitStatusDTO createStatus(UnitStatusSaveRequestDTO dto);

    UnitStatusDTO updateStatus(Long id, UnitStatusSaveRequestDTO dto);

    UnitStatusDTO getStatusById(Long id);

    UnitStatusDTO getStatusByCode(StatusDomain domain, String code);

    List<UnitStatusDTO> getActiveStatusesByDomain(StatusDomain domain);

    List<UnitStatusDTO> getAllActiveStatuses();

    List<UnitStatusDTO> getAllStatuses();

    void activateStatus(Long id);

    void deactivateStatus(Long id);

    EntityUnitStatus getRequiredStatus(StatusDomain domain, String code);

    EntityUnitStatus getDefaultBuildingStatus();

    EntityUnitStatus getDefaultUnitStatus();

    EntityUnitStatus getDefaultContractStatus();

    EntityUnitStatus getDefaultMaintenanceStatus();

    EntityUnitStatus getDefaultPaymentStatus();

    boolean isUnitAvailableForRent(String statusCode);

    boolean isUnitOccupied(String statusCode);
}