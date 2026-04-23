package com.connecthub.media.service;

import com.connecthub.media.entity.MediaFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

public interface MediaService {

    // Upload any file (PDF, DOCX, ZIP etc)
    MediaFile uploadFile(MultipartFile file, Integer uploaderId, Integer roomId);

    // Upload image — auto generates thumbnail
    MediaFile uploadImage(MultipartFile file, Integer uploaderId, Integer roomId);

    // Get file by ID
    Optional<MediaFile> getFileById(Integer mediaId);

    // Get all files in a room (images + documents)
    List<MediaFile> getFilesByRoom(Integer roomId);

    // Get only images in a room (shared media gallery)
    List<MediaFile> getImagesByRoom(Integer roomId);

    // Get only documents in a room
    List<MediaFile> getDocumentsByRoom(Integer roomId);

    // Get all files uploaded by a user
    List<MediaFile> getFilesByUploader(Integer uploaderId);

    // Delete a file
    void deleteFile(Integer mediaId);

    // Get total file count in a room
    int getFileCount(Integer roomId);

    // Get all files (admin)
    List<MediaFile> getAllFiles();
}
