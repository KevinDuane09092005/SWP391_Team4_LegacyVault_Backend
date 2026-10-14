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

        boolean userExists = userRepository.existsByEmail(request.getEmail());
        String status = userExists ? "verified" : "pending";

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
                request.getEmail(),
                null,
                isBeneficiary,
                isExecutor,
                status
        );

        Contact savedContact = contactRepository.save(contact);

        if ("pending".equals(status)) {
            String ownerName = userRepository.findById(ownerId).map(u -> u.getFullName()).orElse("Vault Owner");
            emailService.sendInvitationEmail(savedContact.getEmail(), savedContact.getFullName(), ownerName);
        }

        return mapToResponse(savedContact);
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
