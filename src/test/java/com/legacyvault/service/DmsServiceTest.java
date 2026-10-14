package com.legacyvault.service;

import com.legacyvault.dto.CheckInLogDto;
import com.legacyvault.dto.CheckInResponse;
import com.legacyvault.dto.DmsConfigRequest;
import com.legacyvault.dto.DmsStatusResponse;
import com.legacyvault.entity.CheckInLog;
import com.legacyvault.entity.Contact;
import com.legacyvault.entity.DeadMansSwitch;
import com.legacyvault.repository.CheckInLogRepository;
import com.legacyvault.repository.ContactRepository;
import com.legacyvault.repository.DeadMansSwitchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DmsServiceTest {

    @Mock
    private DeadMansSwitchRepository dmsRepository;

    @Mock
    private CheckInLogRepository checkInLogRepository;

    @Mock
    private ContactRepository contactRepository;

    @InjectMocks
    private DmsService dmsService;

    private DeadMansSwitch testDms;
    private Contact testExecutor;

    @BeforeEach
    void setUp() {
        testDms = new DeadMansSwitch("owner-1", 90, 14);
        testDms.setDmsId("dms-uuid-1");
        testDms.setLastCheckInAt(LocalDateTime.now().minusDays(10));
        testDms.setNextCheckInAt(LocalDateTime.now().plusDays(80));

        testExecutor = new Contact("owner-1", "Linh Pham", "Attorney", "linh.pham@legacymail.com", "0901234567", false, true, "verified");
        testExecutor.setContactId("executor-uuid-1");
    }

    @Test
    void testGetDmsStatus_ExistingConfig() {
        when(dmsRepository.findByOwnerId("owner-1")).thenReturn(Optional.of(testDms));
        when(contactRepository.findByOwnerIdAndIsExecutorTrue("owner-1")).thenReturn(List.of(testExecutor));

        DmsStatusResponse response = dmsService.getDmsStatus("owner-1");

        assertNotNull(response);
        assertEquals("dms-uuid-1", response.getDmsId());
        assertEquals("owner-1", response.getOwnerId());
        assertEquals("active", response.getStatus());
        assertEquals(90, response.getCheckInIntervalDays());
        assertEquals(14, response.getGracePeriodDays());
        assertEquals("Linh Pham", response.getExecutorName());
        assertEquals("linh.pham@legacymail.com", response.getExecutorEmail());
        assertTrue(response.getDaysRemaining() > 0);
    }

    @Test
    void testSaveOrUpdateConfig_Success() {
        DmsConfigRequest request = new DmsConfigRequest(60, 10);

        when(dmsRepository.findByOwnerId("owner-1")).thenReturn(Optional.of(testDms));
        when(dmsRepository.save(any(DeadMansSwitch.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(contactRepository.findByOwnerIdAndIsExecutorTrue("owner-1")).thenReturn(List.of(testExecutor));

        DmsStatusResponse response = dmsService.saveOrUpdateConfig("owner-1", request);

        assertNotNull(response);
        assertEquals(60, response.getCheckInIntervalDays());
        assertEquals(10, response.getGracePeriodDays());
        assertEquals("active", response.getStatus());
        verify(dmsRepository, times(1)).save(any(DeadMansSwitch.class));
    }

    @Test
    void testSaveOrUpdateConfig_InvalidInterval_ThrowsException() {
        DmsConfigRequest request = new DmsConfigRequest(0, 14);

        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                dmsService.saveOrUpdateConfig("owner-1", request)
        );

        assertTrue(exception.getMessage().contains("Check-in interval must be at least 1 day"));
    }

    @Test
    void testCheckIn_Success_ResetsTimerAndCreatesLog() {
        when(dmsRepository.findByOwnerId("owner-1")).thenReturn(Optional.of(testDms));
        when(dmsRepository.save(any(DeadMansSwitch.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(checkInLogRepository.save(any(CheckInLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CheckInResponse response = dmsService.checkIn("owner-1", "MANUAL_DASHBOARD");

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("active", response.getStatus());
        assertNotNull(response.getLastCheckInAt());
        assertNotNull(response.getNextCheckInAt());
        verify(dmsRepository, times(1)).save(any(DeadMansSwitch.class));
        verify(checkInLogRepository, times(1)).save(any(CheckInLog.class));
    }

    @Test
    void testEmergencyCheckIn_Success() {
        testDms.setStatus("grace_period");
        when(dmsRepository.findByOwnerId("owner-1")).thenReturn(Optional.of(testDms));
        when(dmsRepository.save(any(DeadMansSwitch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CheckInResponse response = dmsService.emergencyCheckIn("owner-1");

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("active", response.getStatus());
        verify(dmsRepository, times(1)).save(any(DeadMansSwitch.class));
        verify(checkInLogRepository, times(1)).save(any(CheckInLog.class));
    }

    @Test
    void testGetAuditLogs_ReturnsList() {
        CheckInLog log = new CheckInLog("dms-uuid-1", "owner-1", "MANUAL_DASHBOARD");
        log.setLogId("log-1");
        when(checkInLogRepository.findByOwnerIdOrderByRecordedAtDesc("owner-1")).thenReturn(List.of(log));

        List<CheckInLogDto> logs = dmsService.getAuditLogs("owner-1");

        assertNotNull(logs);
        assertEquals(1, logs.size());
        assertEquals("MANUAL_DASHBOARD", logs.get(0).getCheckInType());
    }
}
