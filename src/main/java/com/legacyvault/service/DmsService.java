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

    public DmsService(DeadMansSwitchRepository dmsRepository,
                      CheckInLogRepository checkInLogRepository,
                      ContactRepository contactRepository) {
        this.dmsRepository = dmsRepository;
        this.checkInLogRepository = checkInLogRepository;
        this.contactRepository = contactRepository;
    }

    @Transactional
    public DmsStatusResponse getDmsStatus(String ownerId) {
        DeadMansSwitch dms = dmsRepository.findByOwnerId(ownerId)
                .orElseGet(() -> {
                    DeadMansSwitch defaultDms = new DeadMansSwitch(ownerId, 30, 7);
                    return dmsRepository.save(defaultDms);
                });

        return buildStatusResponse(dms);
    }

    @Transactional
    public DmsStatusResponse saveOrUpdateConfig(String ownerId, DmsConfigRequest request) {
        if (request.getCheckInIntervalDays() == null || request.getCheckInIntervalDays() < 1) {
            throw new IllegalArgumentException("Check-in interval must be at least 1 day");
        }
        if (request.getGracePeriodDays() == null || request.getGracePeriodDays() < 1) {
            throw new IllegalArgumentException("Grace period must be at least 1 day");
        }

        DeadMansSwitch dms = dmsRepository.findByOwnerId(ownerId)
                .orElseGet(() -> new DeadMansSwitch(ownerId, request.getCheckInIntervalDays(), request.getGracePeriodDays()));

        dms.setCheckInIntervalDays(request.getCheckInIntervalDays());
        dms.setGracePeriodDays(request.getGracePeriodDays());

        LocalDateTime lastCheckIn = dms.getLastCheckInAt() != null ? dms.getLastCheckInAt() : LocalDateTime.now();
        dms.setLastCheckInAt(lastCheckIn);
        dms.setNextCheckInAt(lastCheckIn.plusDays(request.getCheckInIntervalDays()));

        if (!"triggered".equalsIgnoreCase(dms.getStatus())) {
            dms.setStatus("active");
        }

        DeadMansSwitch saved = dmsRepository.save(dms);
        return buildStatusResponse(saved);
    }

    @Transactional
    public CheckInResponse checkIn(String ownerId, String checkInType) {
        DeadMansSwitch dms = dmsRepository.findByOwnerId(ownerId)
                .orElseGet(() -> new DeadMansSwitch(ownerId, 30, 7));

        LocalDateTime now = LocalDateTime.now();
        dms.setLastCheckInAt(now);
        dms.setNextCheckInAt(now.plusDays(dms.getCheckInIntervalDays()));
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
        response.setCheckInIntervalDays(dms.getCheckInIntervalDays());
        response.setGracePeriodDays(dms.getGracePeriodDays());
        response.setLastCheckInAt(dms.getLastCheckInAt());
        response.setNextCheckInAt(dms.getNextCheckInAt());

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextDue = dms.getNextCheckInAt();
        LocalDateTime graceDeadline = nextDue != null ? nextDue.plusDays(dms.getGracePeriodDays()) : now;

        String currentStatus = dms.getStatus();
        if (!"triggered".equalsIgnoreCase(currentStatus)) {
            if (nextDue != null && now.isAfter(graceDeadline)) {
                currentStatus = "grace_period";
            } else if (nextDue != null && now.isAfter(nextDue)) {
                currentStatus = "grace_period";
            } else if (nextDue != null && now.isAfter(nextDue.minusDays(3))) {
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
