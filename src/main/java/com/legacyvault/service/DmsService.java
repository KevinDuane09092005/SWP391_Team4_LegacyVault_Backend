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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DmsService {

    private final DeadMansSwitchRepository dmsRepository;
    private final CheckInLogRepository checkInLogRepository;
    private final ContactRepository contactRepository;
    private final DmsSchedulerService dmsSchedulerService;

    public DmsService(DeadMansSwitchRepository dmsRepository,
                      CheckInLogRepository checkInLogRepository,
                      ContactRepository contactRepository) {
        this(dmsRepository, checkInLogRepository, contactRepository, null);
    }

    @Autowired
    public DmsService(DeadMansSwitchRepository dmsRepository,
                      CheckInLogRepository checkInLogRepository,
                      ContactRepository contactRepository,
                      DmsSchedulerService dmsSchedulerService) {
        this.dmsRepository = dmsRepository;
        this.checkInLogRepository = checkInLogRepository;
        this.contactRepository = contactRepository;
        this.dmsSchedulerService = dmsSchedulerService;
    }

    @Transactional
    public DmsStatusResponse getDmsStatus(String ownerId) {
        DeadMansSwitch dms = dmsRepository.findByOwnerId(ownerId)
                .orElseGet(() -> {
                    DeadMansSwitch defaultDms = new DeadMansSwitch(ownerId, 60, 120);
                    return dmsRepository.save(defaultDms);
                });

        // Tự động chuyển đổi các bản ghi cấu hình test cũ (30s/50s) sang mặc định kiểm thử mới (60s/120s)
        if (dms.getCheckInIntervalSeconds() == 30 && dms.getGracePeriodSeconds() == 50) {
            dms.setCheckInIntervalSeconds(60);
            dms.setGracePeriodSeconds(120);
            if (dms.getLastCheckInAt() != null) {
                dms.setNextCheckInAt(dms.getLastCheckInAt().plusSeconds(60));
            }
            dms = dmsRepository.save(dms);
        }

        if (dmsSchedulerService != null && !"triggered".equalsIgnoreCase(dms.getStatus())) {
            dmsSchedulerService.evaluateSingleDms(dms, LocalDateTime.now());
        }

        return buildStatusResponse(dms);
    }

    @Transactional
    public DmsStatusResponse saveOrUpdateConfig(String ownerId, DmsConfigRequest request) {
        Integer interval = request.getCheckInIntervalSeconds();
        Integer grace = request.getGracePeriodSeconds();

        if (interval == null || interval < 1) {
            throw new IllegalArgumentException("Check-in interval must be at least 1 second");
        }
        if (grace == null || grace < 1) {
            throw new IllegalArgumentException("Grace period must be at least 1 second");
        }

        DeadMansSwitch dms = dmsRepository.findByOwnerId(ownerId)
                .orElseGet(() -> new DeadMansSwitch(ownerId, interval, grace));

        dms.setCheckInIntervalSeconds(interval);
        dms.setGracePeriodSeconds(grace);

        LocalDateTime lastCheckIn = dms.getLastCheckInAt() != null ? dms.getLastCheckInAt() : LocalDateTime.now();
        dms.setLastCheckInAt(lastCheckIn);
        dms.setNextCheckInAt(lastCheckIn.plusSeconds(interval));

        if (!"triggered".equalsIgnoreCase(dms.getStatus())) {
            dms.setStatus("active");
        }

        DeadMansSwitch saved = dmsRepository.save(dms);
        return buildStatusResponse(saved);
    }

    @Transactional
    public CheckInResponse checkIn(String ownerId, String checkInType) {
        DeadMansSwitch dms = dmsRepository.findByOwnerId(ownerId)
                .orElseGet(() -> new DeadMansSwitch(ownerId, 60, 120));

        if (dms.getCheckInIntervalSeconds() == 30 && dms.getGracePeriodSeconds() == 50) {
            dms.setCheckInIntervalSeconds(60);
            dms.setGracePeriodSeconds(120);
        }

        LocalDateTime now = LocalDateTime.now();
        dms.setLastCheckInAt(now);
        dms.setNextCheckInAt(now.plusSeconds(dms.getCheckInIntervalSeconds()));
        dms.setStatus("active");
        DeadMansSwitch saved = dmsRepository.save(dms);

        String type = checkInType != null ? checkInType : "MANUAL_DASHBOARD";
        CheckInLog log = new CheckInLog(saved.getDmsId(), ownerId, type);
        checkInLogRepository.save(log);

        return new CheckInResponse(
                true,
                "Heartbeat confirmed. Timer has been reset to active state.",
                saved.getStatus(),
                saved.getLastCheckInAt(),
                saved.getNextCheckInAt()
        );
    }

    @Transactional
    public CheckInResponse emergencyCheckIn(String ownerId) {
        return checkIn(ownerId, "EMERGENCY_RESTORE");
    }

    @Transactional(readOnly = true)
    public List<CheckInLogDto> getAuditLogs(String ownerId) {
        List<CheckInLog> logs = checkInLogRepository.findByOwnerIdOrderByRecordedAtDesc(ownerId);
        return logs.stream()
                .map(log -> new CheckInLogDto(log.getLogId(), log.getCheckInType(), log.getStatus(), log.getRecordedAt()))
                .collect(Collectors.toList());
    }

    private DmsStatusResponse buildStatusResponse(DeadMansSwitch dms) {
        DmsStatusResponse response = new DmsStatusResponse();
        response.setDmsId(dms.getDmsId());
        response.setOwnerId(dms.getOwnerId());
        response.setCheckInIntervalSeconds(dms.getCheckInIntervalSeconds());
        response.setGracePeriodSeconds(dms.getGracePeriodSeconds());
        response.setCheckInIntervalDays(dms.getCheckInIntervalSeconds());
        response.setGracePeriodDays(dms.getGracePeriodSeconds());
        response.setLastCheckInAt(dms.getLastCheckInAt());
        response.setNextCheckInAt(dms.getNextCheckInAt());

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextDue = dms.getNextCheckInAt();
        LocalDateTime graceDeadline = nextDue != null ? nextDue.plusSeconds(dms.getGracePeriodSeconds()) : now;

        String currentStatus = dms.getStatus();
        if (!"triggered".equalsIgnoreCase(currentStatus)) {
            if (nextDue != null && now.isAfter(graceDeadline)) {
                currentStatus = "triggered";
            } else if (nextDue != null && now.isAfter(nextDue)) {
                currentStatus = "grace_period";
            } else if (nextDue != null && now.isAfter(nextDue.minusSeconds(
                    dms.getCheckInIntervalSeconds() < 180
                            ? Math.min(20, Math.max(10, dms.getCheckInIntervalSeconds() / 3))
                            : Math.min(3 * 86400L, Math.max(60L, dms.getCheckInIntervalSeconds() / 3))))) {
                currentStatus = "due_soon";
            } else {
                currentStatus = "active";
            }
        }
        response.setStatus(currentStatus);

        if (nextDue != null && nextDue.isAfter(now)) {
            Duration remaining = Duration.between(now, nextDue);
            response.setSecondsRemaining(Math.max(0, remaining.getSeconds()));
            response.setDaysRemaining(Math.max(0, remaining.toDays()));
        } else {
            response.setSecondsRemaining(0);
            response.setDaysRemaining(0);
        }

        if (nextDue != null && now.isAfter(nextDue) && graceDeadline.isAfter(now)) {
            Duration graceRemaining = Duration.between(now, graceDeadline);
            response.setGracePeriodSecondsRemaining(Math.max(0, graceRemaining.getSeconds()));
            response.setGracePeriodDaysRemaining(Math.max(0, graceRemaining.toDays()));
        } else {
            response.setGracePeriodSecondsRemaining(0);
            response.setGracePeriodDaysRemaining(0);
        }

        List<Contact> executors = contactRepository.findByOwnerIdAndIsExecutorTrue(dms.getOwnerId());
        if (!executors.isEmpty()) {
            Contact executor = executors.get(0);
            response.setExecutorName(executor.getFullName());
            response.setExecutorEmail(executor.getEmail());
        }

        return response;
    }
}
