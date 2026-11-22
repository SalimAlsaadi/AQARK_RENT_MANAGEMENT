package com.AQARK.AQARK_RENT_MANAGEMENT.Repositories;


import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityRequestTrack;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RequestTrackRepository extends JpaRepository<EntityRequestTrack, Long> {

    List<EntityRequestTrack> findAllByRentalRequestIdOrderByIdAsc(Long requestId);

    List<EntityRequestTrack> findAllByRentalRequestIdOrderByIdDesc(Long requestId);

    Optional<EntityRequestTrack> findFirstByRentalRequestIdOrderByIdDesc(Long requestId);
}