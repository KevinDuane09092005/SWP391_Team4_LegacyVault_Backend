package com.legacyvault.service;

import com.legacyvault.dto.StoredFileInfo;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface FileStorageService {

    /**
     * Store a file in the local file storage under the specified subDirectory.
     *
     * @param file         the uploaded MultipartFile
     * @param subDirectory optional sub-directory (e.g., "verification-documents/LV-1042")
     * @return StoredFileInfo containing file metadata and stored path
     * @throws IOException if an I/O error occurs
     */
    StoredFileInfo storeFile(MultipartFile file, String subDirectory) throws IOException;

    /**
     * Load a stored file as a Spring Resource for download or preview streaming.
     *
     * @param relativePath relative file path (e.g., "verification-documents/LV-1042/uuid_file.pdf")
     * @return Resource representing the file
     * @throws IOException if the file does not exist or cannot be read
     */
    Resource loadFileAsResource(String relativePath) throws IOException;

    /**
     * Delete a file from local storage.
     *
     * @param relativePath relative file path
     */
    void deleteFile(String relativePath);

    /**
     * Check if a file exists.
     *
     * @param relativePath relative file path
     * @return true if exists
     */
    boolean exists(String relativePath);
}
