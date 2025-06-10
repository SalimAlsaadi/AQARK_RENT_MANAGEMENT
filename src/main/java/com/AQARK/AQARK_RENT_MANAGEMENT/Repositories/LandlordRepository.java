package com.AQARK.AQARK_RENT_MANAGEMENT.Repositories;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.Landlord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LandlordRepository extends JpaRepository<Landlord,Long> {

    Landlord findByPhoneNumber(String phoneNumber);
}
