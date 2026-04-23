package com.connecthub.media.resource;

import com.connecthub.media.entity.MediaFile;
import com.connecthub.media.service.MediaService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/media")
public class MediaResource {

    private final MediaService mediaService;

    public MediaResource(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    // ─── POST /media/upload/file ───────────────────────────────────────────────
    // Upload document (PDF, DOCX, ZIP)
    @PostMapping("/upload/file")
    public ResponseEntity<MediaFile> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("uploaderId") Integer uploaderId,
            @RequestParam(value = "roomId", required = false) Integer roomId) {
        MediaFile saved = mediaService.uploadFile(file, uploaderId, roomId);
        return ResponseEntity.ok(saved);
    }

    // ─── POST /media/upload/image ──────────────────────────────────────────────
    // Upload image — thumbnail auto-generated
    @PostMapping("/upload/image")
    public ResponseEntity<MediaFile> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("uploaderId") Integer uploaderId,
            @RequestParam(value = "roomId", required = false) Integer roomId) {
        MediaFile saved = mediaService.uploadImage(file, uploaderId, roomId);
        return ResponseEntity.ok(saved);
    }

    // ─── GET /media/{mediaId} ─────────────────────────────────────────────────
    @GetMapping("/{mediaId}")
    public ResponseEntity<?> getFileById(@PathVariable Integer mediaId) {
        Optional<MediaFile> file = mediaService.getFileById(mediaId);
        if (file.isPresent()) {
            return ResponseEntity.ok(file.get());
        }
        return ResponseEntity.notFound().build();
    }

    // ─── GET /media/room/{roomId} ─────────────────────────────────────────────
    // All files in a room
    @GetMapping("/room/{roomId}")
    public ResponseEntity<List<MediaFile>> getFilesByRoom(@PathVariable Integer roomId) {
        return ResponseEntity.ok(mediaService.getFilesByRoom(roomId));
    }

    // ─── GET /media/room/{roomId}/images ──────────────────────────────────────
    // Shared media gallery — only images
    @GetMapping("/room/{roomId}/images")
    public ResponseEntity<List<MediaFile>> getImagesByRoom(@PathVariable Integer roomId) {
        return ResponseEntity.ok(mediaService.getImagesByRoom(roomId));
    }

    // ─── GET /media/room/{roomId}/documents ───────────────────────────────────
    // Only documents in a room
    @GetMapping("/room/{roomId}/documents")
    public ResponseEntity<List<MediaFile>> getDocumentsByRoom(@PathVariable Integer roomId) {
        return ResponseEntity.ok(mediaService.getDocumentsByRoom(roomId));
    }

    // ─── GET /media/uploader/{uploaderId} ────────────────────────────────────
    @GetMapping("/uploader/{uploaderId}")
    public ResponseEntity<List<MediaFile>> getFilesByUploader(
            @PathVariable Integer uploaderId) {
        return ResponseEntity.ok(mediaService.getFilesByUploader(uploaderId));
    }

    // ─── DELETE /media/{mediaId} ──────────────────────────────────────────────
    @DeleteMapping("/{mediaId}")
    public ResponseEntity<Map<String, String>> deleteFile(@PathVariable Integer mediaId) {
        mediaService.deleteFile(mediaId);
        return ResponseEntity.ok(Map.of("message", "File deleted successfully"));
    }

    // ─── GET /media/room/{roomId}/count ───────────────────────────────────────
    @GetMapping("/room/{roomId}/count")
    public ResponseEntity<Map<String, Integer>> getFileCount(@PathVariable Integer roomId) {
        return ResponseEntity.ok(Map.of("fileCount", mediaService.getFileCount(roomId)));
    }

    // ─── GET /media/all ───────────────────────────────────────────────────────
    // Admin — get all files
    @GetMapping("/all")
    public ResponseEntity<List<MediaFile>> getAllFiles() {
        return ResponseEntity.ok(mediaService.getAllFiles());
    }

    // ─── GET /media/files/{filename} ─────────────────────────────────────────
    // Serve actual file (download)
    @GetMapping("/files/{filename:.+}")
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        try {
            Path filePath = Paths.get("uploads").resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"" + resource.getFilename() + "\"")
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
