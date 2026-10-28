package com.legacyvault.service;

import com.legacyvault.entity.Contact;
import com.legacyvault.entity.DeadMansSwitch;
import com.legacyvault.entity.User;
import com.legacyvault.repository.ContactRepository;
import com.legacyvault.repository.DeadMansSwitchRepository;
import com.legacyvault.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DmsSchedulerService {

    private static final Logger logger = LoggerFactory.getLogger(DmsSchedulerService.class);

    private final DeadMansSwitchRepository dmsRepository;
    private final UserRepository userRepository;
    private final ContactRepository contactRepository;
    private final HandoverService handoverService;
    private final EmailService emailService;
    private final Set<String> graceWarningSentCycles = ConcurrentHashMap.newKeySet();

    public DmsSchedulerService(DeadMansSwitchRepository dmsRepository,
                               UserRepository userRepository,
                               ContactRepository contactRepository,
                               HandoverService handoverService,
                               EmailService emailService) {
        this.dmsRepository = dmsRepository;
        this.userRepository = userRepository;
        this.contactRepository = contactRepository;
        this.handoverService = handoverService;
        this.emailService = emailService;
    }

    /**
     * Định kỳ quét các cấu hình DMS để chuyển đổi trạng thái FSM:
     * active -> due_soon -> grace_period -> triggered
     * Chạy định kỳ mỗi 10 giây (fixedDelay = 10000) để không bỏ lỡ cửa sổ cảnh báo 20s/30s trong Testing Mode.
     */
    @Scheduled(fixedDelay = 10000, initialDelay = 10000)
    @Transactional
    public void scanAndEvaluateDmsStatus() {
        LocalDateTime now = LocalDateTime.now();
        List<DeadMansSwitch> activeSwitches = dmsRepository.findByStatusIn(List.of("active", "due_soon", "grace_period"));

        for (DeadMansSwitch dms : activeSwitches) {
            evaluateSingleDms(dms, now);
        }
    }

    @Transactional
    public void evaluateSingleDms(DeadMansSwitch dms, LocalDateTime now) {
        LocalDateTime nextDue = dms.getNextCheckInAt();
        if (nextDue == null) {
            return;
        }

        LocalDateTime graceDeadline = nextDue.plusSeconds(dms.getGracePeriodSeconds());
        String currentStatus = dms.getStatus();
        boolean isTestingMode = dms.getCheckInIntervalSeconds() < 86400 || dms.getGracePeriodSeconds() < 86400;

        // 1. Quá hạn Grace Period -> Kích hoạt bàn giao (Triggered)
        if (now.isAfter(graceDeadline)) {
            logger.warn("DMS {} of owner {} exceeded Grace Period! Triggering handover.", dms.getDmsId(), dms.getOwnerId());
            dms.setStatus("triggered");
            dmsRepository.save(dms);
            handoverService.createHandoverCase(dms.getOwnerId(), dms.getDmsId());
            return;
        }

        // 2. Quá hạn Check-in nhưng trong Grace Period -> Chuyển sang GRACE_PERIOD
        if (now.isAfter(nextDue)) {
            if (!"grace_period".equalsIgnoreCase(currentStatus)) {
                logger.warn("DMS {} of owner {} missed check-in! Entering Grace Period.", dms.getDmsId(), dms.getOwnerId());
                dms.setStatus("grace_period");
                dmsRepository.save(dms);
            }

            // Gửi mail cảnh báo khi gần hết thời gian Grace Period (ví dụ còn <= 30s đối với chu kỳ 120s)
            long graceReminderThresholdSeconds = dms.getGracePeriodSeconds() <= 300
                    ? Math.min(30, Math.max(10, dms.getGracePeriodSeconds() / 3))
                    : Math.min(3 * 86400L, Math.max(3600L, dms.getGracePeriodSeconds() / 3));

            if (now.isAfter(graceDeadline.minusSeconds(graceReminderThresholdSeconds))) {
                String cycleKey = (dms.getDmsId() != null ? dms.getDmsId() : dms.getOwnerId()) + ":" + nextDue;
                if (graceWarningSentCycles.add(cycleKey)) {
                    int graceDaysParam = isTestingMode
                            ? 0
                            : (int) Math.max(1, Duration.between(now, graceDeadline).toDays());
                    User owner = userRepository.findById(dms.getOwnerId()).orElse(null);
                    String ownerName = owner != null ? owner.getFullName() : "Vault Owner";
                    String ownerEmail = owner != null ? owner.getEmail() : null;

                    if (ownerEmail != null) {
                        emailService.sendDmsGracePeriodEmail(ownerEmail, ownerName, ownerName, graceDaysParam, true);
                    }

                    List<Contact> executors = contactRepository.findByOwnerIdAndIsExecutorTrue(dms.getOwnerId());
                    for (Contact executor : executors) {
                        emailService.sendDmsGracePeriodEmail(executor.getEmail(), executor.getFullName(), ownerName, graceDaysParam, false);
                    }
                }
            }
            return;
        }

        // 3. Sắp đến hạn Check-in (gần hết thời gian Check-in Interval) -> Chuyển sang DUE_SOON & gửi mail nhắc Check-in
        long reminderThresholdSeconds = dms.getCheckInIntervalSeconds() < 180
                ? Math.min(20, Math.max(10, dms.getCheckInIntervalSeconds() / 3))
                : Math.min(3 * 86400L, Math.max(60L, dms.getCheckInIntervalSeconds() / 3));
        if (now.isAfter(nextDue.minusSeconds(reminderThresholdSeconds))) {
            if ("active".equalsIgnoreCase(currentStatus)) {
                logger.info("DMS {} of owner {} due soon. Sending reminder.", dms.getDmsId(), dms.getOwnerId());
                dms.setStatus("due_soon");
                dmsRepository.save(dms);

                int daysParam = isTestingMode
                        ? 0
                        : (int) Math.max(1, Duration.between(now, nextDue).toDays());
                User owner = userRepository.findById(dms.getOwnerId()).orElse(null);
                if (owner != null && owner.getEmail() != null) {
                    emailService.sendDmsWarningEmail(owner.getEmail(), owner.getFullName(), daysParam);
                }
            }
        }
    }
}
