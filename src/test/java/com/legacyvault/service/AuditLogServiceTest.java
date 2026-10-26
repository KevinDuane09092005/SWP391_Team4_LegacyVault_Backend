package com.legacyvault.service;

import com.legacyvault.dto.AuditLogTableResponse;
import com.legacyvault.entity.AuditLog;
import com.legacyvault.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogService auditLogService;

    private AuditLog mockLog;

    @BeforeEach
    void setUp() {
        mockLog = new AuditLog("usr-1", "Alex Mercier", "LEGAL_VERIFIER",
                "Approve Legal Verification", "Case #LV-1042", "SUCCESS", "192.168.1.42");
        mockLog.setLogId("log-001");
        mockLog.setCurrentHashSha256("current-hash-val");
    }

    @Test
    void testRecordLog_GeneratesSha256HashChain() {
        when(auditLogRepository.findTopByOrderByCreatedAtDesc()).thenReturn(Optional.of(mockLog));
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        AuditLog saved = auditLogService.recordLog(
                "usr-2", "Minh Le", "DIGITAL_EXECUTOR",
                "Start Handover Release", "Case #LV-1042", "SUCCESS", "192.168.1.15", "details");

        assertNotNull(saved);
        assertEquals("current-hash-val", saved.getPreviousHashSha256());
        assertNotNull(saved.getCurrentHashSha256());
        assertEquals(64, saved.getCurrentHashSha256().length());
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void testSearchLogs_WithResults() {
        Page<AuditLog> page = new PageImpl<>(List.of(mockLog), PageRequest.of(0, 6), 1);
        when(auditLogRepository.searchAuditLogs(any(), any(), any(), any(), any(), any(PageRequest.class)))
                .thenReturn(page);

        AuditLogTableResponse response = auditLogService.searchLogs("LV-1042", "ALL", "ALL", null, null, 0, 6);

        assertNotNull(response);
        assertEquals(1, response.getLogs().size());
        assertEquals("Alex Mercier", response.getLogs().get(0).getUser());
        assertEquals("Approve Legal Verification", response.getLogs().get(0).getAction());
        assertEquals("SUCCESS", response.getLogs().get(0).getStatus());
    }

    @Test
    void emptySearchReturnsNoPrototypeRows() {
        when(auditLogRepository.searchAuditLogs(isNull(), eq("ALL"), eq("ALL"), isNull(), isNull(), any(PageRequest.class)))
                .thenReturn(Page.empty());

        AuditLogTableResponse response = auditLogService.searchLogs(null, "ALL", "ALL", null, null, 0, 6);

        assertNotNull(response);
        assertTrue(response.getLogs().isEmpty());
        assertEquals("Showing 0 of 0 audit logs", response.getPagination().getDisplayRange());
    }

    @Test
    void testExportCsv() {
        when(auditLogRepository.findForExport(isNull(), isNull())).thenReturn(List.of(mockLog));
        byte[] csv = auditLogService.exportCsv(null, null);

        assertNotNull(csv);
        assertTrue(csv.length > 0);
        String text = new String(csv);
        assertTrue(text.contains("TIME,USER,ACTION,TARGET,STATUS,IP_ADDRESS"));
        assertTrue(text.contains("Approve Legal Verification"));
    }

    @Test
    void exportEscapesCsvCellsAndUsesDatabaseRows() {
        mockLog.setUserName("Alex, \"A\"");
        when(auditLogRepository.findForExport(isNull(), isNull())).thenReturn(List.of(mockLog));

        String csv = new String(auditLogService.exportCsv(null, null));

        assertTrue(csv.contains("\"Alex, \"\"A\"\"\""));
        assertEquals(2, csv.lines().count());
    }

    @Test
    void invalidSearchDateIsRejected() {
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> auditLogService.searchLogs(null, "ALL", "ALL", "not-a-date", null, 0, 6));
    }

    @Test
    void invalidPageSizeIsRejected() {
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> auditLogService.searchLogs(null, "ALL", "ALL", null, null, 0, 201));
    }
}
