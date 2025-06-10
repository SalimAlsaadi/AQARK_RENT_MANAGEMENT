package com.AQARK.AQARK_RENT_MANAGEMENT.Controller;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.MaintenanceRequest;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Implementation.MaintenanceServiceImpl;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface.MaintenanceServiceInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/maintenance")
public class MaintenanceController {

    private MaintenanceServiceInterface maintenanceService;

    @Autowired
    public MaintenanceController (MaintenanceServiceImpl maintenanceService){
        this.maintenanceService=maintenanceService;
    }

    @PostMapping
    public MaintenanceRequest createRequest(@RequestBody MaintenanceRequest request) {
        return maintenanceService.saveRequest(request);
    }

    @GetMapping
    public List<MaintenanceRequest> getAllRequests() {
        return maintenanceService.getAllRequests();
    }

    @GetMapping("/{id}")
    public Optional<MaintenanceRequest> getRequestById(@PathVariable Long id) {
        return maintenanceService.getRequestById(id);
    }

    @DeleteMapping("/{id}")
    public void deleteRequest(@PathVariable Long id) {
        maintenanceService.deleteRequest(id);
    }
}
