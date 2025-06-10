package com.AQARK.AQARK_RENT_MANAGEMENT.Repositories;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.FlatDTO.HostelSearchRequestDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityFlats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FlatsRepository extends JpaRepository<EntityFlats, Long> {

    @Query("""
    SELECT f FROM EntityFlats f
    WHERE (:#{#flat.numberOfRooms} IS NULL OR f.numberOfRooms = :#{#flat.numberOfRooms})
      AND (:#{#flat.numberOfBathrooms} IS NULL OR f.numberOfBathrooms = :#{#flat.numberOfBathrooms})
      AND (:#{#flat.hasLivingRoom} IS NULL OR f.hasLivingRoom = :#{#flat.hasLivingRoom})
      AND (:#{#flat.floorNumber} IS NULL OR f.floorNumber = :#{#flat.floorNumber})
      AND (:#{#flat.tenantType} IS NULL OR f.tenantType = :#{#flat.tenantType})
      AND (:#{#flat.rentTimeType} IS NULL OR f.rentalType = :#{#flat.rentTimeType})
      AND (:#{#flat.region} IS NULL OR f.region = :#{#flat.region})
      AND (:#{#flat.wilaya} IS NULL OR f.wilaya = :#{#flat.wilaya})
      AND (:#{#flat.price} IS NULL OR f.price <= :#{#flat.price})
""")
    List<EntityFlats> findFlats(@Param("flat") HostelSearchRequestDTO flat);



    List<EntityFlats> findByLandlordId(Long landlordID);
}
