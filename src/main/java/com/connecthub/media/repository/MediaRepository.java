package com.connecthub.media.repository;

import com.connecthub.media.entity.MediaFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MediaRepository extends JpaRepository<MediaFile, Integer> {

    List<MediaFile> findByUploaderId(Integer uploaderId);

    List<MediaFile> findByRoomId(Integer roomId);

    Optional<MediaFile> findByMessageId(Integer messageId);

    Optional<MediaFile> findByMediaId(Integer mediaId);

    // Get all images in a room (for shared media gallery)
    @Query("SELECT m FROM MediaFile m WHERE m.roomId = :roomId AND m.mimeType LIKE 'image/%' ORDER BY m.uploadedAt DESC")
    List<MediaFile> findImagesByRoomId(@Param("roomId") Integer roomId);

    // Get all files (non-images) in a room
    @Query("SELECT m FROM MediaFile m WHERE m.roomId = :roomId AND m.mimeType NOT LIKE 'image/%' ORDER BY m.uploadedAt DESC")
    List<MediaFile> findFilesByRoomId(@Param("roomId") Integer roomId);

    List<MediaFile> findByMimeTypeStartingWith(String mimeTypePrefix);

    int countByRoomId(Integer roomId);

    void deleteByMediaId(Integer mediaId);
}
