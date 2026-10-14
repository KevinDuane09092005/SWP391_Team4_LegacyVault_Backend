package com.legacyvault.service;

import com.legacyvault.entity.Contact;
import com.legacyvault.entity.DeadMansSwitch;
import com.legacyvault.entity.User;
import com.legacyvault.repository.ContactRepository;
import com.legacyvault.repository.DeadMansSwitchRepository;
import com.legacyvault.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DmsSchedulerServiceTest {

    @Mock
    private DeadMansSwitchRepository dmsRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ContactRepository contactRepository;

    @Mock
    private HandoverService handoverService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private DmsSchedulerService schedulerService;

    private User testOwner;
    private Contact testExecutor;
    private DeadMansSwitch testDms;

    @BeforeEach
    void setUp() {
        testOwner = new User();
        testOwner.setId("owner-1");
        testOwner.setFullName("Thanh Pham");
        testOwner.setEmail("thanh@example.com");
        testOwner.setPhoneNumber("0123456789");
        testOwner.setRole("vault_owner");

        testExecutor = new Contact("owner-1", "Linh Pham", "Attorney", "linh.pham@legacymail.com", "0901234567", false, true, "verified");
        testExecutor.setContactId("executor-1");

        testDms = new DeadMansSwitch("owner-1", 30, 7);
        testDms.setDmsId("dms-1");
    }

    @Test
    void testEvaluate_GracePeriodExceeded_TriggersHandover() {
        LocalDateTime now = LocalDateTime.now();
        // nextDue was 10 days ago, grace period is 7 days => graceDeadline was 3 days ago
        testDms.setNextCheckInAt(now.minusDays(10));
        testDms.setGracePeriodDays(7);
        testDms.setStatus("grace_period");

        schedulerService.evaluateSingleDms(testDms, now);

        assertEquals("triggered", testDms.getStatus());
        verify(dmsRepository, times(1)).save(testDms);
        verify(handoverService, times(1)).createHandoverCase("owner-1", "dms-1");
    }

    @Test
    void testEvaluate_CheckInMissed_EntersGracePeriod() {
        LocalDateTime now = LocalDateTime.now();
        // nextDue was 2 days ago, grace period is 7 days => still in grace period
        testDms.setNextCheckInAt(now.minusDays(2));
        testDms.setGracePeriodDays(7);
        testDms.setStatus("active");

        when(userRepository.findById("owner-1")).thenReturn(Optional.of(testOwner));
        when(contactRepository.findByOwnerIdAndIsExecutorTrue("owner-1")).thenReturn(List.of(testExecutor));

        schedulerService.evaluateSingleDms(testDms, now);

        assertEquals("grace_period", testDms.getStatus());
        verify(dmsRepository, times(1)).save(testDms);
        verify(emailService, times(1)).sendDmsGracePeriodEmail(eq("thanh@example.com"), any(), any(), anyInt(), eq(true));
        verify(emailService, times(1)).sendDmsGracePeriodEmail(eq("linh.pham@legacymail.com"), any(), any(), anyInt(), eq(false));
    }

    @Test
    void testEvaluate_CheckInDueSoon_TransitionsToDueSoonAndWarns() {
        LocalDateTime now = LocalDateTime.now();
        // nextDue is in 2 days => within 3 days threshold
        testDms.setNextCheckInAt(now.plusDays(2));
        testDms.setStatus("active");

        when(userRepository.findById("owner-1")).thenReturn(Optional.of(testOwner));

        schedulerService.evaluateSingleDms(testDms, now);

        assertEquals("due_soon", testDms.getStatus());
        verify(dmsRepository, times(1)).save(testDms);
        verify(emailService, times(1)).sendDmsWarningEmail(eq("thanh@example.com"), eq("Thanh Pham"), anyInt());
    }

    @Test
    void testEvaluate_NormalActive_RemainsUnchanged() {
        LocalDateTime now = LocalDateTime.now();
        // nextDue is in 20 days => safe
        testDms.setNextCheckInAt(now.plusDays(20));
        testDms.setStatus("active");

        schedulerService.evaluateSingleDms(testDms, now);

        assertEquals("active", testDms.getStatus());
        verify(dmsRepository, never()).save(testDms);
        verify(emailService, never()).sendDmsWarningEmail(any(), any(), anyInt());
    }
}
