package com.AQARK.AQARK_RENT_MANAGEMENT.Repositories;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.Property;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyRepository extends JpaRepository<Property, Long> {
}
