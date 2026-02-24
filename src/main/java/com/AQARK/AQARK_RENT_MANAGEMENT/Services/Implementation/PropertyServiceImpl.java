package com.AQARK.AQARK_RENT_MANAGEMENT.Services.Implementation;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityProperty;
import com.AQARK.AQARK_RENT_MANAGEMENT.Repositories.PropertyRepository;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface.PropertyServiceInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PropertyServiceImpl implements PropertyServiceInterface {

    private PropertyRepository propertyRepository;

    @Autowired
    public PropertyServiceImpl(PropertyRepository propertyRepository){
        this.propertyRepository=propertyRepository;
    }

    @Override
    public EntityProperty saveProperty(EntityProperty entityProperty) {
        return propertyRepository.save(entityProperty);
    }

    @Override
    public List<EntityProperty> getAllProperties() {
        return propertyRepository.findAll();
    }

    @Override
    public Optional<EntityProperty> getPropertyById(Long id) {
        return propertyRepository.findById(id);
    }

    @Override
    public void deleteProperty(Long id) {
        propertyRepository.deleteById(id);
    }
}
