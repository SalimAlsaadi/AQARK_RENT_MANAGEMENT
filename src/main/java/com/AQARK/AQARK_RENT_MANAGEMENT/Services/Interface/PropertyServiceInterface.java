package com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityProperty;

import java.util.List;
import java.util.Optional;

public interface PropertyServiceInterface {

    EntityProperty saveProperty(EntityProperty entityProperty);

    List<EntityProperty> getAllProperties();

    Optional<EntityProperty> getPropertyById(Long id);

    void deleteProperty(Long id);
}
