package com.AQARK.AQARK_RENT_MANAGEMENT.Controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.*;

@RestController
@RequestMapping("/api/images")
public class ImagesController {

    @Value("${upload.directory}")
    private String uploadDir;

    @GetMapping("/{type}/{id}/{fileName}")
    public ResponseEntity<Resource> getImage(@PathVariable String type, @PathVariable String id, @PathVariable String fileName) {

        try {
            Path filePath = Paths.get(uploadDir, type, id, fileName);
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            Resource image = new UrlResource(filePath.toUri());
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(image);

        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
