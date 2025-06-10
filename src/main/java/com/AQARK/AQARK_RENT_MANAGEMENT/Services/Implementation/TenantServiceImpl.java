package com.AQARK.AQARK_RENT_MANAGEMENT.Services.Implementation;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.Tenant;
import com.AQARK.AQARK_RENT_MANAGEMENT.Repositories.TenantRepository;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface.TenantServiceInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TenantServiceImpl implements TenantServiceInterface {

    private TenantRepository tenantRepository;

    @Autowired
    public TenantServiceImpl(TenantRepository userRepository){
        this.tenantRepository=userRepository;

    }

    @Override
    public Tenant saveUser(Tenant user) {
        return tenantRepository.save(user);
    }

    @Override
    public List<Tenant> getAllUsers() {
        return tenantRepository.findAll();
    }

    @Override
    public Optional<Tenant> getUserById(Long id) {
        return tenantRepository.findById(id);
    }

    @Override
    public void deleteUser(Long id) {
        tenantRepository.deleteById(id);
    }

}
