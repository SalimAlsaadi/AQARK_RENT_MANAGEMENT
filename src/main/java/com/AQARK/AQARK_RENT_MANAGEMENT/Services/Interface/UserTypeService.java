package com.AQARK.AQARK_RENT_MANAGEMENT.Services.Interface;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.UserType;

import java.util.List;

public interface UserTypeService {
    UserType create(UserType userType);

    UserType update(Integer id, UserType userType);

    List<UserType> getAll();

    UserType getByCode(String code);

    void delete(Integer id);
}
