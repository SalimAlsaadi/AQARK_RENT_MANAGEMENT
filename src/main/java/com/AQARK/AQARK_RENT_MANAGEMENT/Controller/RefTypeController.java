package com.AQARK.AQARK_RENT_MANAGEMENT.Controller;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.RefType;
import com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface.RefTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ref-types")
@RequiredArgsConstructor
public class RefTypeController {

    private final RefTypeService service;

    @PostMapping
    public RefType create(@RequestBody RefType refType) {
        return service.create(refType);
    }

    @GetMapping
    public List<RefType> getAll() {
        return service.getAll();
    }

    @GetMapping("/{code}")
    public RefType getByCode(@PathVariable String code) {
        return service.getByCode(code);
    }

    @PutMapping("/{id}")
    public RefType update(@PathVariable Integer id, @RequestBody RefType refType) {
        return service.update(id, refType);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }
}

