package com.legacyvault.job;

import com.legacyvault.entity.DeadMansSwitch;
import com.legacyvault.repository.DeadMansSwitchRepository;
import com.legacyvault.service.HandoverService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Quartz Job chạy định kỳ mỗi 1 phút:
 * 1. Quét trạng thái Dead Man's Switch (DMS) của các Vault Owner.
 * 2. Xác định Owner đã quá hạn Check-in và bỏ qua thời gian ân hạn (Grace Period).
 * 3. Kích hoạt bàn giao tự động qua API: POST http://localhost:8080/api/v1/handover/trigger
 */
@Component
@DisallowConcurrentExecution
public class DmsTriggerQuartzJob extends QuartzJobBean {

    private static final Logger logger = LoggerFactory.getLogger(DmsTriggerQuartzJob.class);

    private static final String TRIGGER_API_URL = "http://localhost:8080/api/v1/handover/trigger";

    private final DeadMansSwitchRepository dmsRepository;
    private final HandoverService handoverService;
    private final RestTemplate restTemplate;

    public DmsTriggerQuartzJob(DeadMansSwitchRepository dmsRepository,
                              HandoverService handoverService) {
        this.dmsRepository = dmsRepository;
        this.handoverService = handoverService;
        this.restTemplate = new RestTemplate();
    }

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        logger.info("[Quartz Job: DMS Monitor] Starting 1-minute scheduled check for inactive/deceased Vault Owners...");

        LocalDateTime now = LocalDateTime.now();
        List<DeadMansSwitch> activeSwitches = dmsRepository.findByStatusIn(List.of("active", "due_soon", "grace_period"));

        int triggeredCount = 0;
        for (DeadMansSwitch dms : activeSwitches) {
            LocalDateTime nextDue = dms.getNextCheckInAt();
            if (nextDue == null) {
                continue;
            }

            LocalDateTime graceDeadline = nextDue.plusSeconds(dms.getGracePeriodSeconds());

            // Điều kiện kích hoạt: Đã quá hạn check-in và quá hạn thời gian ân hạn (Grace Period)
            if (now.isAfter(graceDeadline)) {
                logger.warn("[Quartz Job] Owner {} missed check-in and grace period (deadline: {}). Triggering handover...",
                        dms.getOwnerId(), graceDeadline);

                boolean triggeredSuccess = triggerViaApi(dms.getOwnerId());
                if (!triggeredSuccess) {
                    // Fallback gọi service layer trực tiếp nếu HTTP endpoint chưa sẵn sàng
                    logger.info("[Quartz Job] Falling back to direct service execution for owner {}", dms.getOwnerId());
                    handoverService.triggerHandover(dms.getOwnerId());
                }
                triggeredCount++;
            }
        }

        logger.info("[Quartz Job: DMS Monitor] Finished check. Total cases triggered this run: {}", triggeredCount);
    }

    /**
     * Gửi request HTTP POST tới http://localhost:8080/api/v1/handover/trigger
     */
    private boolean triggerViaApi(String ownerId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> body = Map.of("owner_id", ownerId);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

            restTemplate.postForObject(TRIGGER_API_URL, request, Map.class);
            logger.info("[Quartz Job] Successfully triggered handover via API {} for ownerId={}", TRIGGER_API_URL, ownerId);
            return true;
        } catch (Exception e) {
            logger.warn("[Quartz Job] Calling API {} returned exception: {}. Will use internal service fallback.",
                    TRIGGER_API_URL, e.getMessage());
            return false;
        }
    }
}
