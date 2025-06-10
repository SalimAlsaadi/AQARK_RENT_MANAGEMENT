package com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.MaintenanceRequest;

import java.util.List;
import java.util.Optional;

public interface MaintenanceServiceInterface {

    MaintenanceRequest saveRequest(MaintenanceRequest request);

    List<MaintenanceRequest> getAllRequests();

    Optional<MaintenanceRequest> getRequestById(Long id);

    void deleteRequest(Long id);
}
