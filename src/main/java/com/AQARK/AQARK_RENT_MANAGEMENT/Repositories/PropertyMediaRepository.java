package com.AQARK.AQARK_RENT_MANAGEMENT.Repositories;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Enums.PropertyType;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.PropertyMedia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PropertyMediaRepository extends JpaRepository<PropertyMedia, Long> {

    List<PropertyMedia> findByOwnerTypeAndOwnerIdAndDeletedFalseOrderBySortOrderAsc(
            PropertyType ownerType,
            Long ownerId
    );

    Optional<PropertyMedia> findByOwnerTypeAndOwnerIdAndMainImageTrueAndDeletedFalse(
            PropertyType ownerType,
            Long ownerId
    );

    List<PropertyMedia> findByIdInAndOwnerTypeAndOwnerIdAndDeletedFalse(
            List<Long> ids,
            PropertyType ownerType,
            Long ownerId
    );
}
