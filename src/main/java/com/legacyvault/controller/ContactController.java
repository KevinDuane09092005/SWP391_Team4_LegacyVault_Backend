package com.legacyvault.controller;

import com.legacyvault.dto.ContactResponse;
import com.legacyvault.dto.CreateContactRequest;
import com.legacyvault.service.ContactService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/contacts")
public class ContactController {

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @PostMapping
    public ResponseEntity<?> createContact(
            Authentication authentication,
            @Valid @RequestBody CreateContactRequest request
    ) {
        try {
            String ownerId = getAuthenticatedUserId(authentication);
            ContactResponse contact = contactService.createContact(ownerId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "Contact saved successfully",
                    "contact", contact
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getContacts(
            Authentication authentication,
            @RequestParam(value = "keyword", required = false) String keyword
    ) {
        try {
            String ownerId = getAuthenticatedUserId(authentication);
            List<ContactResponse> contacts = contactService.getContacts(ownerId, keyword);
            return ResponseEntity.ok(contacts);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{contactId}")
    public ResponseEntity<?> getContact(
            Authentication authentication,
            @PathVariable("contactId") String contactId
    ) {
        try {
            String ownerId = getAuthenticatedUserId(authentication);
            ContactResponse contact = contactService.getContactById(ownerId, contactId);
            return ResponseEntity.ok(contact);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    private String getAuthenticatedUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("Unauthorized: No user session found");
        }
        return authentication.getName();
    }
}
