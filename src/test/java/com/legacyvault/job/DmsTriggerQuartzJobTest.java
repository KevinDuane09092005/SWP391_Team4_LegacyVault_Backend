package com.legacyvault.job;

import com.legacyvault.entity.DeadMansSwitch;
import com.legacyvault.entity.HandoverCase;
import com.legacyvault.repository.DeadMansSwitchRepository;
import com.legacyvault.service.HandoverService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobExecutionContext;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DmsTriggerQuartzJobTest {

    @Mock
    private DeadMansSwitchRepository dmsRepository;

    @Mock
    private HandoverService handoverService;

    @Mock
    private JobExecutionContext jobExecutionContext;

    @InjectMocks
    private DmsTriggerQuartzJob quartzJob;

    private DeadMansSwitch testDms;

    @BeforeEach
    void setUp() {
        testDms = new DeadMansSwitch("owner-uuid-1", 30, 7);
        testDms.setDmsId("dms-uuid-1");
    }

    @Test
    void testExecuteInternal_WhenGracePeriodExceeded_TriggersHandover() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        // nextDue was 10 seconds ago, grace period is 7 seconds => graceDeadline was 3 seconds ago
        testDms.setNextCheckInAt(now.minusSeconds(10));
        testDms.setGracePeriodSeconds(7);
        testDms.setStatus("grace_period");

        when(dmsRepository.findByStatusIn(anyList())).thenReturn(List.of(testDms));
        when(handoverService.triggerHandover(anyString()))
                .thenReturn(new HandoverCase("HC-1042", "owner-uuid-1", "dms-uuid-1"));

        quartzJob.executeInternal(jobExecutionContext);

        // Verify fallback or direct trigger occurred for owner-uuid-1
        verify(handoverService, atLeastOnce()).triggerHandover("owner-uuid-1");
    }

    @Test
    void testExecuteInternal_WhenActiveAndWithinSafePeriod_DoesNotTrigger() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        // nextDue is 20 seconds in the future
        testDms.setNextCheckInAt(now.plusSeconds(20));
        testDms.setStatus("active");

        when(dmsRepository.findByStatusIn(anyList())).thenReturn(List.of(testDms));

        quartzJob.executeInternal(jobExecutionContext);

        verify(handoverService, never()).triggerHandover(anyString());
    }
}
