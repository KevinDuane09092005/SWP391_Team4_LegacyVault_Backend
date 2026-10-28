package com.legacyvault.service;

import com.legacyvault.dto.HandoverCaseResponse;
import com.legacyvault.entity.Contact;
import com.legacyvault.entity.DeadMansSwitch;
import com.legacyvault.entity.HandoverCase;
import com.legacyvault.entity.User;
import com.legacyvault.repository.ContactRepository;
import com.legacyvault.repository.DeadMansSwitchRepository;
import com.legacyvault.repository.HandoverCaseRepository;
import com.legacyvault.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class HandoverService {

    private static final Logger logger = LoggerFactory.getLogger(HandoverService.class);

    private final HandoverCaseRepository handoverCaseRepository;
    private final UserRepository userRepository;
    private final ContactRepository contactRepository;
    private final EmailService emailService;
    private final DeadMansSwitchRepository dmsRepository;

    public HandoverService(HandoverCaseRepository handoverCaseRepository,
                           UserRepository userRepository,
                           ContactRepository contactRepository,
                           EmailService emailService,
                           DeadMansSwitchRepository dmsRepository) {
        this.handoverCaseRepository = handoverCaseRepository;
        this.userRepository = userRepository;
        this.contactRepository = contactRepository;
        this.emailService = emailService;
        this.dmsRepository = dmsRepository;
    }

    @Transactional
    public HandoverCase triggerHandover(String ownerId) {
        if (ownerId == null || ownerId.trim().isEmpty()) {
            ownerId = userRepository.findAll().stream()
                    .findFirst()
                    .map(User::getId)
                    .orElseThrow(() -> new IllegalArgumentException("No user found in system to trigger handover for"));
        }

        String targetOwnerId = ownerId.trim();
        DeadMansSwitch dms = dmsRepository.findByOwnerId(targetOwnerId)
                .orElseGet(() -> {
                    DeadMansSwitch defaultDms = new DeadMansSwitch(targetOwnerId, 60, 120);
                    return dmsRepository.save(defaultDms);
                });

        dms.setStatus("triggered");
        dmsRepository.save(dms);

        return createHandoverCase(targetOwnerId, dms.getDmsId());
    }

    @Transactional
    public HandoverCase createHandoverCase(String ownerId, String dmsId) {
        String datePrefix = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String randomSuffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String caseNumber = "HC-" + datePrefix + "-" + randomSuffix;

        HandoverCase handoverCase = new HandoverCase(caseNumber, ownerId, dmsId);
        handoverCase.setStatus("initiated");
        HandoverCase savedCase = handoverCaseRepository.save(handoverCase);
        logger.info("Created HandoverCase id={} caseNumber={} for ownerId={}", savedCase.getCaseId(), caseNumber, ownerId);

        User owner = userRepository.findById(ownerId).orElse(null);
        String ownerName = owner != null ? owner.getFullName() : "Vault Owner";

        List<Contact> executors = contactRepository.findByOwnerIdAndIsExecutorTrue(ownerId);
        for (Contact executor : executors) {
            emailService.sendHandoverTriggeredNotice(executor.getEmail(), executor.getFullName(), ownerName, caseNumber);
        }

        return savedCase;
    }

    @Transactional(readOnly = true)
    public List<HandoverCaseResponse> getCasesByOwner(String ownerId) {
        List<HandoverCase> cases = handoverCaseRepository.findByOwnerIdOrderByTriggeredAtDesc(ownerId);
        return cases.stream()
                .map(c -> new HandoverCaseResponse(
                        c.getCaseId(),
                        c.getCaseNumber(),
                        c.getOwnerId(),
                        c.getDmsId(),
                        c.getStatus(),
                        c.getTriggeredAt(),
                        c.getResolvedAt()
                ))
                .collect(Collectors.toList());
    }
}
