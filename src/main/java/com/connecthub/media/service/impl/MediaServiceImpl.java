package com.connecthub.media.service.impl;

import com.connecthub.media.entity.MediaFile;
import com.connecthub.media.repository.MediaRepository;
import com.connecthub.media.service.MediaService;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

@Service
@Transactional
public class MediaServiceImpl implements MediaService {

    private static final Logger log = Logger.getLogger(MediaServiceImpl.class.getName());

    private final MediaRepository mediaRepository;

    @Value("${media.local.upload-dir:${user.home}/connecthub-uploads}")
    private String uploadDir;

    @Value("${media.thumbnail.width:300}")
    private int thumbnailWidth;

    @Value("${media.thumbnail.height:300}")
    private int thumbnailHeight;

    // Allowed image types
    private static final List<String> IMAGE_TYPES = List.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    // Allowed document types
    private static final List<String> DOCUMENT_TYPES = List.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/zip"
    );

    public MediaServiceImpl(MediaRepository mediaRepository) {
        this.mediaRepository = mediaRepository;
    }

    // ─── Upload File (Documents) ──────────────────────────────────────────────

    @Override
    public MediaFile uploadFile(MultipartFile file, Integer uploaderId, Integer roomId) {
        validateFile(file);
        validateFileType(file.getContentType(), false);

        String storedFilename = generateFilename(file.getOriginalFilename());
        String fileUrl = saveToLocal(file, storedFilename);

        MediaFile mediaFile = MediaFile.builder()
                .uploaderId(uploaderId)
                .roomId(roomId)
                .filename(storedFilename)
                .originalName(file.getOriginalFilename())
                .url(fileUrl)
                .mimeType(file.getContentType())
                .sizeKb(file.getSize() / 1024)
                .storageType("LOCAL")
                .build();

        MediaFile saved = mediaRepository.save(mediaFile);
        log.info("File uploaded: " + saved.getOriginalName() + " by userId=" + uploaderId);
        return saved;
    }

    // ─── Upload Image (with thumbnail) ────────────────────────────────────────

    @Override
    public MediaFile uploadImage(MultipartFile file, Integer uploaderId, Integer roomId) {
        validateFile(file);
        validateFileType(file.getContentType(), true);

        String storedFilename = generateFilename(file.getOriginalFilename());
        String fileUrl = saveToLocal(file, storedFilename);

        // Generate thumbnail
        String thumbnailFilename = "thumb_" + storedFilename;
        String thumbnailUrl = generateThumbnail(file, thumbnailFilename);

        MediaFile mediaFile = MediaFile.builder()
                .uploaderId(uploaderId)
                .roomId(roomId)
                .filename(storedFilename)
                .originalName(file.getOriginalFilename())
                .url(fileUrl)
                .thumbnailUrl(thumbnailUrl)
                .mimeType(file.getContentType())
                .sizeKb(file.getSize() / 1024)
                .storageType("LOCAL")
                .build();

        MediaFile saved = mediaRepository.save(mediaFile);
        log.info("Image uploaded: " + saved.getOriginalName() + " by userId=" + uploaderId);
        return saved;
    }

    // ─── Get File By ID ───────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Optional<MediaFile> getFileById(Integer mediaId) {
        return mediaRepository.findByMediaId(mediaId);
    }

    // ─── Get Files By Room ────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<MediaFile> getFilesByRoom(Integer roomId) {
        return mediaRepository.findByRoomId(roomId);
    }

    // ─── Get Images By Room (Media Gallery) ───────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<MediaFile> getImagesByRoom(Integer roomId) {
        return mediaRepository.findImagesByRoomId(roomId);
    }

    // ─── Get Documents By Room ────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<MediaFile> getDocumentsByRoom(Integer roomId) {
        return mediaRepository.findFilesByRoomId(roomId);
    }

    // ─── Get Files By Uploader ────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<MediaFile> getFilesByUploader(Integer uploaderId) {
        return mediaRepository.findByUploaderId(uploaderId);
    }

    // ─── Delete File ──────────────────────────────────────────────────────────

    @Override
    public void deleteFile(Integer mediaId) {
        MediaFile mediaFile = mediaRepository.findByMediaId(mediaId)
                .orElseThrow(() -> new RuntimeException("File not found with id: " + mediaId));

        // Delete from local storage
        deleteFromLocal(mediaFile.getFilename());
        if (mediaFile.getThumbnailUrl() != null) {
            deleteFromLocal("thumb_" + mediaFile.getFilename());
        }

        mediaRepository.deleteByMediaId(mediaId);
        log.info("File deleted: mediaId=" + mediaId);
    }

    // ─── Get File Count ───────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public int getFileCount(Integer roomId) {
        return mediaRepository.countByRoomId(roomId);
    }

    // ─── Get All Files (Admin) ────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<MediaFile> getAllFiles() {
        return mediaRepository.findAll();
    }

    // ─── Helper Methods ───────────────────────────────────────────────────────

    private String generateFilename(String originalName) {
        String extension = "";
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf("."));
        }
        return UUID.randomUUID().toString() + extension;
    }

    // ─── FIXED: Windows-compatible file save ──────────────────────────────────
    private String saveToLocal(MultipartFile file, String filename) {
        try {
            // toAbsolutePath() + normalize() — Windows aur Linux dono pe sahi kaam karta hai
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path filePath = uploadPath.resolve(filename);

            // transferTo() ki jagah Files.copy — Windows pe reliable hai
            try (var inputStream = file.getInputStream()) {
                Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            }

            log.info("File saved to: " + filePath.toString());
            return "/media/files/" + filename;

        } catch (IOException e) {
            throw new RuntimeException("Failed to save file: " + e.getMessage(), e);
        }
    }

    // ─── FIXED: Windows-compatible thumbnail generation ───────────────────────
    private String generateThumbnail(MultipartFile file, String thumbnailFilename) {
        try {
            // Same fix — absolute path use karo
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path thumbPath = uploadPath.resolve(thumbnailFilename);

            Thumbnails.of(file.getInputStream())
                    .size(thumbnailWidth, thumbnailHeight)
                    .keepAspectRatio(true)
                    .toFile(thumbPath.toFile());

            log.info("Thumbnail saved to: " + thumbPath.toString());
            return "/media/files/" + thumbnailFilename;

        } catch (IOException e) {
            log.warning("Failed to generate thumbnail: " + e.getMessage());
            return null;
        }
    }

    private void deleteFromLocal(String filename) {
        try {
            Path filePath = Paths.get(uploadDir).toAbsolutePath().normalize().resolve(filename);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.warning("Failed to delete file: " + filename);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File cannot be empty");
        }
        // Max 25MB
        if (file.getSize() > 25 * 1024 * 1024) {
            throw new RuntimeException("File size exceeds 25MB limit");
        }
    }

    private void validateFileType(String contentType, boolean isImage) {
        if (contentType == null) {
            throw new RuntimeException("File type could not be determined");
        }
        if (isImage && !IMAGE_TYPES.contains(contentType)) {
            throw new RuntimeException(
                    "Invalid image type. Allowed: JPEG, PNG, GIF, WebP");
        }
        if (!isImage && !DOCUMENT_TYPES.contains(contentType) && !IMAGE_TYPES.contains(contentType)) {
            throw new RuntimeException(
                    "Invalid file type. Allowed: PDF, DOCX, XLSX, ZIP, Images");
        }
    }
}
