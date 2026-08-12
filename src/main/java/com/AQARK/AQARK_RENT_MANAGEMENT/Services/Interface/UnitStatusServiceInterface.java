package com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.StatusDTO.UnitStatusDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.StatusDTO.UnitStatusSaveRequestDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.StatusDTO.UnitStatusUpdateRequestDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityUnitStatus;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Enums.StatusDomain;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Enums.StatusLevel;

import java.util.List;

public interface UnitStatusServiceInterface {

    UnitStatusDTO createStatus(UnitStatusSaveRequestDTO dto);
    UnitStatusDTO updateStatus(Long id, UnitStatusUpdateRequestDTO dto);
    UnitStatusDTO getStatusById(Long id);
    UnitStatusDTO getStatusByCode(StatusDomain domain, String code);

    List<UnitStatusDTO> getActiveStatusesByDomain(StatusDomain domain);
    List<UnitStatusDTO> getActiveStatusesByDomainAndLevel(StatusDomain domain, StatusLevel level);
    List<UnitStatusDTO> getSetupStatusesByDomain(StatusDomain domain);
    List<UnitStatusDTO> getAllActiveStatuses();
    List<UnitStatusDTO> getAllStatuses();

    void activateStatus(Long id);
    void deactivateStatus(Long id);

    EntityUnitStatus getRequiredStatus(StatusDomain domain, String code);
    EntityUnitStatus getRequiredStatus(StatusDomain domain, String code, StatusLevel level);
    EntityUnitStatus getRequiredSetupStatus(StatusDomain domain, String code);

    EntityUnitStatus getDefaultBuildingStatus();
    EntityUnitStatus getDefaultStatus(StatusDomain domain);
    EntityUnitStatus getDefaultContractStatus();
    EntityUnitStatus getDefaultMaintenanceStatus();
    EntityUnitStatus getDefaultPaymentStatus();

    boolean isUnitAvailableForRent(String statusCode);
    boolean isUnitOccupied(String statusCode);
}
