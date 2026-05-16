package com.connecthub.media.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "media_files")
public class MediaFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer mediaId;

    // Who uploaded
    @Column(nullable = false)
    private Integer uploaderId;

    // Which room this file belongs to
    private Integer roomId;

    // Which message this file is attached to
    private Integer messageId;

    // Stored file name on S3 (UUID based)
    @Column(nullable = false, length = 300)
    private String filename;

    // Original file name as uploaded by user
    @Column(nullable = false, length = 300)
    private String originalName;

    // S3 URL or local URL
    @Column(nullable = false, length = 1000)
    private String url;

    // Thumbnail URL (only for images)
    @Column(length = 1000)
    private String thumbnailUrl;

    // image/jpeg, image/png, application/pdf etc
    @Column(nullable = false, length = 100)
    private String mimeType;

    // File size in KB
    @Column(nullable = false)
    private Long sizeKb;

    // Only for images
    private Integer width;
    private Integer height;

    // LOCAL or S3
    @Column(nullable = false, length = 10)
    private String storageType = "LOCAL";

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    // ─── No-arg constructor ───────────────────────────────────────────────────
    public MediaFile() {}

    // ─── Getters ──────────────────────────────────────────────────────────────
    public Integer getMediaId() { return mediaId; }
    public Integer getUploaderId() { return uploaderId; }
    public Integer getRoomId() { return roomId; }
    public Integer getMessageId() { return messageId; }
    public String getFilename() { return filename; }
    public String getOriginalName() { return originalName; }
    public String getUrl() { return url; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public String getMimeType() { return mimeType; }
    public Long getSizeKb() { return sizeKb; }
    public Integer getWidth() { return width; }
    public Integer getHeight() { return height; }
    public String getStorageType() { return storageType; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }

    // ─── Setters ──────────────────────────────────────────────────────────────
    public void setMediaId(Integer mediaId) { this.mediaId = mediaId; }
    public void setUploaderId(Integer uploaderId) { this.uploaderId = uploaderId; }
    public void setRoomId(Integer roomId) { this.roomId = roomId; }
    public void setMessageId(Integer messageId) { this.messageId = messageId; }
    public void setFilename(String filename) { this.filename = filename; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }
    public void setUrl(String url) { this.url = url; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public void setSizeKb(Long sizeKb) { this.sizeKb = sizeKb; }
    public void setWidth(Integer width) { this.width = width; }
    public void setHeight(Integer height) { this.height = height; }
    public void setStorageType(String storageType) { this.storageType = storageType; }

    // ─── Builder ──────────────────────────────────────────────────────────────
    public static MediaFileBuilder builder() { return new MediaFileBuilder(); }

    public static class MediaFileBuilder {
        private Integer uploaderId;
        private Integer roomId;
        private Integer messageId;
        private String filename;
        private String originalName;
        private String url;
        private String thumbnailUrl;
        private String mimeType;
        private Long sizeKb;
        private Integer width;
        private Integer height;
        private String storageType = "LOCAL";

        public MediaFileBuilder uploaderId(Integer uploaderId) { this.uploaderId = uploaderId; return this; }
        public MediaFileBuilder roomId(Integer roomId) { this.roomId = roomId; return this; }
        public MediaFileBuilder messageId(Integer messageId) { this.messageId = messageId; return this; }
        public MediaFileBuilder filename(String filename) { this.filename = filename; return this; }
        public MediaFileBuilder originalName(String originalName) { this.originalName = originalName; return this; }
        public MediaFileBuilder url(String url) { this.url = url; return this; }
        public MediaFileBuilder thumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; return this; }
        public MediaFileBuilder mimeType(String mimeType) { this.mimeType = mimeType; return this; }
        public MediaFileBuilder sizeKb(Long sizeKb) { this.sizeKb = sizeKb; return this; }
        public MediaFileBuilder width(Integer width) { this.width = width; return this; }
        public MediaFileBuilder height(Integer height) { this.height = height; return this; }
        public MediaFileBuilder storageType(String storageType) { this.storageType = storageType; return this; }

        public MediaFile build() {
            MediaFile m = new MediaFile();
            m.uploaderId = this.uploaderId;
            m.roomId = this.roomId;
            m.messageId = this.messageId;
            m.filename = this.filename;
            m.originalName = this.originalName;
            m.url = this.url;
            m.thumbnailUrl = this.thumbnailUrl;
            m.mimeType = this.mimeType;
            m.sizeKb = this.sizeKb;
            m.width = this.width;
            m.height = this.height;
            m.storageType = this.storageType;
            return m;
        }
    }

    @Override
    public String toString() {
        return "MediaFile{mediaId=" + mediaId + ", originalName='" + originalName
                + "', mimeType='" + mimeType + "', sizeKb=" + sizeKb + "}";
    }
}
