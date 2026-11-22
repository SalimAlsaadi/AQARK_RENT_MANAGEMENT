package com.AQARK.AQARK_RENT_MANAGEMENT.Common_Utilities;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.MediaDTO.PropertyMediaDTO;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Enums.PropertyType;
import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.PropertyMedia;
import com.AQARK.AQARK_RENT_MANAGEMENT.Repositories.PropertyMediaRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PropertyMediaService {

    private final PropertyMediaRepository propertyMediaRepository;

    @Value("${upload.directory}")
    private String uploadDir;

    @Value("${media.public-base-url:http://localhost:8008/api/media}")
    private String publicBaseUrl;

    /**
     * Save new images for any owner:
     * BUILDING, FLAT, HOME, ROOM, SHOP
     */
    @Transactional
    public List<PropertyMediaDTO> saveImages(
            List<MultipartFile> files,
            PropertyType ownerType,
            Long ownerId
    ) throws IOException {

        if (files == null || files.isEmpty()) {
            return Collections.emptyList();
        }

        validateOwner(ownerType, ownerId);

        List<PropertyMedia> existingImages = propertyMediaRepository
                                                     .findByOwnerTypeAndOwnerIdAndDeletedFalseOrderBySortOrderAsc(ownerType, ownerId);

        int nextOrder = existingImages.size() + 1;
        boolean hasMainImage = existingImages.stream().anyMatch(PropertyMedia::isMainImage);

        List<PropertyMedia> savedMedia = new ArrayList<>();

        Path ownerFolder = buildOwnerFolder(ownerType, ownerId);
        Files.createDirectories(ownerFolder);

        for (MultipartFile file : files) {
            validateImage(file);

            String originalFileName = cleanFileName(file.getOriginalFilename());
            String extension = getFileExtension(originalFileName);
            String storedFileName = System.currentTimeMillis() + "_" + UUID.randomUUID() + extension;

            Path filePath = ownerFolder.resolve(storedFileName);
            file.transferTo(filePath.toFile());

            String storageKey = buildStorageKey(ownerType, ownerId, storedFileName);
            String fileUrl = buildFileUrl(ownerType, ownerId, storedFileName);

            PropertyMedia media = PropertyMedia.builder()
                    .ownerType(ownerType)
                    .ownerId(ownerId)
                    .fileUrl(fileUrl)
                    .storageKey(storageKey)
                    .originalFileName(originalFileName)
                    .contentType(file.getContentType())
                    .sizeBytes(file.getSize())
                    .mainImage(!hasMainImage && savedMedia.isEmpty())
                    .sortOrder(nextOrder++)
                    .deleted(false)
                    .createdAt(LocalDateTime.now())
                    .build();

            savedMedia.add(propertyMediaRepository.save(media));
        }

        return savedMedia.stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * Get active images for owner.
     */
    public List<PropertyMediaDTO> getImages(
            PropertyType ownerType,
            Long ownerId
    ) {
        validateOwner(ownerType, ownerId);

        return propertyMediaRepository
                .findByOwnerTypeAndOwnerIdAndDeletedFalseOrderBySortOrderAsc(ownerType, ownerId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * Update images:
     * - soft delete selected image IDs
     * - add new images
     */
    @Transactional
    public List<PropertyMediaDTO> updateImages(
            PropertyType ownerType,
            Long ownerId,
            List<Long> imageIdsToDelete,
            List<MultipartFile> newImages
    ) throws IOException {

        validateOwner(ownerType, ownerId);

        if (imageIdsToDelete != null && !imageIdsToDelete.isEmpty()) {
            softDeleteImages(ownerType, ownerId, imageIdsToDelete);
        }

        if (newImages != null && !newImages.isEmpty()) {
            saveImages(newImages, ownerType, ownerId);
        }

        normalizeSortOrder(ownerType, ownerId);

        return getImages(ownerType, ownerId);
    }

    /**
     * Soft delete images only from DB.
     * Physical file remains for recovery/background cleanup.
     */
    @Transactional
    public void softDeleteImages(
            PropertyType ownerType,
            Long ownerId,
            List<Long> imageIds
    ) {
        validateOwner(ownerType, ownerId);

        if (imageIds == null || imageIds.isEmpty()) {
            return;
        }

        List<PropertyMedia> mediaList =
                propertyMediaRepository.findByIdInAndOwnerTypeAndOwnerIdAndDeletedFalse(
                        imageIds,
                        ownerType,
                        ownerId
                );

        for (PropertyMedia media : mediaList) {
            media.setDeleted(true);
            media.setDeletedAt(LocalDateTime.now());
            media.setMainImage(false);
        }

        propertyMediaRepository.saveAll(mediaList);

        ensureMainImage(ownerType, ownerId);
    }

    /**
     * Hard delete image:
     * - remove physical file
     * - remove DB record
     */
    @Transactional
    public void hardDeleteImage(Long mediaId) throws IOException {
        PropertyMedia media = propertyMediaRepository.findById(mediaId)
                .orElseThrow(() -> new IllegalArgumentException("Media not found: " + mediaId));

        Path filePath = Paths.get(uploadDir).resolve(media.getStorageKey());

        Files.deleteIfExists(filePath);

        propertyMediaRepository.delete(media);

        ensureMainImage(media.getOwnerType(), media.getOwnerId());
    }

    /**
     * Soft delete all owner images.
     */
    @Transactional
    public void softDeleteAllImages(
            PropertyType ownerType,
            Long ownerId
    ) {
        validateOwner(ownerType, ownerId);

        List<PropertyMedia> mediaList =
                propertyMediaRepository.findByOwnerTypeAndOwnerIdAndDeletedFalseOrderBySortOrderAsc(
                        ownerType,
                        ownerId
                );

        for (PropertyMedia media : mediaList) {
            media.setDeleted(true);
            media.setDeletedAt(LocalDateTime.now());
            media.setMainImage(false);
        }

        propertyMediaRepository.saveAll(mediaList);
    }

    /**
     * Set one image as main.
     */
    @Transactional
    public void setMainImage(
            PropertyType ownerType,
            Long ownerId,
            Long mediaId
    ) {
        validateOwner(ownerType, ownerId);

        List<PropertyMedia> mediaList =
                propertyMediaRepository.findByOwnerTypeAndOwnerIdAndDeletedFalseOrderBySortOrderAsc(
                        ownerType,
                        ownerId
                );

        boolean found = false;

        for (PropertyMedia media : mediaList) {
            boolean isMain = media.getId().equals(mediaId);
            media.setMainImage(isMain);

            if (isMain) {
                found = true;
            }
        }

        if (!found) {
            throw new IllegalArgumentException("Image does not belong to this owner or is deleted.");
        }

        propertyMediaRepository.saveAll(mediaList);
    }

    /**
     * Reorder images.
     */
    @Transactional
    public void reorderImages(
            PropertyType ownerType,
            Long ownerId,
            List<Long> orderedImageIds
    ) {
        validateOwner(ownerType, ownerId);

        if (orderedImageIds == null || orderedImageIds.isEmpty()) {
            return;
        }

        List<PropertyMedia> mediaList =
                propertyMediaRepository.findByOwnerTypeAndOwnerIdAndDeletedFalseOrderBySortOrderAsc(
                        ownerType,
                        ownerId
                );

        Map<Long, PropertyMedia> mediaMap = new HashMap<>();

        for (PropertyMedia media : mediaList) {
            mediaMap.put(media.getId(), media);
        }

        int order = 1;

        for (Long imageId : orderedImageIds) {
            PropertyMedia media = mediaMap.get(imageId);

            if (media != null) {
                media.setSortOrder(order++);
            }
        }

        propertyMediaRepository.saveAll(mediaList);
    }

    private void ensureMainImage(PropertyType ownerType, Long ownerId) {
        List<PropertyMedia> activeImages =
                propertyMediaRepository.findByOwnerTypeAndOwnerIdAndDeletedFalseOrderBySortOrderAsc(
                        ownerType,
                        ownerId
                );

        boolean hasMain = activeImages.stream().anyMatch(PropertyMedia::isMainImage);

        if (!hasMain && !activeImages.isEmpty()) {
            PropertyMedia first = activeImages.get(0);
            first.setMainImage(true);
            propertyMediaRepository.save(first);
        }
    }

    private void normalizeSortOrder(PropertyType ownerType, Long ownerId) {
        List<PropertyMedia> activeImages =
                propertyMediaRepository.findByOwnerTypeAndOwnerIdAndDeletedFalseOrderBySortOrderAsc(
                        ownerType,
                        ownerId
                );

        int order = 1;

        for (PropertyMedia media : activeImages) {
            media.setSortOrder(order++);
        }

        propertyMediaRepository.saveAll(activeImages);
    }

    private Path buildOwnerFolder(PropertyType ownerType, Long ownerId) {
        return Paths.get(uploadDir)
                .resolve(ownerType.name().toLowerCase())
                .resolve(String.valueOf(ownerId));
    }

    private String buildStorageKey(
            PropertyType ownerType,
            Long ownerId,
            String fileName
    ) {
        return ownerType.name().toLowerCase() + "/" + ownerId + "/" + fileName;
    }

    private String buildFileUrl(
            PropertyType ownerType,
            Long ownerId,
            String fileName
    ) {
        return publicBaseUrl + "/"
                + ownerType.name().toLowerCase()
                + "/"
                + ownerId
                + "/"
                + fileName;
    }

    private void validateOwner(PropertyType ownerType, Long ownerId) {
        if (ownerType == null) {
            throw new IllegalArgumentException("Owner type is required.");
        }

        if (ownerId == null || ownerId <= 0) {
            throw new IllegalArgumentException("Owner ID is invalid.");
        }
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file is empty.");
        }

        String contentType = file.getContentType();

        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Only image files are allowed.");
        }

        long maxSize = 10 * 1024 * 1024;

        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("Image size must not exceed 10MB.");
        }
    }

    private String cleanFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "unknown";
        }

        return Paths.get(fileName).getFileName().toString();
    }

    private String getFileExtension(String filename) {
        int index = filename.lastIndexOf(".");
        return index != -1 ? filename.substring(index).toLowerCase() : "";
    }

    /**
     * Builds the URL the browser should request, at read time, from the row's storage key.
     * The persisted {@code file_url} column is deliberately ignored: it was baked with whatever
     * host {@code media.public-base-url} happened to be at upload time, so rows written on one
     * environment serve dead links on another. The storage key is environment-independent.
     */
    private String publicUrlFor(PropertyMedia media) {
        String storageKey = media.getStorageKey();

        if (storageKey == null || storageKey.isBlank()) {
            return media.getFileUrl();
        }

        return publicBaseUrl + "/" + storageKey;
    }

    private PropertyMediaDTO toDTO(PropertyMedia media) {
        return PropertyMediaDTO.builder()
                .id(media.getId())
                .fileUrl(publicUrlFor(media))
                .originalFileName(media.getOriginalFileName())
                .contentType(media.getContentType())
                .sizeBytes(media.getSizeBytes())
                .mainImage(media.isMainImage())
                .sortOrder(media.getSortOrder())
                .build();
    }
}