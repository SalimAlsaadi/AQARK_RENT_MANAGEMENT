package com.AQARK.AQARK_RENT_MANAGEMENT.Common_Utilities;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ImageService {

    @Value("${upload.directory}")
    private String uploadDir;


    public List<String> saveImages(List<MultipartFile> files, String hostelType, String entityId) throws IOException {
        List<String> savedPaths = new ArrayList<>();
        String basePath = Paths.get(uploadDir, hostelType.toLowerCase(), String.valueOf(entityId)).toString();
        Files.createDirectories(Paths.get(basePath));

        for (MultipartFile file : files) {
            String extension = getFileExtension(file.getOriginalFilename());
            String fileName = System.currentTimeMillis() + "_" + UUID.randomUUID() + extension;
            Path path = Paths.get(basePath, fileName);
            file.transferTo(path.toFile());
            savedPaths.add(path.toString());
        }

        return savedPaths;
    }


    public List<String> updateImages(List<String> existingImages,
                                     List<String> imagesToDelete,
                                     List<MultipartFile> newImages,
                                     String hostelType,
                                     String entityId) throws IOException {

        // Remove selected images
        if (imagesToDelete != null) {
            for (String imgPath : imagesToDelete) {
                File imgFile = new File(imgPath);
                if (imgFile.exists() && imgFile.isFile()) {
                    imgFile.delete();
                }
            }
        }

        // Remove deleted images from list
        List<String> updatedList = new ArrayList<>(existingImages);
        if (imagesToDelete != null) {
            updatedList.removeAll(imagesToDelete);
        }

        // Add new images
        if (newImages != null && !newImages.isEmpty()) {
            List<String> newlySaved = saveImages(newImages, hostelType, entityId);
            updatedList.addAll(newlySaved);
        }

        return updatedList;
    }


    public void deleteAllImages(String hostelType, String entityId) {
        String folderPath = Paths.get(uploadDir, hostelType.toLowerCase(), String.valueOf(entityId)).toString();
        File folder = new File(folderPath);

        if (folder.exists() && folder.isDirectory()) {
            Arrays.stream(Objects.requireNonNull(folder.listFiles()))
                    .filter(File::isFile)
                    .forEach(File::delete);
            folder.delete(); // Optionally remove folder
        }
    }

    public void deleteFolderIfEmpty(String baseFolder, String subFolder) {
        Path folderPath = Paths.get(uploadDir, baseFolder, subFolder);
        try {
            if (Files.exists(folderPath) && Files.isDirectory(folderPath)) {
                if (Files.list(folderPath).findAny().isEmpty()) {
                    Files.delete(folderPath);
                }
            }
        } catch (IOException e) {
            // Log the issue or rethrow if needed
            e.printStackTrace();
        }
    }


    public List<String> getImages(String hostelType, Long entityId) {
        String folderPath = Paths.get(uploadDir, hostelType.toLowerCase(), String.valueOf(entityId)).toString();
        File folder = new File(folderPath);

        if (!folder.exists() || !folder.isDirectory()) {
            return Collections.emptyList();
        }

        return Arrays.stream(Objects.requireNonNull(folder.listFiles()))
                .filter(File::isFile)
                .map(File::getAbsolutePath)
                .collect(Collectors.toList());
    }


    private String getFileExtension(String filename) {
        int index = filename.lastIndexOf(".");
        return (index != -1) ? filename.substring(index) : "";
    }
}
