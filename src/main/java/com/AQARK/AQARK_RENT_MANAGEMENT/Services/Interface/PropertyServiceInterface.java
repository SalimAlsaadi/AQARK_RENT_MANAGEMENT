package com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.Property;

import java.util.List;
import java.util.Optional;

public interface PropertyServiceInterface {

    Property saveProperty(Property property);

    List<Property> getAllProperties();

    Optional<Property> getPropertyById(Long id);

    void deleteProperty(Long id);
}
