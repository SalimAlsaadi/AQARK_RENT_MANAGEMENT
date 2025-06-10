package com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.Tenant;

import java.util.List;
import java.util.Optional;

public interface TenantServiceInterface {

    Tenant saveUser(Tenant user);

    List<Tenant> getAllUsers();

    Optional<Tenant> getUserById(Long id);

    void deleteUser(Long id);
}
