package com.legacyvault.service;

import com.legacyvault.dto.ContactResponse;
import com.legacyvault.dto.CreateContactRequest;
import com.legacyvault.entity.Contact;
import com.legacyvault.repository.ContactRepository;
import com.legacyvault.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ContactServiceTest {

    @Mock
    private ContactRepository contactRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private ContactService contactService;

    private CreateContactRequest request;

    @BeforeEach
    void setUp() {
        request = new CreateContactRequest(
                "Tran Thi B",
                "Spouse",
                "tranthib@email.com",
                "Beneficiary"
        );
    }

    @Test
    void testCreateContact_StartsAsPendingAndSendsInvitationEmail() {
        when(contactRepository.existsByOwnerIdAndEmailIgnoreCase("owner-123", "tranthib@email.com")).thenReturn(false);
        when(userRepository.findById("owner-123")).thenReturn(Optional.empty());
        when(contactRepository.save(any(Contact.class))).thenAnswer(invocation -> {
            Contact c = invocation.getArgument(0);
            c.setContactId("contact-uuid-1");
            return c;
        });

        ContactResponse response = contactService.createContact("owner-123", request);

        assertNotNull(response);
        assertEquals("pending", response.getStatus());
        assertTrue(response.getIsBeneficiary());
        assertFalse(response.getIsExecutor());
        verify(emailService, times(1)).sendContactInvitationEmail(
                eq("tranthib@email.com"),
                eq("Tran Thi B"),
                anyString(),
                eq("Spouse"),
                eq("Beneficiary"),
                eq("contact-uuid-1")
        );
    }

    @Test
    void testCreateContact_DuplicateEmail_ThrowsException() {
        when(contactRepository.existsByOwnerIdAndEmailIgnoreCase("owner-123", "tranthib@email.com")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                contactService.createContact("owner-123", request)
        );

        assertTrue(ex.getMessage().contains("không được trùng email"));
        verify(contactRepository, never()).save(any());
    }

    @Test
    void testRespondToInvitation_AcceptSetsVerified() {
        Contact contact = new Contact("owner-123", "Tran Thi B", "Spouse", "tranthib@email.com",
                null, true, false, "pending");
        contact.setContactId("contact-uuid-1");
        when(contactRepository.findById("contact-uuid-1")).thenReturn(Optional.of(contact));
        when(contactRepository.save(any(Contact.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ContactResponse response = contactService.respondToInvitation("contact-uuid-1", "accept");

        assertEquals("verified", response.getStatus());
    }

    @Test
    void testRespondToInvitation_RejectSetsRejected() {
        Contact contact = new Contact("owner-123", "Tran Thi B", "Spouse", "tranthib@email.com",
                null, true, false, "pending");
        contact.setContactId("contact-uuid-1");
        when(contactRepository.findById("contact-uuid-1")).thenReturn(Optional.of(contact));
        when(contactRepository.save(any(Contact.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ContactResponse response = contactService.respondToInvitation("contact-uuid-1", "reject");

        assertEquals("rejected", response.getStatus());
    }

    @Test
    void testGetContacts_Success() {
        Contact contact = new Contact("owner-123", "Tran Thi B", "Spouse", "tranthib@email.com", 
                                      "0912345678", true, false, "verified");
        contact.setContactId("contact-1");
        when(contactRepository.findByOwnerId("owner-123")).thenReturn(List.of(contact));

        List<ContactResponse> list = contactService.getContacts("owner-123", null);

        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("Tran Thi B", list.get(0).getFullName());
    }
}
