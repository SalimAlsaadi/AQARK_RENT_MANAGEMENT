package com.AQARK.AQARK_RENT_MANAGEMENT.Controller;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityProperty;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Implementation.PropertyServiceImpl;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface.PropertyServiceInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/aip/property")
public class PropertyController {

    private PropertyServiceInterface propertyService;

    @Autowired
    public PropertyController(PropertyServiceImpl propertyService){
        this.propertyService=propertyService;
    }

    @PostMapping
    public EntityProperty addProperty(@RequestBody EntityProperty entityProperty) {
        return propertyService.saveProperty(entityProperty);
    }

    @GetMapping
    public List<EntityProperty> getAllProperties() {
        return propertyService.getAllProperties();
    }

    @GetMapping("/{id}")
    public Optional<EntityProperty> getPropertyById(@PathVariable Long id) {
        return propertyService.getPropertyById(id);
    }

    @DeleteMapping("/{id}")
    public void deleteProperty(@PathVariable Long id) {
        propertyService.deleteProperty(id);
    }
}
