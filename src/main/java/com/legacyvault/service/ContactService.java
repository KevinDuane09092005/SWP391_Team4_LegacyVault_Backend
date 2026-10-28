package com.legacyvault.service;

import com.legacyvault.dto.ContactResponse;
import com.legacyvault.dto.CreateContactRequest;
import com.legacyvault.entity.Contact;
import com.legacyvault.repository.ContactRepository;
import com.legacyvault.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ContactService {

    private final ContactRepository contactRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public ContactService(ContactRepository contactRepository, UserRepository userRepository, EmailService emailService) {
        this.contactRepository = contactRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
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

        if (contactRepository.existsByOwnerIdAndEmailIgnoreCase(ownerId, normalizedEmail)) {
            throw new IllegalArgumentException("Email này đã tồn tại trong danh bạ của bạn. Các liên hệ không được trùng email với nhau.");
        }

        // Mọi người liên hệ mới đều bắt đầu ở trạng thái "pending" chờ xác nhận qua email
        String status = "pending";

        boolean isBeneficiary = true;
        boolean isExecutor = false;

        String expectedRole = request.getExpectedRole();
        if ("Executor".equalsIgnoreCase(expectedRole)) {
            isBeneficiary = false;
            isExecutor = true;
        } else if ("Both".equalsIgnoreCase(expectedRole)) {
            isBeneficiary = true;
            isExecutor = true;
        } else if ("Beneficiary".equalsIgnoreCase(expectedRole)) {
            isBeneficiary = true;
            isExecutor = false;
        }

        Contact contact = new Contact(
                ownerId,
                request.getFullName(),
                request.getRelationship(),
                normalizedEmail,
                null,
                isBeneficiary,
                isExecutor,
                status
        );

        Contact savedContact = contactRepository.save(contact);

        String ownerName = userRepository.findById(ownerId).map(u -> u.getFullName()).orElse("Vault Owner");
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
        if (ownerId == null || ownerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Owner ID is required");
        }

        List<Contact> contacts;
        if (keyword != null && !keyword.trim().isEmpty()) {
            contacts = contactRepository.searchContacts(ownerId, keyword.trim());
        } else {
            contacts = contactRepository.findByOwnerId(ownerId);
        }

        return contacts.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ContactResponse getContactById(String ownerId, String contactId) {
        Contact contact = contactRepository.findByContactIdAndOwnerId(contactId, ownerId)
                .orElseThrow(() -> new RuntimeException("Contact not found or access denied"));
        return mapToResponse(contact);
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
