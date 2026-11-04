package com.legacyvault.service;

import com.legacyvault.dto.StoredFileInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class LocalFileStorageServiceTest {

    private LocalFileStorageServiceImpl storageService;

    @TempDir
    Path tempStorageDir;

    @BeforeEach
    void setUp() {
        storageService = new LocalFileStorageServiceImpl();
        ReflectionTestUtils.setField(storageService, "baseDir", tempStorageDir.toString());
        storageService.init();
    }

    @Test
    void testStoreFile_PdfSuccess() throws IOException {
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file",
                "death_certificate.pdf",
                "application/pdf",
                "Mock PDF content for death certificate".getBytes()
        );

        StoredFileInfo info = storageService.storeFile(pdfFile, "verification-documents/LV-1001");

        assertNotNull(info);
        assertEquals("death_certificate.pdf", info.getOriginalFileName());
        assertTrue(info.getStoredFileName().endsWith("_death_certificate.pdf"));
        assertTrue(info.getRelativePath().startsWith("verification-documents/LV-1001/"));
        assertEquals("application/pdf", info.getContentType());
        assertNotNull(info.getSha256Checksum());
        assertTrue(storageService.exists(info.getRelativePath()));

        // Test load
        Resource resource = storageService.loadFileAsResource(info.getRelativePath());
        assertNotNull(resource);
        assertTrue(resource.exists());
        assertTrue(resource.isReadable());

        // Test delete
        storageService.deleteFile(info.getRelativePath());
        assertFalse(storageService.exists(info.getRelativePath()));
    }

    @Test
    void testStoreFile_PngSuccess() throws IOException {
        MockMultipartFile pngFile = new MockMultipartFile(
                "file",
                "id_card_scan.png",
                "image/png",
                new byte[]{1, 2, 3, 4, 5}
        );

        StoredFileInfo info = storageService.storeFile(pngFile, "verification-documents/LV-1002");

        assertNotNull(info);
        assertEquals("id_card_scan.png", info.getOriginalFileName());
        assertEquals("image/png", info.getContentType());
        assertTrue(storageService.exists(info.getRelativePath()));
    }

    @Test
    void testStoreFile_InvalidExtension_ThrowsException() {
        MockMultipartFile exeFile = new MockMultipartFile(
                "file",
                "malicious.exe",
                "application/x-msdownload",
                "malicious content".getBytes()
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                storageService.storeFile(exeFile, "verification-documents/LV-1003"));
        assertTrue(ex.getMessage().contains("Invalid file extension"));
    }

    @Test
    void testStoreFile_EmptyFile_ThrowsException() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.pdf",
                "application/pdf",
                new byte[0]
        );

        assertThrows(IllegalArgumentException.class, () ->
                storageService.storeFile(emptyFile, "verification-documents/LV-1004"));
    }

    @Test
    void testStoreFile_PathTraversalInFilename_ThrowsException() {
        MockMultipartFile traversalFile = new MockMultipartFile(
                "file",
                "../../secret.pdf",
                "application/pdf",
                "content".getBytes()
        );

        assertThrows(IllegalArgumentException.class, () ->
                storageService.storeFile(traversalFile, "test"));
    }

    @Test
    void testLoadFileAsResource_FileNotFound_ThrowsException() {
        assertThrows(FileNotFoundException.class, () ->
                storageService.loadFileAsResource("non-existent-folder/missing.pdf"));
    }
}
