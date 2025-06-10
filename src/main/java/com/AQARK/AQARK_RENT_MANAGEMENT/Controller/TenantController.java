package com.AQARK.AQARK_RENT_MANAGEMENT.Controller;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.Tenant;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Implementation.TenantServiceImpl;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface.TenantServiceInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class TenantController {

    private TenantServiceInterface tenantService;

    @Autowired
    public TenantController(TenantServiceImpl userService){
        this.tenantService=userService;
    }

    @PostMapping("/register")
    public Tenant registerUser(@RequestBody Tenant user) {
        return tenantService.saveUser(user);
    }

    @GetMapping
    public List<Tenant> getAllUsers() {
        return tenantService.getAllUsers();
    }

    @GetMapping("/{id}")
    public Optional<Tenant> getUserById(@PathVariable Long id) {
        return tenantService.getUserById(id);
    }

    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable Long id) {
        tenantService.deleteUser(id);
    }
}
