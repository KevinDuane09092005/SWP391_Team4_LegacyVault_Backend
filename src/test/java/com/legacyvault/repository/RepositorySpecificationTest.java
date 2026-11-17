package com.legacyvault.repository;

import com.legacyvault.entity.AuditLog;
import com.legacyvault.entity.Contact;
import com.legacyvault.entity.HandoverCase;
import com.legacyvault.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:pg_compat_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class RepositorySpecificationTest {

    @Autowired
    private HandoverCaseRepository handoverCaseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private ContactRepository contactRepository;

    @Test
    void searchMethodsHandleNullAndFilteredParametersCleanly() {
        HandoverCase handoverCase = new HandoverCase("LV-1001", "owner-1", "dms-1");
        handoverCase.setStatus("PENDING_VERIFICATION");
        handoverCaseRepository.save(handoverCase);

        Page<HandoverCase> allCases = handoverCaseRepository.searchVerificationQueue(null, null, PageRequest.of(0, 10));
        assertEquals(1, allCases.getTotalElements());
        Page<HandoverCase> filteredCases = handoverCaseRepository.searchVerificationQueue("pending_verification", "1001", PageRequest.of(0, 10));
        assertEquals(1, filteredCases.getTotalElements());

        User admin = new User();
        admin.setFullName("Admin User");
        admin.setEmail("admin@example.com");
        admin.setPasswordHash("hashed");
        admin.setRole("system_admin");
        userRepository.save(admin);

        assertEquals(1, userRepository.searchAdminUsers(null, null, null, PageRequest.of(0, 10)).getTotalElements());
        assertEquals(1, userRepository.searchAdminUsers("admin", "admin", "ACTIVE", PageRequest.of(0, 10)).getTotalElements());

        AuditLog log = new AuditLog("user-1", "Admin User", "SYSTEM_ADMIN", "LOGIN", "Auth", "SUCCESS", "127.0.0.1");
        log.setCurrentHashSha256("hash1");
        auditLogRepository.save(log);

        assertEquals(1, auditLogRepository.searchAuditLogs(null, "ALL", "ALL", "ALL", null, null, PageRequest.of(0, 10)).getTotalElements());
        assertEquals(1, auditLogRepository.findLogsForExport(null, null).size());

        Contact contact = new Contact("owner-1", "Alice", "Sister", "alice@example.com", null, true, false, "verified");
        contactRepository.save(contact);

        List<Contact> allContacts = contactRepository.searchContacts("owner-1", null);
        assertEquals(1, allContacts.size());
        assertEquals(1, contactRepository.searchContacts("owner-1", "ali").size());
    }
}
