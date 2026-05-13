package com.connecthub.media.resource;

import com.connecthub.media.entity.MediaFile;
import com.connecthub.media.service.MediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/media")
@SecurityRequirement(name = "bearerAuth")
@Tag(
    name        = "Media & File Management",
    description = "Upload images and documents, retrieve files by room or uploader, " +
                  "serve files for download, and manage the shared media gallery."
)
public class MediaResource {

    private final MediaService mediaService;

    @Value("${media.local.upload-dir:${user.home}/connecthub-uploads}")
    private String uploadDir;

    public MediaResource(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    // ─── POST /media/upload/file ───────────────────────────────────────────────

    @Operation(
        summary     = "Upload a document (PDF, DOCX, ZIP, etc.)",
        description = "Accepts multipart/form-data. Returns the saved MediaFile record with URL."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "File uploaded successfully",
            content = @Content(schema = @Schema(implementation = MediaFile.class))),
        @ApiResponse(responseCode = "400", description = "Invalid file or missing params", content = @Content),
        @ApiResponse(responseCode = "401", description = "JWT missing or invalid", content = @Content)
    })
    @PostMapping(value = "/upload/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MediaFile> uploadFile(
            @Parameter(description = "File to upload", required = true)
                @RequestParam("file") MultipartFile file,
            @Parameter(description = "ID of the user uploading the file", example = "1")
                @RequestParam("uploaderId") Integer uploaderId,
            @Parameter(description = "Room ID to associate file with (optional)", example = "1")
                @RequestParam(value = "roomId", required = false) Integer roomId) {
        return ResponseEntity.ok(mediaService.uploadFile(file, uploaderId, roomId));
    }

    // ─── POST /media/upload/image ──────────────────────────────────────────────

    @Operation(
        summary     = "Upload an image",
        description = "Accepts multipart/form-data. A thumbnail is auto-generated on the server."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Image uploaded successfully",
            content = @Content(schema = @Schema(implementation = MediaFile.class))),
        @ApiResponse(responseCode = "400", description = "Invalid image file", content = @Content)
    })
    @PostMapping(value = "/upload/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MediaFile> uploadImage(
            @Parameter(description = "Image file to upload", required = true)
                @RequestParam("file") MultipartFile file,
            @Parameter(description = "ID of the user uploading the image", example = "1")
                @RequestParam("uploaderId") Integer uploaderId,
            @Parameter(description = "Room ID to associate image with (optional)", example = "1")
                @RequestParam(value = "roomId", required = false) Integer roomId) {
        return ResponseEntity.ok(mediaService.uploadImage(file, uploaderId, roomId));
    }

    // ─── GET /media/{mediaId} ─────────────────────────────────────────────────

    @Operation(summary = "Get media file metadata by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "MediaFile record returned",
            content = @Content(schema = @Schema(implementation = MediaFile.class))),
        @ApiResponse(responseCode = "404", description = "File not found", content = @Content)
    })
    @GetMapping("/{mediaId}")
    public ResponseEntity<?> getFileById(
            @Parameter(description = "Media file ID", example = "1")
            @PathVariable Integer mediaId) {
        Optional<MediaFile> file = mediaService.getFileById(mediaId);
        return file.<ResponseEntity<?>>map(ResponseEntity::ok)
                   .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ─── GET /media/room/{roomId} ─────────────────────────────────────────────

    @Operation(
        summary     = "Get all files shared in a room",
        description = "Returns every file (images + documents) associated with the room."
    )
    @ApiResponse(responseCode = "200", description = "File list returned")
    @GetMapping("/room/{roomId}")
    public ResponseEntity<List<MediaFile>> getFilesByRoom(
            @Parameter(description = "Room ID", example = "1")
            @PathVariable Integer roomId) {
        return ResponseEntity.ok(mediaService.getFilesByRoom(roomId));
    }

    // ─── GET /media/room/{roomId}/images ──────────────────────────────────────

    @Operation(
        summary     = "Get only images shared in a room",
        description = "Used to build the shared media gallery (photo grid view) in the room sidebar."
    )
    @ApiResponse(responseCode = "200", description = "Image list returned")
    @GetMapping("/room/{roomId}/images")
    public ResponseEntity<List<MediaFile>> getImagesByRoom(
            @Parameter(description = "Room ID", example = "1")
            @PathVariable Integer roomId) {
        return ResponseEntity.ok(mediaService.getImagesByRoom(roomId));
    }

    // ─── GET /media/room/{roomId}/documents ───────────────────────────────────

    @Operation(
        summary     = "Get only documents shared in a room",
        description = "Used to build the shared files list (PDFs, DOCX, ZIP etc.) in the room sidebar."
    )
    @ApiResponse(responseCode = "200", description = "Document list returned")
    @GetMapping("/room/{roomId}/documents")
    public ResponseEntity<List<MediaFile>> getDocumentsByRoom(
            @Parameter(description = "Room ID", example = "1")
            @PathVariable Integer roomId) {
        return ResponseEntity.ok(mediaService.getDocumentsByRoom(roomId));
    }

    // ─── GET /media/uploader/{uploaderId} ────────────────────────────────────

    @Operation(summary = "Get all files uploaded by a specific user")
    @ApiResponse(responseCode = "200", description = "File list returned")
    @GetMapping("/uploader/{uploaderId}")
    public ResponseEntity<List<MediaFile>> getFilesByUploader(
            @Parameter(description = "Uploader user ID", example = "1")
            @PathVariable Integer uploaderId) {
        return ResponseEntity.ok(mediaService.getFilesByUploader(uploaderId));
    }

    // ─── DELETE /media/{mediaId} ──────────────────────────────────────────────

    @Operation(
        summary     = "Delete a media file",
        description = "Removes the file record from DB and deletes the physical file from storage."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "File deleted successfully"),
        @ApiResponse(responseCode = "404", description = "File not found", content = @Content)
    })
    @DeleteMapping("/{mediaId}")
    public ResponseEntity<Map<String, String>> deleteFile(
            @Parameter(description = "Media file ID", example = "1")
            @PathVariable Integer mediaId) {
        mediaService.deleteFile(mediaId);
        return ResponseEntity.ok(Map.of("message", "File deleted successfully"));
    }

    // ─── GET /media/room/{roomId}/count ───────────────────────────────────────

    @Operation(summary = "Get total file count for a room")
    @ApiResponse(responseCode = "200", description = "File count returned")
    @GetMapping("/room/{roomId}/count")
    public ResponseEntity<Map<String, Integer>> getFileCount(
            @Parameter(description = "Room ID", example = "1")
            @PathVariable Integer roomId) {
        return ResponseEntity.ok(Map.of("fileCount", mediaService.getFileCount(roomId)));
    }

    // ─── GET /media/all ───────────────────────────────────────────────────────

    @Operation(
        summary     = "Get all media files (Admin only)",
        description = "Returns every file across all rooms and users. Intended for admin/monitoring."
    )
    @ApiResponse(responseCode = "200", description = "All files returned")
    @GetMapping("/all")
    public ResponseEntity<List<MediaFile>> getAllFiles() {
        return ResponseEntity.ok(mediaService.getAllFiles());
    }

    // ─── GET /media/files/{filename} ─────────────────────────────────────────

    @Operation(
        summary     = "Download / serve a file by filename",
        description = "Streams the actual file bytes. Use the `fileUrl` field from the MediaFile record " +
                      "to construct this URL. Returns `Content-Disposition: attachment`."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "File streamed successfully"),
        @ApiResponse(responseCode = "404", description = "File not found on disk", content = @Content)
    })
    @GetMapping("/files/{filename:.+}")
    public ResponseEntity<Resource> serveFile(
            @Parameter(description = "Filename (from MediaFile.fileUrl)", example = "abc123_report.pdf")
            @PathVariable String filename) {
        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path filePath = uploadPath.resolve(filename).normalize();
            if (!filePath.startsWith(uploadPath)) {
                return ResponseEntity.badRequest().build();
            }

            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                String contentType = Files.probeContentType(filePath);
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "inline; filename=\"" + resource.getFilename() + "\"")
                        .contentType(contentType != null
                                ? MediaType.parseMediaType(contentType)
                                : MediaType.APPLICATION_OCTET_STREAM)
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
