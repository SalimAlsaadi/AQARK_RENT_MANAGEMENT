package com.AQARK.AQARK_RENT_MANAGEMENT.Repositories;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.MediaOwnerType;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.PropertyMedia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PropertyMediaRepository extends JpaRepository<PropertyMedia, Long> {

    List<PropertyMedia> findByOwnerTypeAndOwnerIdAndDeletedFalseOrderBySortOrderAsc(
            MediaOwnerType ownerType,
            Long ownerId
    );

    Optional<PropertyMedia> findByOwnerTypeAndOwnerIdAndMainImageTrueAndDeletedFalse(
            MediaOwnerType ownerType,
            Long ownerId
    );

    List<PropertyMedia> findByIdInAndOwnerTypeAndOwnerIdAndDeletedFalse(
            List<Long> ids,
            MediaOwnerType ownerType,
            Long ownerId
    );
}
