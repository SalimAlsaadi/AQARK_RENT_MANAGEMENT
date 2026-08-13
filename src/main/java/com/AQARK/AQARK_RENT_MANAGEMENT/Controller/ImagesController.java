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
            Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path filePath = root.resolve(Paths.get(type, id, fileName)).normalize();

            // Reject anything that escapes the upload root once normalised, so crafted path
            // segments cannot read files elsewhere on the server.
            if (!filePath.startsWith(root)) {
                return ResponseEntity.notFound().build();
            }

            if (!Files.isRegularFile(filePath)) {
                return ResponseEntity.notFound().build();
            }

            Resource image = new UrlResource(filePath.toUri());
            return ResponseEntity.ok()
                    .contentType(resolveContentType(filePath))
                    .body(image);

        } catch (IOException | InvalidPathException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Uploads accept any {@code image/*}, so the stored file is not necessarily a JPEG. Probing
     * the real type keeps PNG/WebP/GIF from being served under the wrong Content-Type.
     */
    private MediaType resolveContentType(Path filePath) throws IOException {
        String probed = Files.probeContentType(filePath);

        if (probed == null) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }

        return MediaType.parseMediaType(probed);
    }
}
