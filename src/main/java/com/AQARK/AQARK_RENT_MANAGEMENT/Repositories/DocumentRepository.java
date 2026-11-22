package com.AQARK.AQARK_RENT_MANAGEMENT.Repositories;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.EntityDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRepository extends JpaRepository<EntityDocument, Long> {

    List<EntityDocument> findAllByRequestIdAndActiveTrueOrderByCreatedAtDesc(Long requestId);

    List<EntityDocument> findAllByEntityTypeAndEntityIdAndActiveTrue(String entityType, Long entityId);

    List<EntityDocument> findAllByRequestIdAndDocumentTypeAndActiveTrue(Long requestId, String documentType);
}