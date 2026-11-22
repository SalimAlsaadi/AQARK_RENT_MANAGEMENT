package com.AQARK.AQARK_RENT_MANAGEMENT.Repositories;


import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RequestRepository extends JpaRepository<EntityRequest, Long> {

    Optional<EntityRequest> findByRequestNumber(String requestNumber);

    List<EntityRequest> findAllByTenantIdOrderByCreatedAtDesc(Long tenantId);

    List<EntityRequest> findAllByLandlordIdOrderByCreatedAtDesc(Long landlordId);

    List<EntityRequest> findAllByTenantIdAndStatusIdOrderByCreatedAtDesc(Long tenantId, Long statusId);

    List<EntityRequest> findAllByLandlordIdAndStatusIdOrderByCreatedAtDesc(Long landlordId, Long statusId);

    List<EntityRequest> findAllByPropertyTypeAndPropertyId(String propertyType, Long propertyId);

    boolean existsByPropertyTypeAndPropertyIdAndStatusId(String propertyType, Long propertyId, Long statusId);
}