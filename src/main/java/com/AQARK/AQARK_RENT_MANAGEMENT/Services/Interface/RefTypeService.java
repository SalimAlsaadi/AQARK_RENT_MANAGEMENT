package com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.RefType;

import java.util.List;

public interface RefTypeService {
    RefType create(RefType refType);

    RefType update(Integer id, RefType refType);

    List<RefType> getAll();

    RefType getByCode(String code);

    void delete(Integer id);
}
