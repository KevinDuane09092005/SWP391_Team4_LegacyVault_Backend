package com.legacyvault.service;

import com.legacyvault.dto.ContactResponse;
import com.legacyvault.dto.CreateContactRequest;
import com.legacyvault.entity.Contact;
import com.legacyvault.entity.DeadMansSwitch;
import com.legacyvault.entity.User;
import com.legacyvault.repository.AssetAssignmentRepository;
import com.legacyvault.repository.ContactRepository;
import com.legacyvault.repository.DeadMansSwitchRepository;
import com.legacyvault.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ContactService {

    private final ContactRepository contactRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final AssetAssignmentRepository assetAssignmentRepository;
    private final DeadMansSwitchRepository deadMansSwitchRepository;

    public ContactService(ContactRepository contactRepository, UserRepository userRepository, EmailService emailService) {
        this(contactRepository, userRepository, emailService, null, null);
    }

    @Autowired
    public ContactService(ContactRepository contactRepository,
                          UserRepository userRepository,
                          EmailService emailService,
                          AssetAssignmentRepository assetAssignmentRepository,
                          DeadMansSwitchRepository deadMansSwitchRepository) {
        this.contactRepository = contactRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.assetAssignmentRepository = assetAssignmentRepository;
        this.deadMansSwitchRepository = deadMansSwitchRepository;
    }

    @Transactional
    public ContactResponse createContact(String ownerId, CreateContactRequest request) {
        if (ownerId == null || ownerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Owner ID is required");
        }

        String normalizedEmail = request.getEmail() != null ? request.getEmail().trim() : "";
        if (normalizedEmail.isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }

        Optional<User> ownerOpt = userRepository.findById(ownerId.trim());
        if (ownerOpt.isPresent() && ownerOpt.get().getEmail() != null
                && ownerOpt.get().getEmail().trim().equalsIgnoreCase(normalizedEmail)) {
            throw new IllegalArgumentException("Bạn không thể tự thêm email của chính mình vào sổ liên lạc.");
        }

        if (contactRepository.existsByOwnerIdAndEmailIgnoreCase(ownerId, normalizedEmail)) {
            throw new IllegalArgumentException("Email này đã tồn tại trong danh bạ của bạn. Các liên hệ không được trùng email với nhau.");
        }

        // Mọi người liên hệ mới đều bắt đầu ở trạng thái "pending" chờ xác nhận qua email
        String status = "pending";

        boolean isBeneficiary = true;
        boolean isExecutor = false;

        String expectedRole = request.getExpectedRole();
        if ("Executor".equalsIgnoreCase(expectedRole) || "digital_executor".equalsIgnoreCase(expectedRole)) {
            isBeneficiary = false;
            isExecutor = true;
        } else if ("Both".equalsIgnoreCase(expectedRole)) {
            isBeneficiary = true;
            isExecutor = true;
        } else if ("Beneficiary".equalsIgnoreCase(expectedRole)) {
            isBeneficiary = true;
            isExecutor = false;
        }

        String phoneNumber = request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank()
                ? request.getPhoneNumber().trim()
                : null;

        Contact contact = new Contact(
                ownerId,
                request.getFullName(),
                request.getRelationship(),
                normalizedEmail,
                phoneNumber,
                isBeneficiary,
                isExecutor,
                status
        );

        Contact savedContact = contactRepository.save(contact);

        String ownerName = ownerOpt.map(User::getFullName).orElse("Vault Owner");
        emailService.sendContactInvitationEmail(
                savedContact.getEmail(),
                savedContact.getFullName(),
                ownerName,
                savedContact.getRelationship(),
                expectedRole != null ? expectedRole : "Beneficiary",
                savedContact.getContactId()
        );

        return mapToResponse(savedContact);
    }

    @Transactional
    public ContactResponse respondToInvitation(String contactId, String decision) {
        if (contactId == null || contactId.isBlank()) {
            throw new IllegalArgumentException("Contact ID is required");
        }
        Contact contact = contactRepository.findById(contactId.trim())
                .orElseThrow(() -> new RuntimeException("Contact invitation not found"));

        if ("revoked".equalsIgnoreCase(contact.getStatus())) {
            throw new IllegalStateException("Lời mời liên hệ này đã bị chủ tài sản thu hồi. Đường dẫn không còn hiệu lực.");
        }

        if ("accept".equalsIgnoreCase(decision) || "accepted".equalsIgnoreCase(decision)) {
            contact.setStatus("verified");
        } else if ("reject".equalsIgnoreCase(decision) || "rejected".equalsIgnoreCase(decision)
                || "decline".equalsIgnoreCase(decision)) {
            contact.setStatus("rejected");
        } else {
            throw new IllegalArgumentException("Invalid decision. Use 'accept' or 'reject'.");
        }

        Contact saved = contactRepository.save(contact);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ContactResponse> getContacts(String ownerId, String keyword) {
        return getContacts(ownerId, keyword, null, null);
    }

    @Transactional(readOnly = true)
    public List<ContactResponse> getContacts(String ownerId, String keyword, String expectedRole, String status) {
        if (ownerId == null || ownerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Owner ID is required");
        }

        List<Contact> contacts;
        if (keyword != null && !keyword.trim().isEmpty()) {
            contacts = contactRepository.searchContacts(ownerId, keyword.trim());
        } else {
            contacts = contactRepository.findByOwnerId(ownerId);
        }

        return contacts.stream()
                .filter(c -> {
                    if (expectedRole == null || expectedRole.isBlank() || "ALL".equalsIgnoreCase(expectedRole)) {
                        return true;
                    }
                    String role = expectedRole.trim();
                    if ("executor".equalsIgnoreCase(role) || "digital_executor".equalsIgnoreCase(role)) {
                        return Boolean.TRUE.equals(c.getIsExecutor());
                    }
                    if ("beneficiary".equalsIgnoreCase(role)) {
                        return Boolean.TRUE.equals(c.getIsBeneficiary());
                    }
                    return true;
                })
                .filter(c -> {
                    if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) {
                        return true;
                    }
                    return c.getStatus() != null && c.getStatus().equalsIgnoreCase(status.trim());
                })
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ContactResponse getContactById(String ownerId, String contactId) {
        Contact contact = contactRepository.findByContactIdAndOwnerId(contactId, ownerId)
                .orElseThrow(() -> new RuntimeException("Contact not found or access denied"));
        return mapToResponse(contact);
    }

    @Transactional
    public ContactResponse updateContact(String ownerId, String contactId, CreateContactRequest request) {
        validateOwnerAndContactId(ownerId, contactId);
        ensureVaultIsActive(ownerId);

        Contact contact = contactRepository.findByContactIdAndOwnerId(contactId.trim(), ownerId.trim())
                .orElseThrow(() -> new RuntimeException("Contact not found or access denied"));

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            contact.setFullName(request.getFullName().trim());
        }
        if (request.getRelationship() != null && !request.getRelationship().isBlank()) {
            contact.setRelationship(request.getRelationship().trim());
        }
        if (request.getPhoneNumber() != null) {
            contact.setPhoneNumber(request.getPhoneNumber().isBlank() ? null : request.getPhoneNumber().trim());
        }

        String expectedRole = request.getExpectedRole();
        if (expectedRole != null && !expectedRole.isBlank()) {
            if ("Executor".equalsIgnoreCase(expectedRole) || "digital_executor".equalsIgnoreCase(expectedRole)) {
                contact.setIsBeneficiary(false);
                contact.setIsExecutor(true);
            } else if ("Both".equalsIgnoreCase(expectedRole)) {
                contact.setIsBeneficiary(true);
                contact.setIsExecutor(true);
            } else if ("Beneficiary".equalsIgnoreCase(expectedRole)) {
                contact.setIsBeneficiary(true);
                contact.setIsExecutor(false);
            }
        }

        Contact saved = contactRepository.save(contact);
        return mapToResponse(saved);
    }

    @Transactional
    public ContactResponse resendContactInvitation(String ownerId, String contactId) {
        validateOwnerAndContactId(ownerId, contactId);
        ensureVaultIsActive(ownerId);

        Contact contact = contactRepository.findByContactIdAndOwnerId(contactId.trim(), ownerId.trim())
                .orElseThrow(() -> new RuntimeException("Contact not found or access denied"));

        if ("verified".equalsIgnoreCase(contact.getStatus())) {
            throw new IllegalStateException("Liên hệ này đã xác nhận lời mời (verified), không cần gửi lại.");
        }

        contact.setStatus("pending");
        Contact saved = contactRepository.save(contact);

        String ownerName = userRepository.findById(ownerId.trim()).map(User::getFullName).orElse("Vault Owner");
        String roleLabel = Boolean.TRUE.equals(saved.getIsExecutor()) && Boolean.TRUE.equals(saved.getIsBeneficiary())
                ? "Both"
                : (Boolean.TRUE.equals(saved.getIsExecutor()) ? "Executor" : "Beneficiary");

        emailService.sendContactInvitationEmail(
                saved.getEmail(),
                saved.getFullName(),
                ownerName,
                saved.getRelationship(),
                roleLabel,
                saved.getContactId()
        );

        return mapToResponse(saved);
    }

    @Transactional
    public ContactResponse revokeContactInvitation(String ownerId, String contactId) {
        validateOwnerAndContactId(ownerId, contactId);

        Contact contact = contactRepository.findByContactIdAndOwnerId(contactId.trim(), ownerId.trim())
                .orElseThrow(() -> new RuntimeException("Contact not found or access denied"));

        if (!"pending".equalsIgnoreCase(contact.getStatus())) {
            throw new IllegalStateException("Chỉ có thể thu hồi lời mời liên hệ đang ở trạng thái chờ (pending).");
        }

        contact.setStatus("revoked");
        Contact saved = contactRepository.save(contact);
        return mapToResponse(saved);
    }

    @Transactional
    public ContactResponse revokeContactAccess(String ownerId, String contactId) {
        validateOwnerAndContactId(ownerId, contactId);
        ensureVaultIsActive(ownerId);

        Contact contact = contactRepository.findByContactIdAndOwnerId(contactId.trim(), ownerId.trim())
                .orElseThrow(() -> new RuntimeException("Contact not found or access denied"));

        if ("revoked".equalsIgnoreCase(contact.getStatus())) {
            throw new IllegalStateException("Quyền truy cập của người liên hệ này đã bị thu hồi trước đó.");
        }

        contact.setStatus("revoked");
        Contact saved = contactRepository.save(contact);
        return mapToResponse(saved);
    }

    @Transactional
    public void deleteContact(String ownerId, String contactId) {
        validateOwnerAndContactId(ownerId, contactId);
        ensureVaultIsActive(ownerId);

        Contact contact = contactRepository.findByContactIdAndOwnerId(contactId.trim(), ownerId.trim())
                .orElseThrow(() -> new RuntimeException("Contact not found or access denied"));

        if (assetAssignmentRepository != null && assetAssignmentRepository.existsByExecutorContactId(contact.getContactId())) {
            throw new IllegalStateException(
                    "Không thể xóa người liên hệ này vì đang được chỉ định làm Người thực thi (Executor) cho tài sản. Vui lòng thay đổi Người thực thi của tài sản trước."
            );
        }

        contactRepository.delete(contact);
    }

    private void validateOwnerAndContactId(String ownerId, String contactId) {
        if (ownerId == null || ownerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Owner ID is required");
        }
        if (contactId == null || contactId.isBlank()) {
            throw new IllegalArgumentException("Contact ID is required");
        }
    }

    private void ensureVaultIsActive(String ownerId) {
        if (deadMansSwitchRepository != null) {
            Optional<DeadMansSwitch> dmsOpt = deadMansSwitchRepository.findByOwnerId(ownerId.trim());
            if (dmsOpt.isPresent() && "triggered".equalsIgnoreCase(dmsOpt.get().getStatus())) {
                throw new IllegalStateException("Kho lưu trữ đã kích hoạt bàn giao (triggered) vì Dead Man's Switch đã được kích hoạt, không thể thay đổi hoặc thu hồi quyền truy cập.");
            }
        }
    }

    private ContactResponse mapToResponse(Contact contact) {
        return new ContactResponse(
                contact.getContactId(),
                contact.getOwnerId(),
                contact.getFullName(),
                contact.getRelationship(),
                contact.getEmail(),
                contact.getPhoneNumber(),
                contact.getIsBeneficiary(),
                contact.getIsExecutor(),
                contact.getStatus(),
                contact.getCreatedAt(),
                contact.getUpdatedAt()
        );
    }
}
