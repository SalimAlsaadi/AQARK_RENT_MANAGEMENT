package com.AQARK.AQARK_RENT_MANAGEMENT.Services.Implementation;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.MaintenanceRequest;
import com.AQARK.AQARK_RENT_MANAGEMENT.Repositories.MaintenanceRequestRepository;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface.MaintenanceServiceInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MaintenanceServiceImpl implements MaintenanceServiceInterface {

    private MaintenanceRequestRepository maintenanceRequestRepository;

    @Autowired
    public MaintenanceServiceImpl(MaintenanceRequestRepository maintenanceRequestRepository){
        this.maintenanceRequestRepository=maintenanceRequestRepository;
    }

    @Override
    public MaintenanceRequest saveRequest(MaintenanceRequest request) {
        return maintenanceRequestRepository.save(request);
    }

    @Override
    public List<MaintenanceRequest> getAllRequests() {
        return maintenanceRequestRepository.findAll();
    }

    @Override
    public Optional<MaintenanceRequest> getRequestById(Long id) {
        return maintenanceRequestRepository.findById(id);
    }

    @Override
    public void deleteRequest(Long id) {
        maintenanceRequestRepository.deleteById(id);
    }
}
