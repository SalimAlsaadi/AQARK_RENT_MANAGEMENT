package com.AQARK.AQARK_RENT_MANAGEMENT.Services.Implementation;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.RefType;
import com.AQARK.AQARK_RENT_MANAGEMENT.Repositories.RefTypeRepository;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface.RefTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RefTypeServiceImpl implements RefTypeService {

    private final RefTypeRepository repository;

    @Override
    public RefType create(RefType refType) {
        if (repository.existsByCode(refType.getCode()))
            throw new RuntimeException("RefType already exists");

        return repository.save(refType);
    }

    @Override
    public RefType update(Integer id, RefType refType) {
        RefType existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Not found"));

        existing.setCode(refType.getCode());
        existing.setDescription(refType.getDescription());
        existing.setActive(refType.isActive());

        return repository.save(existing);
    }

    @Override
    public List<RefType> getAll() {
        return repository.findAll();
    }

    @Override
    public RefType getByCode(String code) {
        return repository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("RefType not found"));
    }

    @Override
    public void delete(Integer id) {
        repository.deleteById(id);
    }
}
