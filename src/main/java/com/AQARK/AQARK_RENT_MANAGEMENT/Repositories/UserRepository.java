package com.AQARK.AQARK_RENT_MANAGEMENT.Repositories;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<EntityUser, Long> {

    EntityUser findByPhoneNumber(String phoneNumber);

    Optional<EntityUser> findBySasUserId(Long sasUserId);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByNationalId(String nationalId);

    boolean existsByNationalIdAndIdNot(String nationalId, Long id);

}