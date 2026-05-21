package com.AQARK.AQARK_RENT_MANAGEMENT.Controller;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.UserType;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface.UserTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ref-types")
@RequiredArgsConstructor
public class UserTypeController {

    private final UserTypeService service;

    @PostMapping
    public UserType create(@RequestBody UserType userType) {
        return service.create(userType);
    }

    @GetMapping
    public List<UserType> getAll() {
        return service.getAll();
    }

    @GetMapping("/{code}")
    public UserType getByCode(@PathVariable String code) {
        return service.getByCode(code);
    }

    @PutMapping("/{id}")
    public UserType update(@PathVariable Integer id, @RequestBody UserType userType) {
        return service.update(id, userType);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }
}

