package com.legacyvault.service;

import com.legacyvault.dto.StoredFileInfo;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class SupabaseFileStorageServiceTest {

    private HttpServer server;
    private String baseUrl;
    private Map<String, byte[]> storedObjects;
    private AtomicReference<String> lastAuthHeader;
    private AtomicReference<String> lastApiKeyHeader;

    @BeforeEach
    void setUp() throws IOException {
        storedObjects = new ConcurrentHashMap<>();
        lastAuthHeader = new AtomicReference<>();
        lastApiKeyHeader = new AtomicReference<>();

        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/storage/v1/object/", this::handleObjectRequest);
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    private void handleObjectRequest(HttpExchange exchange) throws IOException {
        lastAuthHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
        lastApiKeyHeader.set(exchange.getRequestHeaders().getFirst("apikey"));

        String path = exchange.getRequestURI().getPath();
        String key = path.substring("/storage/v1/object/".length());
        String method = exchange.getRequestMethod();

        if ("POST".equalsIgnoreCase(method)) {
            byte[] body = exchange.getRequestBody().readAllBytes();
            storedObjects.put(key, body);
            sendText(exchange, 200, "{\"Key\":\"" + key + "\"}");
        } else if ("GET".equalsIgnoreCase(method)) {
            byte[] data = storedObjects.get(key);
            if (data == null) {
                sendText(exchange, 404, "{\"error\":\"not_found\"}");
            } else {
                exchange.sendResponseHeaders(200, data.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(data);
                }
            }
        } else if ("HEAD".equalsIgnoreCase(method)) {
            int code = storedObjects.containsKey(key) ? 200 : 404;
            exchange.sendResponseHeaders(code, -1);
            exchange.close();
        } else if ("DELETE".equalsIgnoreCase(method)) {
            storedObjects.remove(key);
            sendText(exchange, 200, "{\"message\":\"deleted\"}");
        } else {
            sendText(exchange, 405, "Method Not Allowed");
        }
    }

    private void sendText(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    @Test
    void storeLoadExistsAndDeleteWithSbSecretKey() throws IOException {
        SupabaseFileStorageServiceImpl service = new SupabaseFileStorageServiceImpl(
                baseUrl, "sb_secret_test_key_123", "legacyvault-files");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "tai lieu tai san.pdf",
                "application/pdf",
                "hello-pdf-content".getBytes(StandardCharsets.UTF_8)
        );

        StoredFileInfo info = service.storeFile(file, "asset-documents");
        assertTrue(info.getRelativePath().startsWith("asset-documents/"));
        assertTrue(info.getStoredFileName().endsWith("tai_lieu_tai_san.pdf"));
        assertEquals("sb_secret_test_key_123", lastApiKeyHeader.get());
        assertNull(lastAuthHeader.get());

        assertTrue(service.exists(info.getRelativePath()));

        Resource resource = service.loadFileAsResource(info.getRelativePath());
        assertArrayEquals("hello-pdf-content".getBytes(StandardCharsets.UTF_8),
                resource.getInputStream().readAllBytes());

        service.deleteFile(info.getRelativePath());
        assertFalse(service.exists(info.getRelativePath()));
        assertThrows(FileNotFoundException.class, () -> service.loadFileAsResource(info.getRelativePath()));
    }

    @Test
    void legacyJwtKeySendsBothApikeyAndBearerHeaders() throws IOException {
        SupabaseFileStorageServiceImpl service = new SupabaseFileStorageServiceImpl(
                baseUrl, "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test", "legacyvault-files");

        MockMultipartFile file = new MockMultipartFile(
                "file", "id.png", "image/png", new byte[]{1, 2, 3});
        service.storeFile(file, "verification-documents/LV-1001");

        assertEquals("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test", lastApiKeyHeader.get());
        assertEquals("Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test", lastAuthHeader.get());
    }
}
