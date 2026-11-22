package com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "request_id",
            nullable = false
    )
    private EntityRequest request;

    @Column(
            name = "entity_type",
            nullable = false,
            length = 30
    )
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(
            name = "document_type",
            nullable = false,
            length = 50
    )
    private String documentType;

    @Column(
            name = "file_name",
            nullable = false,
            length = 255
    )
    private String fileName;

    @Column(
            name = "original_file_name",
            length = 255
    )
    private String originalFileName;

    @Column(
            name = "file_extension",
            length = 20
    )
    private String fileExtension;

    @Column(
            name = "mime_type",
            length = 100
    )
    private String mimeType;

    @Column(
            name = "storage_path",
            nullable = false,
            length = 1000
    )
    private String storagePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(
            name = "checksum",
            length = 128
    )
    private String checksum;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "uploaded_by",
            nullable = false
    )
    private EntityUser uploadedBy;

    @Column(
            name = "active",
            nullable = false
    )
    private Boolean active = true;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        if (active == null) {
            active = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}