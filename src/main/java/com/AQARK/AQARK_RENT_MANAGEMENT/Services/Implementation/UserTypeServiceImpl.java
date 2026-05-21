package com.AQARK.AQARK_RENT_MANAGEMENT.Services.Implementation;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.UserType;
import com.AQARK.AQARK_RENT_MANAGEMENT.Repositories.UserTypeRepository;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface.UserTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserTypeServiceImpl implements UserTypeService {

    private final UserTypeRepository repository;

    @Override
    public UserType create(UserType userType) {
        if (repository.existsByCode(userType.getCode()))
            throw new RuntimeException("RefType already exists");

        return repository.save(userType);
    }

    @Override
    public UserType update(Integer id, UserType userType) {
        UserType existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Not found"));

        existing.setCode(userType.getCode());
        existing.setDescription(userType.getDescription());
        existing.setActive(userType.isActive());

        return repository.save(existing);
    }

    @Override
    public List<UserType> getAll() {
        return repository.findAll();
    }

    @Override
    public UserType getByCode(String code) {
        return repository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("RefType not found"));
    }

    @Override
    public void delete(Integer id) {
        repository.deleteById(id);
    }
}
