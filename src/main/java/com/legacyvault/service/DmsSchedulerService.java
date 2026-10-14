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

@Service
public class DmsSchedulerService {

    private static final Logger logger = LoggerFactory.getLogger(DmsSchedulerService.class);

    private final DeadMansSwitchRepository dmsRepository;
    private final UserRepository userRepository;
    private final ContactRepository contactRepository;
    private final HandoverService handoverService;
    private final EmailService emailService;

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
     * Chạy định kỳ mỗi 60 giây (hoặc cấu hình cron trong production).
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 10000)
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

        LocalDateTime graceDeadline = nextDue.plusDays(dms.getGracePeriodDays());
        String currentStatus = dms.getStatus();

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

                long graceDaysLeft = Math.max(1, Duration.between(now, graceDeadline).toDays());
                User owner = userRepository.findById(dms.getOwnerId()).orElse(null);
                String ownerName = owner != null ? owner.getFullName() : "Vault Owner";
                String ownerEmail = owner != null ? owner.getEmail() : null;

                if (ownerEmail != null) {
                    emailService.sendDmsGracePeriodEmail(ownerEmail, ownerName, ownerName, (int) graceDaysLeft, true);
                }

                List<Contact> executors = contactRepository.findByOwnerIdAndIsExecutorTrue(dms.getOwnerId());
                for (Contact executor : executors) {
                    emailService.sendDmsGracePeriodEmail(executor.getEmail(), executor.getFullName(), ownerName, (int) graceDaysLeft, false);
                }
            }
            return;
        }

        // 3. Sắp đến hạn Check-in (trong vòng 3 ngày) -> Chuyển sang DUE_SOON
        if (now.isAfter(nextDue.minusDays(3))) {
            if ("active".equalsIgnoreCase(currentStatus)) {
                logger.info("DMS {} of owner {} due soon. Sending reminder.", dms.getDmsId(), dms.getOwnerId());
                dms.setStatus("due_soon");
                dmsRepository.save(dms);

                long daysLeft = Math.max(1, Duration.between(now, nextDue).toDays());
                User owner = userRepository.findById(dms.getOwnerId()).orElse(null);
                if (owner != null && owner.getEmail() != null) {
                    emailService.sendDmsWarningEmail(owner.getEmail(), owner.getFullName(), (int) daysLeft);
                }
            }
        }
    }
}
