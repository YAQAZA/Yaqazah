package com.yaqazah.infrastructure.storage.service;

import com.yaqazah.infrastructure.storage.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    /**
     * Endpoint to upload a MultipartFile
     * POST /api/files/upload
     */
    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
        }

        try {
            String fileUrl = fileService.uploadFile(file);
            return ResponseEntity.ok(fileUrl);
        } catch (RuntimeException e) {
            return ResponseEntity.internalServerError().body("Failed to upload file: " + e.getMessage());
        }
    }

    /**
     * Endpoint to delete a file by its Cloudinary public ID
     * DELETE /api/files/{publicId}
     */
    @DeleteMapping("/{publicId}")
    public ResponseEntity<String> deleteFile(@PathVariable String publicId) {
        try {
            fileService.deleteFile(publicId);
            return ResponseEntity.ok("File deleted successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.internalServerError().body("Failed to delete file: " + e.getMessage());
        }
    }
}